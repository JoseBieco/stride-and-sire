package com.josebieco.strideandsire.attachment;

import com.josebieco.strideandsire.genetics.LineageTrait;
import com.josebieco.strideandsire.genetics.TraitState;
import com.josebieco.strideandsire.network.HorseGeneticsSyncPacket;
import com.josebieco.strideandsire.registry.ModAttachments;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

/**
 * The single read/write entry point for a horse's {@link HorseGeneticsAttachment}.
 *
 * <p>Eligibility is the same as for horseshoes/saddlebags ({@link HorseshoeAccess#isEligible}). For ineligible
 * entities {@link #get} returns {@link HorseGeneticsAttachment#EMPTY} and {@link #set} is a no-op.
 */
public final class HorseGeneticsAccess {
    private HorseGeneticsAccess() {}

    public static HorseGeneticsAttachment get(AbstractHorse horse) {
        HorseGeneticsStorage storage = storage(horse);
        return storage == null ? HorseGeneticsAttachment.EMPTY : storage.value();
    }

    public static TraitState getState(AbstractHorse horse, LineageTrait trait) {
        return get(horse).state(trait);
    }

    public static boolean hasManifest(AbstractHorse horse, LineageTrait trait) {
        return get(horse).isManifest(trait);
    }

    public static void set(AbstractHorse horse, LineageTrait trait, TraitState state) {
        HorseGeneticsStorage storage = storage(horse);
        if (storage == null) {
            return;
        }
        set(horse, storage.value().with(trait, state));
    }

    public static void set(AbstractHorse horse, HorseGeneticsAttachment attachment) {
        HorseGeneticsStorage storage = storage(horse);
        if (storage == null) {
            return;
        }
        storage.setValue(attachment == null ? HorseGeneticsAttachment.EMPTY : attachment);
        if (!horse.level().isClientSide()) {
            HorseGeneticsSyncPacket.sendToTracking(horse);
        }
    }

    private static HorseGeneticsStorage storage(AbstractHorse horse) {
        if (!HorseshoeAccess.isEligible(horse)) {
            return null;
        }
        return horse.getCapability(ModAttachments.GENETICS).resolve().orElse(null);
    }
}
