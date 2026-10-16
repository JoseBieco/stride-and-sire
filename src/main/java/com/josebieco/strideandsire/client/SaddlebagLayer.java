package com.josebieco.strideandsire.client;

import com.josebieco.strideandsire.item.saddlebag.SaddlebagMaterial;
import com.josebieco.strideandsire.render.SaddlebagRendering;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.jspecify.annotations.Nullable;

/**
 * Draws the equipped saddlebag as a pair of small bags on the flanks of Horse, Donkey, Mule, Skeleton Horse and
 * Zombie Horse.
 *
 * <p>Rendering: like {@link HorseshoeLayer}, the parent (adult equine) model is re-submitted with a per-tier cutout
 * overlay texture ({@link SaddlebagRendering#bagTexture}) that is transparent except for a small patch on each side
 * face of the body box ({@code texOffs(0, 32)}, 10x10x22, UV side strips u 0..22 and u 32..54 at v 54..64). The patch
 * sits just behind the saddle (body-local z 1..5): the saddle box ({@code EquineSaddleModel}, z -9..0 inflated by 0.5)
 * covers almost the full body height, so anything painted under it would be hidden. Drawn only for saddled adults:
 * foals use a different UV layout, and a bag without a saddle should not happen (guarded anyway).
 *
 * <p>Render-state bridge: the horse is resolved from the state by {@link EquineEntityLookup#findHorse}.
 * The tier decision itself is {@link SaddlebagRendering#materialToRender}, which is common code and GameTest-covered.
 */
public class SaddlebagLayer<S extends EquineRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
    /** Submitted after vanilla's saddle and body-armor layers (order 2). */
    private static final int ORDER = 3;

    public SaddlebagLayer(RenderLayerParent<S, M> renderer) {
        super(renderer);
    }

    /** The visibility predicate: {@code null} means draw nothing. Delegates to the common, testable seam. */
    static @Nullable SaddlebagMaterial materialToRender(AbstractHorse horse) {
        return SaddlebagRendering.materialToRender(horse);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, S state, float yRot, float xRot) {
        if (state.isBaby || state.isInvisible || state.saddle.isEmpty()) {
            return;
        }
        AbstractHorse horse = EquineEntityLookup.findHorse(state);
        SaddlebagMaterial material = horse == null ? null : materialToRender(horse);
        if (material == null) {
            return;
        }
        renderColoredCutoutModel(this.getParentModel(), SaddlebagRendering.bagTexture(material), poseStack, submitNodeCollector,
            lightCoords, state, -1, ORDER);
    }
}
