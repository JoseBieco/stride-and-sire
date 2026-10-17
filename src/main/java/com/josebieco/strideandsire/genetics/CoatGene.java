package com.josebieco.strideandsire.genetics;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * The three independent recessive coat genes of Fase D (spec names in parentheses):
 * <ul>
 *   <li>{@link #OBSIDIAN} (Preto Absoluto)</li>
 *   <li>{@link #ALBINO} (Albino)</li>
 *   <li>{@link #SILVER_DAPPLE} (Pangaré Prateado)</li>
 * </ul>
 * The declaration order carries no dominance or priority: when a horse is {@link Zygosity#HOMOZYGOUS_RECESSIVE} on
 * more than one gene, the displayed coat is drawn at random at birth
 * ({@link HorseCoatInheritance#pickManifestGene}) and then fixed permanently.
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
