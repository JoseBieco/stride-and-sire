package com.josebieco.strideandsire.client;

import com.josebieco.strideandsire.render.HorseCoatRendering;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.jspecify.annotations.Nullable;

/**
 * Draws the manifested recessive coat texture ({@link HorseCoatRendering#textureToRender}) over the adult {@code Horse}
 * model, visually replacing both the vanilla base coat and vanilla markings while staying below horseshoes, armor,
 * saddle, and saddlebags.
 *
 * <p>Attached only to {@code EntityTypes.HORSE} before {@link HorseshoeLayer}, so at submission order {@value #ORDER}
 * it renders above {@code HorseMarkingLayer} and beneath {@link HorseshoeLayer}. Foals are skipped because their baby
 * models use a different UV layout.
 */
public class HorseCoatOverrideLayer<S extends EquineRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
    /** Same submission order band as markings and horseshoes; registered before HorseshoeLayer so hooves draw on top. */
    private static final int ORDER = 1;

    public HorseCoatOverrideLayer(RenderLayerParent<S, M> renderer) {
        super(renderer);
    }

    static @Nullable Identifier textureToRender(AbstractHorse horse) {
        return HorseCoatRendering.textureToRender(horse);
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
        if (state.isBaby || state.isInvisible) {
            return;
        }
        AbstractHorse horse = EquineEntityLookup.findHorse(state);
        Identifier texture = horse == null ? null : textureToRender(horse);
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
