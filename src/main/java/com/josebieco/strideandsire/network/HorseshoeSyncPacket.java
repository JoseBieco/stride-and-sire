package com.josebieco.strideandsire.network;

import com.josebieco.strideandsire.attachment.HorseshoeAccess;
import com.josebieco.strideandsire.client.HorseshoeClientSync;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.PacketDistributor;

/** Server to client: the horseshoe currently equipped on entity {@code entityId} (EMPTY means none). */
public record HorseshoeSyncPacket(int entityId, ItemStack stack) {
    public static final StreamCodec<RegistryFriendlyByteBuf, HorseshoeSyncPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, HorseshoeSyncPacket::entityId,
        ItemStack.OPTIONAL_STREAM_CODEC, HorseshoeSyncPacket::stack,
        HorseshoeSyncPacket::new);

    private static HorseshoeSyncPacket of(AbstractHorse horse) {
        return new HorseshoeSyncPacket(horse.getId(), HorseshoeAccess.get(horse).copy());
    }

    /** Sends the horse's current horseshoe to every player tracking it (riders included). */
    public static void sendToTracking(AbstractHorse horse) {
        ModNetwork.CHANNEL.send(of(horse), PacketDistributor.TRACKING_ENTITY.with(horse));
    }

    /** Sends the horse's current horseshoe to one player (used when that player starts tracking the horse). */
    public static void sendTo(AbstractHorse horse, ServerPlayer player) {
        ModNetwork.CHANNEL.send(of(horse), PacketDistributor.PLAYER.with(player));
    }

    /** Main-thread handler, only ever invoked on the client, so the client-only class never loads on a server. */
    static void handle(HorseshoeSyncPacket packet, CustomPayloadEvent.Context context) {
        HorseshoeClientSync.apply(packet.entityId(), packet.stack());
    }
}
