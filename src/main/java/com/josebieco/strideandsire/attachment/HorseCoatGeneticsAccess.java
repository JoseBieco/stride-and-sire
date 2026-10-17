package com.josebieco.strideandsire.attachment;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.jspecify.annotations.Nullable;

/**
 * The single read/write entry point for a horse's {@link HorseCoatGeneticsAttachment}.
 *
 * <p>Unlike equipment and lineage traits, recessive coat genetics apply strictly to vanilla {@code Horse} entities
 * ({@link EntityTypes#HORSE}). Donkeys, mules, skeleton horses, and zombie horses are ineligible; {@link #get} returns
 * {@link HorseCoatGeneticsAttachment#EMPTY} and {@link #set} is a no-op for them.
 */
public final class HorseCoatGeneticsAccess {
    private HorseCoatGeneticsAccess() {}

    public static boolean isEligible(@Nullable Entity entity) {
        return entity != null && entity.getType() == EntityTypes.HORSE;
    }

    public static boolean isEligible(@Nullable AbstractHorse horse) {
        return horse != null && horse.getType() == EntityTypes.HORSE;
    }

    public static HorseCoatGeneticsAttachment get(AbstractHorse horse) {
        HorseCoatGeneticsStorage storage = storage(horse);
        return storage == null ? HorseCoatGeneticsAttachment.EMPTY : storage.value();
    }

    public static void set(AbstractHorse horse, HorseCoatGeneticsAttachment attachment) {
        HorseCoatGeneticsStorage storage = storage(horse);
        if (storage == null) {
            return;
        }
        storage.setValue(attachment == null ? HorseCoatGeneticsAttachment.EMPTY : attachment);
        // Wired to HorseCoatGeneticsSyncPacket.sendToTracking in Task 4
    }

    private static @Nullable HorseCoatGeneticsStorage storage(AbstractHorse horse) {
        if (!isEligible(horse)) {
            return null;
        }
        // Wired to ModAttachments.COAT_GENETICS in Task 4
        return null;
    }
}
