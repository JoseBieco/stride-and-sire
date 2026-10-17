package com.josebieco.strideandsire.client;

import com.josebieco.strideandsire.render.HorseCoatRendering;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.client.renderer.entity.state.HorseRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.equine.Markings;
import org.jspecify.annotations.Nullable;

/**
 * Draws the manifested exclusive coat texture ({@link HorseCoatRendering#overrideTextureFor}) over the adult
 * {@code Horse} model as a full override of the vanilla base coat, staying below horseshoes, armor, saddle and
 * saddlebags.
 *
 * <p>Ordering: submitted at order {@value HorseCoatRendering#LAYER_ORDER} with an opaque cutout render type (solid
 * phase), so it covers the base model (order 0) and is covered by {@link HorseshoeLayer} (order 2, same parent model,
 * later in the solid phase). Vanilla's {@code HorseMarkingLayer} draws translucent at order 1, and the dispatcher runs
 * every order's solid phase before any translucent phase, so markings would land on top of this layer whatever its
 * order. They are therefore suppressed instead: {@link #suppressVanillaMarkings} (called from a
 * {@code RenderLivingEvent.Pre} listener, i.e. after state extraction and before any layer submits) sets the state's
 * markings to {@link Markings#NONE} exactly when this layer will draw
 * ({@link HorseCoatRendering#suppressesVanillaMarkings}). Foals are skipped because their baby models use a different
 * UV layout, and keep their vanilla markings.
 */
public class HorseCoatOverrideLayer<S extends EquineRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
    private static final int ORDER = HorseCoatRendering.LAYER_ORDER;

    public HorseCoatOverrideLayer(RenderLayerParent<S, M> renderer) {
        super(renderer);
    }

    static @Nullable Identifier textureToRender(EquineRenderState state) {
        if (state.isBaby || state.isInvisible) {
            return null; // skip the entity lookup; overrideTextureFor returns null for these anyway
        }
        return HorseCoatRendering.overrideTextureFor(state.isBaby, state.isInvisible, EquineEntityLookup.findHorse(state));
    }

    /** Hides vanilla markings for this frame when the exclusive coat override will be drawn over {@code state}. */
    static void suppressVanillaMarkings(HorseRenderState state) {
        if (state.markings != Markings.NONE && HorseCoatRendering.suppressesVanillaMarkings(textureToRender(state))) {
            state.markings = Markings.NONE;
        }
    }

    @Override
    public void submit(
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        int lightCoords,
        S state,
        float yRot,
        float xRot
    ) {
        Identifier texture = textureToRender(state);
        if (texture == null) {
            return;
        }
        renderColoredCutoutModel(
            this.getParentModel(),
            texture,
            poseStack,
            submitNodeCollector,
            lightCoords,
            state,
            -1,
            ORDER);
    }
}
