package com.josebieco.strideandsire.genetics;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * Mendelian zygosity state of a recessive {@link CoatGene}: absent ({@link #HOMOZYGOUS_DOMINANT}), carried silently as
 * a single recessive allele ({@link #HETEROZYGOUS}), or phenotypically expressed with two recessive alleles
 * ({@link #HOMOZYGOUS_RECESSIVE}).
 */
public enum Zygosity implements StringRepresentable {
    HOMOZYGOUS_DOMINANT,
    HETEROZYGOUS,
    HOMOZYGOUS_RECESSIVE;

    public static final Codec<Zygosity> CODEC = StringRepresentable.fromEnum(Zygosity::values);
    public static final StreamCodec<ByteBuf, Zygosity> STREAM_CODEC =
        ByteBufCodecs.idMapper(i -> values()[i], Enum::ordinal);

    /** True if the horse carries at least one recessive allele ({@link #HETEROZYGOUS} or {@link #HOMOZYGOUS_RECESSIVE}). */
    public boolean isCarrier() {
        return this != HOMOZYGOUS_DOMINANT;
    }

    /** True only when the recessive coat phenotype is expressed on this horse ({@link #HOMOZYGOUS_RECESSIVE}). */
    public boolean isManifest() {
        return this == HOMOZYGOUS_RECESSIVE;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
