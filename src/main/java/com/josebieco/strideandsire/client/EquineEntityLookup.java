package com.josebieco.strideandsire.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Render-state to entity bridge shared by the mod's equine render layers ({@link HorseshoeLayer}, {@link SaddlebagLayer}).
 *
 * <p>26.3 layers only see the {@link EquineRenderState}, Forge 66 has no hook for adding custom data to render states
 * (no render-data/extract event; {@code HorseRenderer} is final), and the mod's equipment lives in capabilities on the
 * entity. So the entity is resolved from the state: the client-level horse of the same {@code entityType} whose
 * (interpolated) position is closest to the state's position, within 1 block.
 */
final class EquineEntityLookup {
    private static final double MATCH_RADIUS = 1.0;

    private EquineEntityLookup() {}

    static @Nullable AbstractHorse findHorse(EquineRenderState state) {
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
