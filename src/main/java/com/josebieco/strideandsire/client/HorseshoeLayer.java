package com.josebieco.strideandsire.client;

import com.josebieco.strideandsire.item.horseshoe.HorseshoeMaterial;
import com.josebieco.strideandsire.render.HorseshoeRendering;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.jspecify.annotations.Nullable;

/**
 * Draws the equipped horseshoe on the four hooves of Horse, Donkey, Mule, Skeleton Horse and Zombie Horse.
 *
 * <p>Rendering: the parent (adult equine) model is re-submitted with a per-tier cutout overlay texture
 * ({@link HorseshoeRendering#hoofTexture}) that is transparent except for the hoof area of the leg UVs
 * ({@code texOffs(48, 21)}, 4x11x4 boxes: the hoof underside and the bottom two rows of each side), the same
 * technique vanilla's {@code HorseMarkingLayer} uses. Foals are skipped: their baby models use a different UV layout.
 *
 * <p>Render-state bridge: the horse is resolved from the state by {@link EquineEntityLookup#findHorse}.
 * The tier decision itself is {@link HorseshoeRendering#materialToRender}, which is common code and GameTest-covered.
 */
public class HorseshoeLayer<S extends EquineRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
    /** Same submission order as vanilla horse markings: above the base model, below armor and saddle (order 2). */
    private static final int ORDER = 1;

    public HorseshoeLayer(RenderLayerParent<S, M> renderer) {
        super(renderer);
    }

    /** The visibility predicate: {@code null} means draw nothing. Delegates to the common, testable seam. */
    static @Nullable HorseshoeMaterial materialToRender(AbstractHorse horse) {
        return HorseshoeRendering.materialToRender(horse);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, S state, float yRot, float xRot) {
        if (state.isBaby || state.isInvisible) {
            return;
        }
        AbstractHorse horse = EquineEntityLookup.findHorse(state);
        HorseshoeMaterial material = horse == null ? null : materialToRender(horse);
        if (material == null) {
            return;
        }
        renderColoredCutoutModel(this.getParentModel(), HorseshoeRendering.hoofTexture(material), poseStack, submitNodeCollector,
            lightCoords, state, -1, ORDER);
    }
}
