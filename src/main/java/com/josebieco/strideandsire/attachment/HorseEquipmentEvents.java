package com.josebieco.strideandsire.attachment;

import com.josebieco.strideandsire.event.SaddlebagBonusEffectHandler;
import com.josebieco.strideandsire.network.HorseEquipmentSyncPacket;
import com.josebieco.strideandsire.network.HorseGeneticsSyncPacket;
import com.josebieco.strideandsire.registry.ModAttachments;
import java.util.Collection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * Game-bus listeners for all per-horse equipment (horseshoe, saddlebag) and genetics: attach the storages, sync on
 * start-tracking, drop the equipment on death.
 */
public final class HorseEquipmentEvents {
    private HorseEquipmentEvents() {}

    public static void register() {
        AttachCapabilitiesEvent.Entities.BUS.addListener(HorseEquipmentEvents::attach);
        PlayerEvent.StartTracking.BUS.addListener(HorseEquipmentEvents::onStartTracking);
        LivingDropsEvent.BUS.addListener(HorseEquipmentEvents::onDrops);
    }

    private static void attach(AttachCapabilitiesEvent.Entities event) {
        if (HorseshoeAccess.isEligible(event.getObject())) {
            HorseshoeStorage horseshoe = new HorseshoeStorage();
            event.addCapability(ModAttachments.HORSESHOE_ID, horseshoe);
            event.addListener(horseshoe::invalidate);
            SaddlebagStorage saddlebag = new SaddlebagStorage();
            event.addCapability(ModAttachments.SADDLEBAG_ID, saddlebag);
            event.addListener(saddlebag::invalidate);
            HorseGeneticsStorage genetics = new HorseGeneticsStorage();
            event.addCapability(ModAttachments.GENETICS_ID, genetics);
            event.addListener(genetics::invalidate);
        }
    }

    private static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof AbstractHorse horse && event.getEntity() instanceof ServerPlayer player) {
            for (HorseEquipmentSlot slot : HorseEquipmentSlot.values()) {
                if (!slot.get(horse).isEmpty()) {
                    HorseEquipmentSyncPacket.sendTo(horse, slot, player);
                }
            }
            if (!HorseGeneticsAccess.get(horse).isEmpty()) {
                HorseGeneticsSyncPacket.sendTo(horse, player);
            }
        }
    }

    /** Same path as vanilla saddle/armor: equipment is added to the captured death drops. */
    private static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof AbstractHorse horse)) {
            return;
        }
        dropHorseshoe(horse, event.getDrops());
        dropSaddlebag(horse, event.getSource(), event.getDrops());
    }

    /** Unequips the horseshoe and drops it, honouring Curse of Vanishing. */
    private static void dropHorseshoe(AbstractHorse horse, Collection<ItemEntity> drops) {
        ItemStack shoe = HorseshoeAccess.get(horse);
        if (shoe.isEmpty()) {
            return;
        }
        HorseshoeAccess.set(horse, ItemStack.EMPTY);
        if (EnchantmentHelper.has(shoe, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
            return;
        }
        drops.add(itemEntity(horse, shoe));
    }

    /** Unequips the saddlebag and drops it as one item; its container contents travel with the stack. */
    private static void dropSaddlebag(AbstractHorse horse, DamageSource source, Collection<ItemEntity> drops) {
        ItemStack bag = SaddlebagAccess.get(horse);
        if (bag.isEmpty()) {
            return;
        }
        SaddlebagAccess.set(horse, ItemStack.EMPTY);
        drops.add(itemEntity(horse, saddlebagDeathDrop(bag, source)));
    }

    /**
     * The stack a dying horse drops for its equipped saddlebag {@code bag} when killed by {@code source}: the bag,
     * with its container cleared on an explosion / fire death unless its material protects against that damage
     * (see {@link SaddlebagBonusEffectHandler}).
     */
    static ItemStack saddlebagDeathDrop(ItemStack bag, DamageSource source) {
        return SaddlebagBonusEffectHandler.deathDrop(bag, source);
    }

    private static ItemEntity itemEntity(AbstractHorse horse, ItemStack stack) {
        ItemEntity drop = new ItemEntity(horse.level(), horse.getX(), horse.getY(), horse.getZ(), stack);
        drop.setDefaultPickUpDelay();
        return drop;
    }
}
