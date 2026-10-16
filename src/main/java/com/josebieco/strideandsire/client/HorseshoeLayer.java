package com.josebieco.strideandsire.client;

import com.josebieco.strideandsire.item.horseshoe.HorseshoeMaterial;
import com.josebieco.strideandsire.render.HorseshoeRendering;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the equipped horseshoe on the four hooves of Horse, Donkey, Mule, Skeleton Horse and Zombie Horse.
 *
 * <p>Rendering: the parent (adult equine) model is re-submitted with a per-tier cutout overlay texture
 * ({@link HorseshoeRendering#hoofTexture}) that is transparent except for the hoof area of the leg UVs
 * ({@code texOffs(48, 21)}, 4x11x4 boxes: the hoof underside and the bottom two rows of each side), the same
 * technique vanilla's {@code HorseMarkingLayer} uses. Foals are skipped: their baby models use a different UV layout.
 *
 * <p>Render-state bridge: 26.3 layers only see the {@link EquineRenderState}, Forge 66 has no hook for adding
 * custom data to render states (no render-data/extract event; {@code HorseRenderer} is final), and the horseshoe lives
 * in a capability on the entity. So the layer resolves the entity from the state: the client-level horse of the
 * same {@code entityType} whose (interpolated) position is closest to the state's position, within 1 block.
 * The tier decision itself is {@link HorseshoeRendering#materialToRender}, which is common code and GameTest-covered.
 */
public class HorseshoeLayer<S extends EquineRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
    /** Same submission order as vanilla horse markings: above the base model, below armor and saddle (order 2). */
    private static final int ORDER = 1;
    private static final double MATCH_RADIUS = 1.0;

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
        AbstractHorse horse = findHorse(state);
        HorseshoeMaterial material = horse == null ? null : materialToRender(horse);
        if (material == null) {
            return;
        }
        renderColoredCutoutModel(this.getParentModel(), HorseshoeRendering.hoofTexture(material), poseStack, submitNodeCollector,
            lightCoords, state, -1, ORDER);
    }

    private static @Nullable AbstractHorse findHorse(EquineRenderState state) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return null;
        }
        Vec3 statePos = new Vec3(state.x, state.y, state.z);
        float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        List<AbstractHorse> candidates = level.getEntitiesOfClass(AbstractHorse.class,
            AABB.ofSize(statePos, 2 * MATCH_RADIUS, 2 * MATCH_RADIUS, 2 * MATCH_RADIUS), horse -> horse.getType() == state.entityType);
        AbstractHorse best = null;
        double bestDistance = MATCH_RADIUS * MATCH_RADIUS;
        for (AbstractHorse horse : candidates) {
            // The world pass extracts with the frame's partial tick; GUI previews extract with partial tick 1 (current pos).
            Vec3 lerped = new Vec3(Mth.lerp(partialTick, horse.xOld, horse.getX()), Mth.lerp(partialTick, horse.yOld, horse.getY()),
                Mth.lerp(partialTick, horse.zOld, horse.getZ()));
            double distance = Math.min(lerped.distanceToSqr(statePos), horse.position().distanceToSqr(statePos));
            if (distance <= bestDistance) {
                best = horse;
                bestDistance = distance;
            }
        }
        return best;
    }
}
