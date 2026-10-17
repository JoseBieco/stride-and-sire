package com.josebieco.strideandsire.render;

import com.josebieco.strideandsire.StrideAndSire;
import com.josebieco.strideandsire.attachment.SaddlebagAccess;
import com.josebieco.strideandsire.item.saddlebag.SaddlebagItem;
import com.josebieco.strideandsire.item.saddlebag.SaddlebagMaterial;
import java.util.Locale;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.jspecify.annotations.Nullable;

/**
 * Common-side (no client classes) logic behind the saddlebag render layer {@code client.SaddlebagLayer}.
 * It lives outside the {@code client} package so the dedicated GameTest server can exercise it without loading
 * {@code RenderLayer}. On the client the equipped bag arrives via the tracking sync (without its container contents,
 * which is all this needs: the item, hence the material).
 */
public final class SaddlebagRendering {
    /** Submission order of the saddlebag layer (its own model): after vanilla's saddle and body armor (order 2). */
    public static final int LAYER_ORDER = 3;

    /**
     * Texture size of the 3D side-bag model ({@code client.SaddlebagModel}); every {@link #bagTexture} PNG has exactly
     * this size. Mirrored by {@code tools/pixelart/generate.py} and pinned by {@code AssetCompletenessTest}.
     */
    public static final int MODEL_TEXTURE_WIDTH = 32;
    public static final int MODEL_TEXTURE_HEIGHT = 16;

    private SaddlebagRendering() {}

    /** Where the 3D bags hang on the flanks ({@code client.SaddlebagModel}). */
    public enum Placement {
        /** Just behind the saddle, at mid flank. */
        FLANK,
        /** Lowered below a Donkey/Mule chest so the chest box does not swallow them. */
        UNDER_CHEST
    }

    /** The single placement rule, shared by the client model (from the render state) and {@link #placement}. */
    public static Placement placementFor(boolean hasChest) {
        return hasChest ? Placement.UNDER_CHEST : Placement.FLANK;
    }

    /** Placement for {@code horse}: under the chest for a chested Donkey/Mule, on the flank otherwise. */
    public static Placement placement(AbstractHorse horse) {
        return placementFor(horse instanceof AbstractChestedHorse chested && chested.hasChest());
    }

    /** The material whose bags to draw for {@code horse}, or {@code null} when nothing (or a non-saddlebag) is equipped. */
    public static @Nullable SaddlebagMaterial materialToRender(AbstractHorse horse) {
        return SaddlebagAccess.get(horse).getItem() instanceof SaddlebagItem item ? item.getMaterial() : null;
    }

    /**
     * {@code strideandsire:textures/entity/saddlebag/<tier>.png}: the {@link #MODEL_TEXTURE_WIDTH}x{@link #MODEL_TEXTURE_HEIGHT}
     * texture of the 3D side-bag model {@code client.SaddlebagModel}, laid out for its UVs.
     */
    public static Identifier bagTexture(SaddlebagMaterial material) {
        return Identifier.fromNamespaceAndPath(StrideAndSire.MODID,
            "textures/entity/saddlebag/" + material.name().toLowerCase(Locale.ROOT) + ".png");
    }
}
