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
    /**
     * Submission order of the coat override layer: the same band as vanilla's {@code HorseMarkingLayer} (1), strictly
     * below {@link HorseshoeRendering#LAYER_ORDER} so hooves drawn over the same parent model always win the coplanar
     * depth test.
     */
    public static final int LAYER_ORDER = 1;

    private HorseCoatRendering() {}

    /**
     * The texture the override layer draws for one render pass, or {@code null} for none: foals (their baby model
     * uses a different UV layout) and invisible horses never get the override.
     */
    public static @Nullable Identifier overrideTextureFor(boolean isBaby, boolean isInvisible, @Nullable HorseCoatGeneticsAttachment attachment) {
        return isBaby || isInvisible ? null : textureToRender(attachment);
    }

    /** {@link #overrideTextureFor(boolean, boolean, HorseCoatGeneticsAttachment)} for a (possibly unresolved) horse. */
    public static @Nullable Identifier overrideTextureFor(boolean isBaby, boolean isInvisible, @Nullable AbstractHorse horse) {
        return overrideTextureFor(isBaby, isInvisible, horse == null ? null : HorseCoatGeneticsAccess.get(horse));
    }

    /**
     * Whether vanilla horse markings must be hidden for this render pass: exactly when the full exclusive-coat override
     * is drawn (spec: the exclusive coat replaces the vanilla colour and markings completely and is never combined with
     * a vanilla marking). Needed because {@code HorseMarkingLayer} draws translucent, i.e. after every solid-phase
     * cutout submit regardless of order, so it would land on top of the coat.
     */
    public static boolean suppressesVanillaMarkings(@Nullable Identifier overrideTexture) {
        return overrideTexture != null;
    }

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
