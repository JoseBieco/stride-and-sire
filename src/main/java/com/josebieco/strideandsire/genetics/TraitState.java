package com.josebieco.strideandsire.genetics;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * Per-horse expression state of a {@link LineageTrait}: absent, carried genetically without activating behavior
 * ({@link #LATENT}), or actively expressed ({@link #MANIFEST}).
 */
public enum TraitState implements StringRepresentable {
    ABSENT,
    LATENT,
    MANIFEST;

    public static final Codec<TraitState> CODEC = StringRepresentable.fromEnum(TraitState::values);
    public static final StreamCodec<ByteBuf, TraitState> STREAM_CODEC =
        ByteBufCodecs.idMapper(i -> values()[i], Enum::ordinal);

    /** True if the horse carries this trait genetically (either latent or manifest). */
    public boolean isCarrier() {
        return this != ABSENT;
    }

    /** True only when the trait's gameplay behavior is active on this horse. */
    public boolean isManifest() {
        return this == MANIFEST;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
