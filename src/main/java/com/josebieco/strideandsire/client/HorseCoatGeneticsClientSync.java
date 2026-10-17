package com.josebieco.strideandsire.client;

import com.josebieco.strideandsire.attachment.HorseCoatGeneticsAccess;
import com.josebieco.strideandsire.attachment.HorseCoatGeneticsAttachment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

/** Client-only: applies a {@code HorseCoatGeneticsSyncPacket} to the client-side copy of the horse. */
public final class HorseCoatGeneticsClientSync {
    private HorseCoatGeneticsClientSync() {}

    public static void apply(int entityId, HorseCoatGeneticsAttachment attachment) {
        var level = Minecraft.getInstance().level;
        if (level != null && level.getEntity(entityId) instanceof AbstractHorse horse) {
            HorseCoatGeneticsAccess.set(horse, attachment);
        }
    }
}
