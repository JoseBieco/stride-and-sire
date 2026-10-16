package com.josebieco.strideandsire.network;

import com.josebieco.strideandsire.StrideAndSire;
import net.minecraft.resources.Identifier;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;

/** The mod's play-phase channel. Required on both sides (the mod adds items, so it must be on both anyway). */
public final class ModNetwork {
    public static final int PROTOCOL_VERSION = 1;

    public static final SimpleChannel CHANNEL = ChannelBuilder
        .named(Identifier.fromNamespaceAndPath(StrideAndSire.MODID, "main"))
        .networkProtocolVersion(PROTOCOL_VERSION)
        .simpleChannel()
            .play()
                .clientbound()
                    .addMain(HorseshoeSyncPacket.class, HorseshoeSyncPacket.STREAM_CODEC, HorseshoeSyncPacket::handle)
        .build();

    private ModNetwork() {}

    /** Forces class initialisation (channel registration) during mod construction. */
    public static void init() {}
}
