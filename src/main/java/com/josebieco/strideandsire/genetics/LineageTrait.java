package com.josebieco.strideandsire.genetics;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * The three heritable lineage traits in Fase C:
 * <ul>
 *   <li>{@link #DRAFT} (Dorso Largo): unlocks 18-slot saddlebag capacity when manifest.</li>
 *   <li>{@link #SURE_FOOTED} (Casco Firme): total auto-step height 1.5 blocks while manifest and ridden by a player.</li>
 *   <li>{@link #FEARLESS} (Sangue Frio): no rearing from zombie / creeper / explosion damage when manifest (vanilla
 *       never ejects the rider on damage; the damage itself still applies).</li>
 * </ul>
 */
public enum LineageTrait implements StringRepresentable {
    DRAFT,
    SURE_FOOTED,
    FEARLESS;

    public static final Codec<LineageTrait> CODEC = StringRepresentable.fromEnum(LineageTrait::values);
    public static final StreamCodec<ByteBuf, LineageTrait> STREAM_CODEC =
        ByteBufCodecs.idMapper(i -> values()[i], Enum::ordinal);

    /** Lang key of the trait's display name ({@code trait.strideandsire.<name>}), used by the spyglass screen. */
    public String translationKey() {
        return "trait.strideandsire." + getSerializedName();
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
