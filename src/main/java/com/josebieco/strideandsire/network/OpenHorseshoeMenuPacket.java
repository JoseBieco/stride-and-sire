package com.josebieco.strideandsire.network;

import com.josebieco.strideandsire.menu.HorseshoeMenuEvents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client to server: the rider pressed the inventory key while riding an eligible horse (replaces OPEN_INVENTORY). */
public record OpenHorseshoeMenuPacket() {
    public static final OpenHorseshoeMenuPacket INSTANCE = new OpenHorseshoeMenuPacket();
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenHorseshoeMenuPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    /** Main-thread handler on the server. */
    static void handle(OpenHorseshoeMenuPacket packet, CustomPayloadEvent.Context context) {
        if (context.getSender() != null) {
            HorseshoeMenuEvents.openFromVehicle(context.getSender());
        }
    }
}
