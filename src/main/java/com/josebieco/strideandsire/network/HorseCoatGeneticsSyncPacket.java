package com.josebieco.strideandsire.network;

import com.josebieco.strideandsire.attachment.HorseCoatGeneticsAccess;
import com.josebieco.strideandsire.attachment.HorseCoatGeneticsAttachment;
import com.josebieco.strideandsire.client.HorseCoatGeneticsClientSync;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.PacketDistributor;

/** Server to client: syncs a horse's {@link HorseCoatGeneticsAttachment} to tracking clients. */
public record HorseCoatGeneticsSyncPacket(int entityId, HorseCoatGeneticsAttachment attachment) {
    public static final StreamCodec<RegistryFriendlyByteBuf, HorseCoatGeneticsSyncPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, HorseCoatGeneticsSyncPacket::entityId,
        HorseCoatGeneticsAttachment.STREAM_CODEC, HorseCoatGeneticsSyncPacket::attachment,
        HorseCoatGeneticsSyncPacket::new);

    private static HorseCoatGeneticsSyncPacket of(AbstractHorse horse) {
        return new HorseCoatGeneticsSyncPacket(horse.getId(), HorseCoatGeneticsAccess.get(horse));
    }

    public static void sendToTracking(AbstractHorse horse) {
        ModNetwork.CHANNEL.send(of(horse), PacketDistributor.TRACKING_ENTITY.with(horse));
    }

    public static void sendTo(AbstractHorse horse, ServerPlayer player) {
        ModNetwork.CHANNEL.send(of(horse), PacketDistributor.PLAYER.with(player));
    }

    static void handle(HorseCoatGeneticsSyncPacket packet, CustomPayloadEvent.Context context) {
        HorseCoatGeneticsClientSync.apply(packet.entityId(), packet.attachment());
    }
}
