package com.josebieco.strideandsire.attachment;

import com.josebieco.strideandsire.network.HorseshoeSyncPacket;
import com.josebieco.strideandsire.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

/** Game-bus listeners: attach the storage, sync on start-tracking, drop the horseshoe on death. */
public final class HorseshoeEvents {
    private HorseshoeEvents() {}

    public static void register() {
        AttachCapabilitiesEvent.Entities.BUS.addListener(HorseshoeEvents::attach);
        PlayerEvent.StartTracking.BUS.addListener(HorseshoeEvents::onStartTracking);
        LivingDropsEvent.BUS.addListener(HorseshoeEvents::onDrops);
    }

    private static void attach(AttachCapabilitiesEvent.Entities event) {
        if (HorseshoeAccess.isEligible(event.getObject())) {
            HorseshoeStorage storage = new HorseshoeStorage();
            event.addCapability(ModAttachments.HORSESHOE_ID, storage);
            event.addListener(storage::invalidate);
        }
    }

    private static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof AbstractHorse horse && event.getEntity() instanceof ServerPlayer player
                && !HorseshoeAccess.get(horse).isEmpty()) {
            HorseshoeSyncPacket.sendTo(horse, player);
        }
    }

    /** Same path as vanilla saddle/armor: added to the captured death drops, honouring Curse of Vanishing. */
    private static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof AbstractHorse horse)) {
            return;
        }
        ItemStack shoe = HorseshoeAccess.get(horse);
        if (shoe.isEmpty()) {
            return;
        }
        HorseshoeAccess.set(horse, ItemStack.EMPTY);
        if (EnchantmentHelper.has(shoe, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
            return;
        }
        ItemEntity drop = new ItemEntity(horse.level(), horse.getX(), horse.getY(), horse.getZ(), shoe);
        drop.setDefaultPickUpDelay();
        event.getDrops().add(drop);
    }
}
