package com.josebieco.strideandsire.render;

import com.josebieco.strideandsire.StrideAndSire;
import com.josebieco.strideandsire.attachment.HorseCoatGeneticsAccess;
import com.josebieco.strideandsire.attachment.HorseCoatGeneticsAttachment;
import com.josebieco.strideandsire.genetics.CoatGene;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.jspecify.annotations.Nullable;

/**
 * Common-side (no client classes) logic behind the recessive coat override layer {@code client.HorseCoatOverrideLayer}.
 * Lives outside the {@code client} package so unit tests and the dedicated GameTest server can exercise it without
 * loading {@code RenderLayer}.
 */
public final class HorseCoatRendering {
    private HorseCoatRendering() {}

    /**
     * Returns the full-coat override texture for {@code horse}, or {@code null} when no recessive coat gene is
     * manifested (or when {@code horse} is ineligible / {@code null}).
     */
    public static @Nullable Identifier textureToRender(@Nullable AbstractHorse horse) {
        if (horse == null) {
            return null;
        }
        return textureToRender(HorseCoatGeneticsAccess.get(horse));
    }

    /**
     * Returns the full-coat override texture for {@code attachment}, or {@code null} when
     * {@link HorseCoatGeneticsAttachment#manifestGeneToRender()} is {@code null}.
     */
    public static @Nullable Identifier textureToRender(@Nullable HorseCoatGeneticsAttachment attachment) {
        if (attachment == null) {
            return null;
        }
        CoatGene gene = attachment.manifestGeneToRender();
        return gene == null ? null : coatTexture(gene);
    }

    /** {@code strideandsire:textures/entity/horse/<gene>.png}: a 64x64 full-coat texture in the adult horse UV layout. */
    public static Identifier coatTexture(CoatGene gene) {
        return Identifier.fromNamespaceAndPath(
            StrideAndSire.MODID,
            "textures/entity/horse/" + gene.getSerializedName() + ".png");
    }
}
