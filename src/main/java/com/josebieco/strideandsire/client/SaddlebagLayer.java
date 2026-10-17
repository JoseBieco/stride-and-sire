package com.josebieco.strideandsire.client;

import com.josebieco.strideandsire.item.saddlebag.SaddlebagMaterial;
import com.josebieco.strideandsire.render.SaddlebagRendering;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.jspecify.annotations.Nullable;

/**
 * Draws the equipped saddlebag as two 3D side bags ({@link SaddlebagModel}) on Horse, Donkey, Mule, Skeleton Horse and
 * Zombie Horse.
 *
 * <p>Rendering mirrors vanilla's equine saddle layer: a separate model sharing the equine part tree, animated from the
 * same render state, submitted with the per-tier texture {@link SaddlebagRendering#bagTexture}. The parent renderer's
 * adult model is scaled per type through its root pose (horse 1.1, donkey 0.87, mule 0.92); that pose is copied onto
 * the bag model's root here, at construction time, when {@code getModel()} is still the adult model (renderers are
 * built, and this layer added, on every resource reload before anything is drawn).
 *
 * <p>Drawn only for saddled, visible adults (foals use a different model; a bag without a saddle should not happen but
 * is guarded anyway). Render-state bridge: the horse is resolved from the state by {@link EquineEntityLookup#findHorse}.
 * The tier decision itself is {@link SaddlebagRendering#materialToRender}, which is common code and GameTest-covered.
 */
public class SaddlebagLayer<S extends EquineRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
    /** Submitted after vanilla's saddle and body-armor layers (order 2). */
    private static final int ORDER = 3;

    private final SaddlebagModel model;

    public SaddlebagLayer(RenderLayerParent<S, M> renderer, EntityModelSet models) {
        super(renderer);
        ModelPart root = models.bakeLayer(SaddlebagModel.LAYER);
        root.setInitialPose(renderer.getModel().root().getInitialPose());
        root.resetPose();
        this.model = new SaddlebagModel(root);
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
        renderColoredCutoutModel(this.model, SaddlebagRendering.bagTexture(material), poseStack, submitNodeCollector,
            lightCoords, state, -1, ORDER);
    }
}
