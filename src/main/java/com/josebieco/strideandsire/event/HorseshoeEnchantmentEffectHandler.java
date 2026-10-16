package com.josebieco.strideandsire.event;

import com.josebieco.strideandsire.attachment.HorseshoeAccess;
import com.josebieco.strideandsire.enchantment.HorseshoeEnchantments;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.ReplaceDisk;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;

/**
 * Effects of the three horseshoe enchantments. Every effect reads the enchantment level from the horse's own
 * horseshoe ({@link HorseshoeAccess#get}) through {@link HorseshoeEnchantments#effectiveLevel} (registry lookup via
 * the level's registry access, and only if the enchantment is eligible on that horseshoe's material).
 *
 * <ul>
 *   <li><b>Feather Step</b>: {@link LivingFallEvent} is cancelled for a feather-step horse and for any entity riding
 *       one (spec section 4: "do cavalo (e do jogador montado)"). Cancelling the horse's event also skips vanilla's
 *       {@code propagateFallToPassengers}.</li>
 *   <li><b>Frost Step</b> (server only): vanilla Frost Walker runs its {@code minecraft:replace_disk}
 *       {@code location_changed} effect from {@code LivingEntity.onChangedBlock} whenever the wearer enters a new
 *       block while on the ground and not riding anything. Horseshoes are not in an equipment slot, so that hook never
 *       sees them; instead the same {@link ReplaceDisk} (identical parameters to vanilla {@code frost_walker}) is
 *       applied at Frost Walker level {@value #FROST_WALKER_EQUIVALENT_LEVEL} when the horse is on the ground and
 *       has entered a new block. Frosted ice melts through its own vanilla block ticks (scheduled by
 *       {@code FrostedIceBlock.onPlace}); no task referencing the horse is ever scheduled.</li>
 *   <li><b>Lava Walker</b>: vanilla Striders stand on lava through {@code LivingEntity.canStandOnFluid} +
 *       {@code getLiquidCollisionShape}, which are plain overridable methods with no Forge hook (no event, no
 *       {@code IForgeEntity} method), and this MDK has no mixins. So the Strider behaviour is reproduced from
 *       {@link LivingEvent.LivingTickEvent}, which fires at the start of {@code LivingEntity.tick()}, i.e. before
 *       {@code baseTick}'s fluid update and before {@code aiStep}/{@code travel}: a horse whose hooves are in the top
 *       lava block, at most half a block below the surface (Strider's half-block liquid shape), is lifted onto the
 *       surface, its downward velocity is cancelled and it is marked on-ground (so it walks at ground speed and can
 *       jump). Deeper than that it floats up exactly like {@code Strider.floatStrider}. It runs only on the side that
 *       simulates the horse's movement ({@link AbstractHorse#canSimulateMovement()}: the server for free horses, the
 *       rider's client for a ridden horse), so it never fights interpolation. Fall distance is reset on every side
 *       while the horse is supported, so the server never accumulates it from the rider's vehicle packets.</li>
 * </ul>
 *
 * <p>Per-entity state: only {@link #LAST_FROST_POS} (the block a frost-step horse last froze from), a
 * {@link WeakHashMap} keyed by the horse (note: {@code Entity.equals/hashCode} are based on the entity id, not
 * identity; ids are unique per running server, so entries cannot collide in practice), touched only on the server thread, and cleaned when the horse dies,
 * loses the enchantment or leaves the level.
 */
public final class HorseshoeEnchantmentEffectHandler {
    /** Frost Step behaves like Frost Walker II (spec section 4). */
    public static final int FROST_WALKER_EQUIVALENT_LEVEL = 2;
    /** Strider's liquid collision shape is a half-block column: deeper than this it floats up instead of standing. */
    private static final double LAVA_STAND_MAX_DEPTH = 0.5;
    /** A horse this far above the lava surface counts as standing on it (absorbs float rounding). */
    private static final double LAVA_SURFACE_EPSILON = 1.0E-4;

    private static final Map<AbstractHorse, BlockPos> LAST_FROST_POS = new WeakHashMap<>();

    private HorseshoeEnchantmentEffectHandler() {}

    public static void register() {
        LivingFallEvent.BUS.addListener(HorseshoeEnchantmentEffectHandler::onLivingFall);
        LivingEvent.LivingTickEvent.BUS.addListener(HorseshoeEnchantmentEffectHandler::onLivingTick);
        EntityLeaveLevelEvent.BUS.addListener(HorseshoeEnchantmentEffectHandler::onLeaveLevel);
    }

    private static int level(AbstractHorse horse, ResourceKey<Enchantment> enchantment) {
        return HorseshoeEnchantments.effectiveLevel(horse.level(), HorseshoeAccess.get(horse), enchantment);
    }

    private static boolean isActive(AbstractHorse horse) {
        return horse.isAlive() && !horse.isRemoved();
    }

    private static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof AbstractHorse horse) || !HorseshoeAccess.isEligible(horse)) {
            return;
        }
        if (!isActive(horse)) {
            forgetFrostPosition(horse);
            return;
        }
        tickFrostStep(horse);
        tickLavaWalker(horse);
    }

    private static void onLeaveLevel(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof AbstractHorse horse) {
            forgetFrostPosition(horse);
        }
    }

    // ---- Feather Step ------------------------------------------------------------------------------------------

    /** Whether {@code horse} is alive and its horseshoe carries an eligible Feather Step. */
    public static boolean hasFeatherStep(AbstractHorse horse) {
        return isActive(horse) && level(horse, HorseshoeEnchantments.FEATHER_STEP) > 0;
    }

    private static boolean onLivingFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof AbstractHorse horse && hasFeatherStep(horse)) {
            if (wouldTakeFallDamage(horse, event.getDistance(), event.getDamageMultiplier())) {
                onFeatherStepNegatedFall(horse, event.getDistance());
            }
            return true;
        }
        // The rider: covers falls reaching a passenger by any path while it is mounted on a feather-step horse.
        return entity.getVehicle() instanceof AbstractHorse mount && hasFeatherStep(mount);
    }

    /** Mirrors {@code LivingEntity.calculateFallDamage} (protected) with public attribute reads. */
    private static boolean wouldTakeFallDamage(LivingEntity entity, double distance, float multiplier) {
        if (entity.is(EntityTypeTags.FALL_DAMAGE_IMMUNE)) {
            return false;
        }
        double power = distance + 1.0E-6 - entity.getAttributeValue(Attributes.SAFE_FALL_DISTANCE);
        return Mth.floor(power * multiplier * entity.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER)) > 0;
    }

    /**
     * Extension point: called once per fall that would have damaged the horse and was negated by Feather Step.
     * {@link LivingFallEvent} can fire on the client too (wherever vanilla computes the fall), so durability
     * consumption (Task 8) hooking in here must guard with {@code !horse.level().isClientSide()}.
     */
    private static void onFeatherStepNegatedFall(AbstractHorse horse, double fallDistance) {
        // Intentionally empty until Task 8.
    }

    // ---- Frost Step --------------------------------------------------------------------------------------------

    /** Vanilla {@code minecraft:frost_walker}'s {@code replace_disk} effect, parameter for parameter. */
    private static final class FrostDisk {
        static final ReplaceDisk DISK = new ReplaceDisk(
            new LevelBasedValue.Clamped(LevelBasedValue.perLevel(3.0F, 1.0F), 0.0F, 16.0F),
            LevelBasedValue.constant(1.0F),
            new Vec3i(0, -1, 0),
            Optional.of(BlockPredicate.allOf(
                BlockPredicate.matchesTag(Direction.UP, BlockTags.AIR),
                BlockPredicate.matchesBlocks(Blocks.WATER),
                BlockPredicate.matchesFluids(Fluids.WATER),
                BlockPredicate.unobstructed())),
            BlockStateProvider.holderOf(Blocks.FROSTED_ICE),
            Optional.of(GameEvent.BLOCK_PLACE));
    }

    private static void tickFrostStep(AbstractHorse horse) {
        if (!(horse.level() instanceof ServerLevel level)) {
            return;
        }
        ItemStack shoe = HorseshoeAccess.get(horse);
        if (HorseshoeEnchantments.effectiveLevel(level, shoe, HorseshoeEnchantments.FROST_STEP) <= 0) {
            forgetFrostPosition(horse);
            return;
        }
        // Vanilla requirements: on the ground, not riding anything; fires when the wearer enters a new block.
        if (!horse.onGround() || horse.isPassenger()) {
            return;
        }
        BlockPos pos = horse.blockPosition();
        if (pos.equals(LAST_FROST_POS.get(horse))) {
            return;
        }
        LAST_FROST_POS.put(horse, pos);
        FrostDisk.DISK.apply(level, FROST_WALKER_EQUIVALENT_LEVEL, new EnchantedItemInUse(shoe, null, horse, broken -> {}),
            horse, horse.position());
        onFrostStepFroze(horse);
    }

    /** Whether the frost handler currently holds per-entity state for {@code horse} (for tests / leak checks). */
    public static boolean isTrackingFrostPosition(AbstractHorse horse) {
        return LAST_FROST_POS.containsKey(horse);
    }

    private static void forgetFrostPosition(AbstractHorse horse) {
        if (!horse.level().isClientSide()) {
            LAST_FROST_POS.remove(horse);
        }
    }

    /**
     * Extension point: called (server side) each time Frost Step applies its freezing disk, i.e. whenever vanilla Frost
     * Walker's {@code location_changed} effect would fire. Durability consumption (Task 8) hooks in here.
     */
    private static void onFrostStepFroze(AbstractHorse horse) {
        // Intentionally empty until Task 8.
    }

    // ---- Lava Walker -------------------------------------------------------------------------------------------

    private static void tickLavaWalker(AbstractHorse horse) {
        if (horse.isPassenger() || horse.noPhysics || level(horse, HorseshoeEnchantments.LAVA_WALKER) <= 0) {
            return;
        }
        Level level = horse.level();
        BlockPos feet = horse.blockPosition();
        FluidState fluid = level.getFluidState(feet);
        if (!fluid.is(FluidTags.LAVA)) {
            return;
        }
        boolean simulating = horse.canSimulateMovement();
        Vec3 motion = horse.getDeltaMovement();
        double surface = feet.getY() + fluid.getHeight(level, feet);
        double y = horse.getY();
        boolean submerged = level.getFluidState(feet.above()).is(FluidTags.LAVA);
        if (submerged || surface - y > LAVA_STAND_MAX_DEPTH) {
            if (simulating) {
                // Strider.floatStrider: rise towards the surface.
                horse.setDeltaMovement(motion.scale(0.5).add(0.0, 0.05, 0.0));
            }
            return;
        }
        if (y > surface + LAVA_SURFACE_EPSILON) {
            return; // airborne above the lava (e.g. mid-jump)
        }
        if (simulating) {
            if (y < surface) {
                horse.setPos(horse.getX(), surface, horse.getZ());
            }
            if (motion.y < 0.0) {
                horse.setDeltaMovement(motion.x, 0.0, motion.z);
            }
            horse.setOnGround(true);
        }
        // On every side, including the server for a rider-controlled horse: there, movement arrives through
        // ServerboundMoveVehiclePacket, whose handler runs doCheckFallDamage with the client's onGround flag. Over lava
        // that flag is false (vanilla move() clears it after travel), so every downward step (e.g. each charged jump
        // landing back on the lava) would add to fallDistance, never be reset, and hurt horse and rider on reaching
        // land. Standing on lava is not falling.
        horse.resetFallDistance();
        if (!level.isClientSide()) {
            onLavaWalkerSupported(horse);
        }
    }

    /**
     * Extension point: called once per server tick while Lava Walker holds the horse on a lava surface (also for a
     * horse whose movement is simulated by its rider's client). Durability consumption (Task 8) hooks in here.
     */
    private static void onLavaWalkerSupported(AbstractHorse horse) {
        // Intentionally empty until Task 8.
    }
}
