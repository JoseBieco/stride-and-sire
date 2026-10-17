package com.josebieco.strideandsire.genetics;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * The three recessive coat genes in Fase D, ordered by epistatic dominance when multiple genes are
 * {@link Zygosity#HOMOZYGOUS_RECESSIVE}:
 * <ul>
 *   <li>{@link #OBSIDIAN} (Obsidiana)</li>
 *   <li>{@link #ALBINO} (Albino)</li>
 *   <li>{@link #SILVER_DAPPLE} (Prata Dappled)</li>
 * </ul>
 */
public enum CoatGene implements StringRepresentable {
    OBSIDIAN,
    ALBINO,
    SILVER_DAPPLE;

    public static final Codec<CoatGene> CODEC = StringRepresentable.fromEnum(CoatGene::values);
    public static final StreamCodec<ByteBuf, CoatGene> STREAM_CODEC =
        ByteBufCodecs.idMapper(i -> values()[i], Enum::ordinal);

    /** Lang key of the coat gene's display name ({@code coatgene.strideandsire.<name>}), used by the spyglass screen. */
    public String translationKey() {
        return "coatgene.strideandsire." + getSerializedName();
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
