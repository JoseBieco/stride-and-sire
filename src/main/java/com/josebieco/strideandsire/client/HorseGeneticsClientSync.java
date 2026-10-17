package com.josebieco.strideandsire.client;

import com.josebieco.strideandsire.attachment.HorseGeneticsAccess;
import com.josebieco.strideandsire.attachment.HorseGeneticsAttachment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

/** Client-only: applies a {@code HorseGeneticsSyncPacket} to the client-side copy of the horse. */
public final class HorseGeneticsClientSync {
    private HorseGeneticsClientSync() {}

    public static void apply(int entityId, HorseGeneticsAttachment attachment) {
        var level = Minecraft.getInstance().level;
        if (level != null && level.getEntity(entityId) instanceof AbstractHorse horse) {
            HorseGeneticsAccess.set(horse, attachment);
        }
    }
}
