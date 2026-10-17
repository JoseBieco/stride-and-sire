package com.josebieco.strideandsire.network;

import com.josebieco.strideandsire.attachment.HorseGeneticsAccess;
import com.josebieco.strideandsire.attachment.HorseGeneticsAttachment;
import com.josebieco.strideandsire.client.HorseGeneticsClientSync;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.PacketDistributor;

/** Server to client: syncs a horse's {@link HorseGeneticsAttachment} to tracking clients. */
public record HorseGeneticsSyncPacket(int entityId, HorseGeneticsAttachment attachment) {
    public static final StreamCodec<RegistryFriendlyByteBuf, HorseGeneticsSyncPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, HorseGeneticsSyncPacket::entityId,
        HorseGeneticsAttachment.STREAM_CODEC, HorseGeneticsSyncPacket::attachment,
        HorseGeneticsSyncPacket::new);

    private static HorseGeneticsSyncPacket of(AbstractHorse horse) {
        return new HorseGeneticsSyncPacket(horse.getId(), HorseGeneticsAccess.get(horse));
    }

    public static void sendToTracking(AbstractHorse horse) {
        ModNetwork.CHANNEL.send(of(horse), PacketDistributor.TRACKING_ENTITY.with(horse));
    }

    public static void sendTo(AbstractHorse horse, ServerPlayer player) {
        ModNetwork.CHANNEL.send(of(horse), PacketDistributor.PLAYER.with(player));
    }

    static void handle(HorseGeneticsSyncPacket packet, CustomPayloadEvent.Context context) {
        HorseGeneticsClientSync.apply(packet.entityId(), packet.attachment());
    }
}
