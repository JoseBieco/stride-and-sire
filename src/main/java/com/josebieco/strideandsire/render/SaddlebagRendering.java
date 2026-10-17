package com.josebieco.strideandsire.render;

import com.josebieco.strideandsire.StrideAndSire;
import com.josebieco.strideandsire.attachment.SaddlebagAccess;
import com.josebieco.strideandsire.item.saddlebag.SaddlebagItem;
import com.josebieco.strideandsire.item.saddlebag.SaddlebagMaterial;
import java.util.Locale;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.jspecify.annotations.Nullable;

/**
 * Common-side (no client classes) logic behind the saddlebag render layer {@code client.SaddlebagLayer}.
 * It lives outside the {@code client} package so the dedicated GameTest server can exercise it without loading
 * {@code RenderLayer}. On the client the equipped bag arrives via the tracking sync (without its container contents,
 * which is all this needs: the item, hence the material).
 */
public final class SaddlebagRendering {
    /**
     * Texture size of the 3D side-bag model ({@code client.SaddlebagModel}); every {@link #bagTexture} PNG has exactly
     * this size. Mirrored by {@code tools/pixelart/generate.py} and pinned by {@code AssetCompletenessTest}.
     */
    public static final int MODEL_TEXTURE_WIDTH = 32;
    public static final int MODEL_TEXTURE_HEIGHT = 16;

    private SaddlebagRendering() {}

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
