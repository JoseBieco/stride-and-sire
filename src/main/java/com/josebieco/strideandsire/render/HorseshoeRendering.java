package com.josebieco.strideandsire.render;

import com.josebieco.strideandsire.StrideAndSire;
import com.josebieco.strideandsire.attachment.HorseshoeAccess;
import com.josebieco.strideandsire.item.horseshoe.HorseshoeItem;
import com.josebieco.strideandsire.item.horseshoe.HorseshoeMaterial;
import java.util.Locale;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.jspecify.annotations.Nullable;

/**
 * Common-side (no client classes) logic behind the hoof render layer {@code client.HorseshoeLayer}.
 * It lives outside the {@code client} package so the dedicated GameTest server can exercise it without loading
 * {@code RenderLayer}.
 */
public final class HorseshoeRendering {
    private HorseshoeRendering() {}

    /** The material whose hooves to draw for {@code horse}, or {@code null} when nothing (or a non-horseshoe) is equipped. */
    public static @Nullable HorseshoeMaterial materialToRender(AbstractHorse horse) {
        return HorseshoeAccess.get(horse).getItem() instanceof HorseshoeItem item ? item.getMaterial() : null;
    }

    /** {@code strideandsire:textures/entity/horseshoe/<tier>.png}: a 64x64 overlay in the adult equine UV layout. */
    public static Identifier hoofTexture(HorseshoeMaterial material) {
        return Identifier.fromNamespaceAndPath(StrideAndSire.MODID,
            "textures/entity/horseshoe/" + material.name().toLowerCase(Locale.ROOT) + ".png");
    }
}
