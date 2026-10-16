package com.josebieco.strideandsire.attachment;

import com.josebieco.strideandsire.network.HorseEquipmentSyncPacket;
import com.josebieco.strideandsire.registry.ModAttachments;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.ItemStack;

/**
 * The single read/write entry point for a horse's saddlebag.
 *
 * <p>Eligibility is the same as for horseshoes ({@link HorseshoeAccess#isEligible}): Horse, Donkey, Mule, Skeleton Horse,
 * Zombie Horse. For ineligible entities (Camel, Llama families) {@link #get} returns {@link ItemStack#EMPTY} and
 * {@link #set} is a no-op.
 *
 * <p>{@link #get} returns the <b>live</b> stored stack, not a copy. After mutating it (e.g. its container contents via
 * {@link com.josebieco.strideandsire.item.saddlebag.SaddlebagItem#setContents}) call {@link #set} again: {@link #set} is
 * the write choke point and, on the server, syncs the new stack to every client tracking the horse. Side effects tied
 * to equipping belong here and must be idempotent (menu slot writes may call {@code set} more than once).
 */
public final class SaddlebagAccess {
    private SaddlebagAccess() {}

    /** The equipped saddlebag (the stored instance, not a copy), or {@link ItemStack#EMPTY}. */
    public static ItemStack get(AbstractHorse horse) {
        SaddlebagStorage storage = storage(horse);
        return storage == null ? ItemStack.EMPTY : storage.value().stack();
    }

    /**
     * Equips {@code stack} (stored as-is, so pass a stack the horse may own, e.g. {@code stack.split(1)}),
     * or clears the slot with {@link ItemStack#EMPTY}. No-op on ineligible entities.
     */
    public static void set(AbstractHorse horse, ItemStack stack) {
        SaddlebagStorage storage = storage(horse);
        if (storage == null) {
            return;
        }
        storage.setValue(SaddlebagAttachment.of(stack));
        if (!horse.level().isClientSide()) {
            HorseEquipmentSyncPacket.sendToTracking(horse, HorseEquipmentSlot.SADDLEBAG);
        }
    }

    private static SaddlebagStorage storage(AbstractHorse horse) {
        if (!HorseshoeAccess.isEligible(horse)) {
            return null;
        }
        return horse.getCapability(ModAttachments.SADDLEBAG).resolve().orElse(null);
    }
}
