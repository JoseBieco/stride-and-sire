package com.josebieco.strideandsire.event;

import com.josebieco.strideandsire.StrideAndSire;
import com.josebieco.strideandsire.attachment.HorseshoeAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;

/**
 * Horseshoe wear (spec section 4): the equipped horseshoe loses durability when its effects do work, and breaks
 * (the slot empties) when it runs out. Mending repairs it from XP its rider picks up.
 *
 * <p><b>Triggers</b> (server side only; the Task 6/7 effect hooks delegate here through {@link #consume}):
 * <ul>
 *   <li>{@link Trigger#FEATHER_STEP_FALL}: 1 point per fall that would have hurt and was negated.</li>
 *   <li>{@link Trigger#FIRE_NEGATED}: 1 point per fire/lava damage instance negated by the Netherite passive,
 *       throttled (that event fires every tick while exposed).</li>
 *   <li>{@link Trigger#FROST_STEP_FREEZING}: 1 point per {@value #THROTTLE_TICKS} ticks while Frost Step freezes
 *       under the moving horse (see {@link #isOverFrostedIce}).</li>
 *   <li>{@link Trigger#LAVA_WALKER_SUPPORT}: 1 point per {@value #THROTTLE_TICKS} ticks while Lava Walker holds the
 *       horse up.</li>
 *   <li>{@link Trigger#RIDING}: 1 point per {@value #RIDING_BLOCKS_PER_POINT} blocks travelled horizontally while the
 *       horse is ridden (has a controlling passenger) and on the ground, every tier (see {@link #tickRidingWear}).</li>
 * </ul>
 * Throttled triggers allow at most one point per {@value #THROTTLE_TICKS} game ticks per horse and per
 * {@link Channel}. Fire negation and Lava Walker share the {@link Channel#HEAT} window, so a netherite horse walking
 * on lava (supported and possibly touching lava/fire every tick) pays one point per second, not two. The window is
 * per entity, stored as the last-consumed game time in the horse's own Forge persistent data (no static state).
 *
 * <p><b>Damage path</b>: vanilla {@link ItemStack#hurtAndBreak(int, ServerLevel, net.minecraft.server.level.ServerPlayer,
 * java.util.function.Consumer)}, which rolls Unbreaking ({@code EnchantmentHelper.processDurabilityChange}) and Forge's
 * {@code IForgeItem.damageItem}. The (possibly emptied) stack is always written back through
 * {@link HorseshoeAccess#set} so clients sync and equip side effects (e.g. removing the Gold speed modifier) run.
 *
 * <p><b>Mending</b>: vanilla only repairs the player's own equipment. XP that reaches a rider after vanilla's own
 * Mending pass (Forge {@link PlayerXpEvent.XpChange}, scoped to orb pickups by {@link PlayerXpEvent.PickupXp})
 * repairs the mount's damaged Mending horseshoe at vanilla's rate
 * ({@link EnchantmentHelper#modifyDurabilityToRepairFromXp}, i.e. the {@code minecraft:repair_with_xp} effect) and
 * is consumed the same way {@code ExperienceOrb.repairPlayerItems} consumes it. The rider's own gear has priority.
 */
public final class HorseshoeDurabilityHandler {
    /** Throttle window for continuous triggers: at most one point per this many game ticks. */
    public static final int THROTTLE_TICKS = 20;
    /** Riding wear: one point per this many blocks travelled horizontally, ridden and on the ground. */
    public static final int RIDING_BLOCKS_PER_POINT = 100;
    /** A horizontal move longer than this in one tick is a teleport, not riding, and is not counted. */
    public static final double MAX_RIDING_STEP = 8.0;

    private static final String RIDE_LAST_X_KEY = StrideAndSire.MODID + ".riding_wear.last_x";
    private static final String RIDE_LAST_Z_KEY = StrideAndSire.MODID + ".riding_wear.last_z";
    private static final String RIDE_DISTANCE_KEY = StrideAndSire.MODID + ".riding_wear.distance";

    private static final String PICKUP_TICK_KEY = StrideAndSire.MODID + ".xp_orb_pickup_tick";

    /** Throttle windows; triggers in the same channel share one window. */
    public enum Channel {
        NONE, HEAT, FROST;

        String key() {
            return StrideAndSire.MODID + ".durability_throttle." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public enum Trigger {
        FEATHER_STEP_FALL(Channel.NONE),
        FIRE_NEGATED(Channel.HEAT),
        FROST_STEP_FREEZING(Channel.FROST),
        LAVA_WALKER_SUPPORT(Channel.HEAT),
        /** Distance-based, metered by {@link #tickRidingWear}; no time throttle. */
        RIDING(Channel.NONE);

        private final Channel channel;

        Trigger(Channel channel) {
            this.channel = channel;
        }

        public Channel channel() {
            return channel;
        }
    }

    private HorseshoeDurabilityHandler() {}

    public static void register() {
        PlayerXpEvent.PickupXp.BUS.addListener(HorseshoeDurabilityHandler::onPickupXp);
        PlayerXpEvent.XpChange.BUS.addListener(HorseshoeDurabilityHandler::onXpChange);
        LivingEvent.LivingTickEvent.BUS.addListener(HorseshoeDurabilityHandler::onLivingTick);
    }

    // ---- wear ----------------------------------------------------------------------------------------------------

    /**
     * Applies one point of wear for {@code trigger} to {@code horse}'s horseshoe, unless the trigger's throttle
     * window is still closed. Server side only (no-op on the client, on ineligible entities and on an empty or
     * undamageable slot). Returns whether a point was applied (Unbreaking may still have negated it).
     */
    public static boolean consume(AbstractHorse horse, Trigger trigger) {
        if (!(horse.level() instanceof ServerLevel level)) {
            return false;
        }
        ItemStack shoe = HorseshoeAccess.get(horse);
        if (shoe.isEmpty() || !shoe.isDamageableItem()) {
            return false;
        }
        if (trigger.channel() != Channel.NONE && !tryOpenWindow(horse, trigger.channel(), level.getGameTime())) {
            return false;
        }
        damage(horse, level, shoe, 1);
        return true;
    }

    /** Per-horse throttle: opens (and records) the channel's window if {@link #THROTTLE_TICKS} have passed. */
    private static boolean tryOpenWindow(AbstractHorse horse, Channel channel, long now) {
        CompoundTag data = horse.getPersistentData();
        String key = channel.key();
        if (data.contains(key)) {
            long last = data.getLongOr(key, Long.MIN_VALUE);
            // A game time behind the stored one (e.g. /time set) reopens the window instead of locking it.
            if (now >= last && now - last < THROTTLE_TICKS) {
                return false;
            }
        }
        data.putLong(key, now);
        return true;
    }

    private static void damage(AbstractHorse horse, ServerLevel level, ItemStack shoe, int amount) {
        shoe.hurtAndBreak(amount, level, null, broken -> playBreakEffects(horse, level, broken));
        // hurtAndBreak shrinks a broken stack to empty; set() maps that to an empty slot and syncs either way.
        HorseshoeAccess.set(horse, shoe.isEmpty() ? ItemStack.EMPTY : shoe);
    }

    /** Vanilla's item-break feedback ({@code LivingEntity.breakItem}), played at the horse from the server. */
    private static void playBreakEffects(AbstractHorse horse, ServerLevel level, ItemStack broken) {
        if (!horse.isSilent()) {
            Holder<SoundEvent> sound = broken.get(DataComponents.BREAK_SOUND);
            level.playSound(null, horse.getX(), horse.getY(), horse.getZ(),
                sound != null ? sound : SoundEvents.ITEM_BREAK, horse.getSoundSource(),
                0.8F, 0.8F + horse.getRandom().nextFloat() * 0.4F);
        }
        horse.spawnItemParticles(broken, 5);
    }

    /**
     * Whether Frost Step is actually carrying the horse over water: some block under its hooves (its footprint one
     * block down) is frosted ice. Walking on land, even next to water that gets frozen, does not count.
     */
    public static boolean isOverFrostedIce(AbstractHorse horse) {
        AABB box = horse.getBoundingBox();
        int y = Mth.floor(box.minY - 1.0E-3);
        for (BlockPos pos : BlockPos.betweenClosed(
                Mth.floor(box.minX), y, Mth.floor(box.minZ), Mth.floor(box.maxX - 1.0E-7), y, Mth.floor(box.maxZ - 1.0E-7))) {
            if (horse.level().getBlockState(pos).is(Blocks.FROSTED_ICE)) {
                return true;
            }
        }
        return false;
    }

    // ---- riding wear -----------------------------------------------------------------------------------------------

    private static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof AbstractHorse horse && !horse.level().isClientSide() && HorseshoeAccess.isEligible(horse)) {
            tickRidingWear(horse);
        }
    }

    /**
     * Meters riding wear for {@code horse} (server side, once per tick): the horizontal distance since the previous
     * tick is added to a per-horse accumulator (Forge persistent data, like the throttle windows) while the horse is
     * ridden and on the ground; every {@value #RIDING_BLOCKS_PER_POINT} blocks cost one point through {@link #consume}.
     * Steps longer than {@value #MAX_RIDING_STEP} blocks (teleports) are skipped. Nothing is tracked, and stale state is
     * dropped, while the horse has no damageable horseshoe.
     */
    public static void tickRidingWear(AbstractHorse horse) {
        CompoundTag data = horse.getPersistentData();
        ItemStack shoe = HorseshoeAccess.get(horse);
        if (!horse.isAlive() || shoe.isEmpty() || !shoe.isDamageableItem()) {
            data.remove(RIDE_LAST_X_KEY);
            data.remove(RIDE_LAST_Z_KEY);
            data.remove(RIDE_DISTANCE_KEY);
            return;
        }
        double x = horse.getX();
        double z = horse.getZ();
        boolean hasLast = data.contains(RIDE_LAST_X_KEY) && data.contains(RIDE_LAST_Z_KEY);
        double step = hasLast ? Math.hypot(x - data.getDoubleOr(RIDE_LAST_X_KEY, x), z - data.getDoubleOr(RIDE_LAST_Z_KEY, z)) : 0.0;
        data.putDouble(RIDE_LAST_X_KEY, x);
        data.putDouble(RIDE_LAST_Z_KEY, z);
        if (step <= 0.0 || step > MAX_RIDING_STEP || horse.getControllingPassenger() == null || !horse.onGround()) {
            return;
        }
        double distance = data.getDoubleOr(RIDE_DISTANCE_KEY, 0.0) + step;
        if (distance >= RIDING_BLOCKS_PER_POINT) {
            distance -= RIDING_BLOCKS_PER_POINT;
            data.putDouble(RIDE_DISTANCE_KEY, distance);
            consume(horse, Trigger.RIDING); // may break the shoe; the next tick then drops the state
        } else {
            data.putDouble(RIDE_DISTANCE_KEY, distance);
        }
    }

    // ---- Mending ---------------------------------------------------------------------------------------------------

    /** Marks this tick as an orb pickup for {@code player}, so the following {@link PlayerXpEvent.XpChange} is scoped. */
    private static void onPickupXp(PlayerXpEvent.PickupXp event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide()) {
            player.getPersistentData().putLong(PICKUP_TICK_KEY, player.level().getGameTime());
        }
    }

    /**
     * Fired by {@code Player.giveExperiencePoints} with what is left of an orb after vanilla repaired the player's own
     * Mending gear. Spends it on the mount's horseshoe and passes on only the remainder.
     */
    private static void onXpChange(PlayerXpEvent.XpChange event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level) || event.getAmount() <= 0) {
            return;
        }
        CompoundTag data = player.getPersistentData();
        if (!data.contains(PICKUP_TICK_KEY)) {
            return;
        }
        long pickupTick = data.getLongOr(PICKUP_TICK_KEY, Long.MIN_VALUE);
        data.remove(PICKUP_TICK_KEY);
        if (pickupTick != level.getGameTime() || !(player.getVehicle() instanceof AbstractHorse horse)) {
            return;
        }
        event.setAmount(repairWithXp(horse, level, event.getAmount()));
    }

    /**
     * Repairs {@code horse}'s damaged Mending horseshoe with {@code xp} points exactly like
     * {@code ExperienceOrb.repairPlayerItems}; returns the XP left over.
     */
    public static int repairWithXp(AbstractHorse horse, ServerLevel level, int xp) {
        ItemStack shoe = HorseshoeAccess.get(horse);
        if (shoe.isEmpty() || !shoe.isDamaged() || !EnchantmentHelper.has(shoe, EnchantmentEffectComponents.REPAIR_WITH_XP)) {
            return xp;
        }
        int toRepair = EnchantmentHelper.modifyDurabilityToRepairFromXp(level, shoe, xp);
        int repair = Math.min(toRepair, shoe.getDamageValue());
        if (repair <= 0) {
            return xp;
        }
        shoe.setDamageValue(shoe.getDamageValue() - repair);
        HorseshoeAccess.set(horse, shoe);
        return Math.max(0, xp - repair * xp / toRepair);
    }
}
