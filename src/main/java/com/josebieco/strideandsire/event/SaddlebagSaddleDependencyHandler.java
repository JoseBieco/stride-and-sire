package com.josebieco.strideandsire.event;

import com.josebieco.strideandsire.attachment.HorseshoeAccess;
import com.josebieco.strideandsire.attachment.SaddlebagAccess;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;

/**
 * A saddlebag only works on a saddled horse: when the saddle is removed, the equipped saddlebag is unequipped and
 * dropped in the world (container contents intact). Saddle removal itself is never blocked.
 *
 * <p>Hook: {@link LivingEquipmentChangeEvent} (server side, fired from {@code LivingEntity.tick} when the stack in an
 * equipment slot changes). Every vanilla removal path ends in {@code setItemSlot(SADDLE, EMPTY)} - taking the saddle
 * out of the horse menu slot, shearing it off ({@code Mob.shearItem}), commands - and so fires it, at the latest on the
 * horse's next tick. Death needs no special handling: {@code HorseEquipmentEvents} empties the attachment when it
 * adds the bag to the death drops, so the later saddle-cleared event finds nothing to drop; dead or dying horses are
 * skipped here anyway.
 *
 * <p>A horse can also enter a level already inconsistent (bag but no saddle: older saves, commands, other mods). Such
 * horses are queued on {@link EntityJoinLevelEvent} and corrected on the next server tick, never synchronously during
 * the join, because adding the dropped item entity while a chunk is loading is unsafe.
 */
public final class SaddlebagSaddleDependencyHandler {
    private SaddlebagSaddleDependencyHandler() {}

    /** Horses that joined a server level since the last server tick; checked (and drained) on the next tick. */
    private static List<AbstractHorse> pendingJoinChecks = new ArrayList<>();

    public static void register() {
        LivingEquipmentChangeEvent.BUS.addListener(SaddlebagSaddleDependencyHandler::onEquipmentChange);
        EntityJoinLevelEvent.BUS.addListener(SaddlebagSaddleDependencyHandler::onJoinLevel);
        TickEvent.ServerTickEvent.Pre.BUS.addListener(SaddlebagSaddleDependencyHandler::onServerTickPre);
    }

    private static void onJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof AbstractHorse horse
                && HorseshoeAccess.isEligible(horse)) {
            pendingJoinChecks.add(horse);
        }
    }

    private static void onServerTickPre(TickEvent.ServerTickEvent.Pre event) {
        if (pendingJoinChecks.isEmpty()) {
            return;
        }
        List<AbstractHorse> toCheck = pendingJoinChecks;
        pendingJoinChecks = new ArrayList<>();
        for (AbstractHorse horse : toCheck) {
            if (!horse.isRemoved()) {
                dropBagIfUnsaddled(horse);
            }
        }
    }

    private static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getSlot() == EquipmentSlot.SADDLE && event.getEntity() instanceof AbstractHorse horse
                && !event.getFrom().isEmpty() && event.getTo().isEmpty()) {
            dropBagIfUnsaddled(horse);
        }
    }

    /**
     * If {@code horse} has a saddlebag but no saddle, unequips the bag (attachment cleared first, so the stack never
     * has two owners) and spawns it as an item entity at the horse. Server side only; returns whether it dropped.
     */
    public static boolean dropBagIfUnsaddled(AbstractHorse horse) {
        if (horse.level().isClientSide() || !HorseshoeAccess.isEligible(horse) || horse.isDeadOrDying()
                || !horse.getItemBySlot(EquipmentSlot.SADDLE).isEmpty()) {
            return false;
        }
        ItemStack bag = SaddlebagAccess.get(horse);
        if (bag.isEmpty()) {
            return false;
        }
        ItemStack dropped = bag.copy();
        SaddlebagAccess.set(horse, ItemStack.EMPTY);
        // Same spawn as Entity.spawnAtLocation: this ItemEntity constructor already gives the vanilla drop motion
        // (random horizontal +-0.1, upward 0.2); spawnAtLocation itself is avoided because it may divert into
        // captureDrops.
        ItemEntity drop = new ItemEntity(horse.level(), horse.getX(), horse.getY(), horse.getZ(), dropped);
        drop.setDefaultPickUpDelay();
        horse.level().addFreshEntity(drop);
        return true;
    }
}
