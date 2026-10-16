package com.josebieco.strideandsire.network;

import com.josebieco.strideandsire.StrideAndSire;
import net.minecraft.resources.Identifier;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;

/** The mod's play-phase channel. Required on both sides (the mod adds items, so it must be on both anyway). */
public final class ModNetwork {
    public static final int PROTOCOL_VERSION = 2; // 2: HorseshoeSyncPacket generalised into HorseEquipmentSyncPacket (slot discriminator)

    public static final SimpleChannel CHANNEL = ChannelBuilder
        .named(Identifier.fromNamespaceAndPath(StrideAndSire.MODID, "main"))
        .networkProtocolVersion(PROTOCOL_VERSION)
        .simpleChannel()
            .play()
                .clientbound()
                    .addMain(HorseEquipmentSyncPacket.class, HorseEquipmentSyncPacket.STREAM_CODEC, HorseEquipmentSyncPacket::handle)
                .serverbound()
                    .addMain(OpenHorseshoeMenuPacket.class, OpenHorseshoeMenuPacket.STREAM_CODEC, OpenHorseshoeMenuPacket::handle)
        .build();

    private ModNetwork() {}

    /** Forces class initialisation (channel registration) during mod construction. */
    public static void init() {}
}
