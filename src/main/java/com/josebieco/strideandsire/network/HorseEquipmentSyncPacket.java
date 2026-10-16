package com.josebieco.strideandsire.network;

import com.josebieco.strideandsire.attachment.HorseEquipmentSlot;
import com.josebieco.strideandsire.client.HorseEquipmentClientSync;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.PacketDistributor;

/** Server to client: the stack currently in equipment {@code slot} of entity {@code entityId} (EMPTY means none). */
public record HorseEquipmentSyncPacket(int entityId, HorseEquipmentSlot slot, ItemStack stack) {
    public static final StreamCodec<RegistryFriendlyByteBuf, HorseEquipmentSyncPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, HorseEquipmentSyncPacket::entityId,
        HorseEquipmentSlot.STREAM_CODEC, HorseEquipmentSyncPacket::slot,
        ItemStack.OPTIONAL_STREAM_CODEC, HorseEquipmentSyncPacket::stack,
        HorseEquipmentSyncPacket::new);

    private static HorseEquipmentSyncPacket of(AbstractHorse horse, HorseEquipmentSlot slot) {
        return new HorseEquipmentSyncPacket(horse.getId(), slot, slot.get(horse).copy());
    }

    /** Sends the horse's current stack in {@code slot} to every player tracking it (riders included). */
    public static void sendToTracking(AbstractHorse horse, HorseEquipmentSlot slot) {
        ModNetwork.CHANNEL.send(of(horse, slot), PacketDistributor.TRACKING_ENTITY.with(horse));
    }

    /** Sends the horse's current stack in {@code slot} to one player (used when that player starts tracking the horse). */
    public static void sendTo(AbstractHorse horse, HorseEquipmentSlot slot, ServerPlayer player) {
        ModNetwork.CHANNEL.send(of(horse, slot), PacketDistributor.PLAYER.with(player));
    }

    /** Main-thread handler, only ever invoked on the client, so the client-only class never loads on a server. */
    static void handle(HorseEquipmentSyncPacket packet, CustomPayloadEvent.Context context) {
        HorseEquipmentClientSync.apply(packet.entityId(), packet.slot(), packet.stack());
    }
}
