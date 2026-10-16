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
    private SaddlebagRendering() {}

    /** The material whose bags to draw for {@code horse}, or {@code null} when nothing (or a non-saddlebag) is equipped. */
    public static @Nullable SaddlebagMaterial materialToRender(AbstractHorse horse) {
        return SaddlebagAccess.get(horse).getItem() instanceof SaddlebagItem item ? item.getMaterial() : null;
    }

    /**
     * {@code strideandsire:textures/entity/saddlebag/<tier>.png}: a 64x64 overlay in the adult equine UV layout, opaque
     * only on the two body-side faces just behind the saddle.
     */
    public static Identifier bagTexture(SaddlebagMaterial material) {
        return Identifier.fromNamespaceAndPath(StrideAndSire.MODID,
            "textures/entity/saddlebag/" + material.name().toLowerCase(Locale.ROOT) + ".png");
    }
}
