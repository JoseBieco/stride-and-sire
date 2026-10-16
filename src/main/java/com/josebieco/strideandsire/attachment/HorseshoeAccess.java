package com.josebieco.strideandsire.attachment;

import com.josebieco.strideandsire.network.HorseshoeSyncPacket;
import com.josebieco.strideandsire.registry.ModAttachments;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.item.ItemStack;

/**
 * The single read/write entry point for a horse's horseshoe.
 *
 * <p>Eligible entities are {@link AbstractHorse} subtypes except {@link Camel} (and Camel Husk) and
 * {@link Llama} (and Trader Llama): Horse, Donkey, Mule, Skeleton Horse, Zombie Horse.
 * For ineligible entities {@link #get} returns {@link ItemStack#EMPTY} and {@link #set} is a no-op.
 *
 * <p>{@link #set} is the write choke point: on the server it also syncs the new stack to every client
 * tracking the horse. Side effects tied to equipping (e.g. the Gold speed modifier) belong here.
 */
public final class HorseshoeAccess {
    private HorseshoeAccess() {}

    public static boolean isEligible(Entity entity) {
        return entity instanceof AbstractHorse && !(entity instanceof Camel) && !(entity instanceof Llama);
    }

    /** The equipped horseshoe (the stored instance, not a copy), or {@link ItemStack#EMPTY}. */
    public static ItemStack get(AbstractHorse horse) {
        HorseshoeStorage storage = storage(horse);
        return storage == null ? ItemStack.EMPTY : storage.value().stack();
    }

    /**
     * Equips {@code stack} (stored as-is, so pass a stack the horse may own, e.g. {@code stack.split(1)}),
     * or clears the slot with {@link ItemStack#EMPTY}. No-op on ineligible entities.
     */
    public static void set(AbstractHorse horse, ItemStack stack) {
        HorseshoeStorage storage = storage(horse);
        if (storage == null) {
            return;
        }
        storage.setValue(HorseshoeAttachment.of(stack));
        if (!horse.level().isClientSide()) {
            HorseshoeSyncPacket.sendToTracking(horse);
        }
    }

    private static HorseshoeStorage storage(AbstractHorse horse) {
        if (!isEligible(horse)) {
            return null;
        }
        return horse.getCapability(ModAttachments.HORSESHOE).resolve().orElse(null);
    }
}
