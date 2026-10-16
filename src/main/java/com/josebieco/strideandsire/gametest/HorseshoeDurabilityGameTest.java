package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.attachment.HorseshoeAccess;
import com.josebieco.strideandsire.enchantment.HorseshoeEnchantments;
import com.josebieco.strideandsire.event.HorseshoeDurabilityHandler;
import com.josebieco.strideandsire.event.HorseshoePassiveEffectHandler;
import com.josebieco.strideandsire.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Task 8 durability loss, breakage and Mending. Test instances: data/strideandsire/test_instance/horseshoe_durability_*.json
 */
public final class HorseshoeDurabilityGameTest {
    private HorseshoeDurabilityGameTest() {}

    @SafeVarargs
    private static ItemStack shoe(GameTestHelper helper, Item item, ResourceKey<Enchantment>... enchantments) {
        ItemStack stack = new ItemStack(item);
        for (ResourceKey<Enchantment> key : enchantments) {
            stack.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key), 1);
        }
        return stack;
    }

    private static Horse stillHorse(GameTestHelper helper, int x, int y, int z) {
        Horse horse = helper.spawn(EntityTypes.HORSE, x, y, z);
        horse.goalSelector.removeAllGoals(goal -> true);
        return horse;
    }

    private static int damageOf(AbstractHorse horse) {
        return HorseshoeAccess.get(horse).getDamageValue();
    }

    /** Brief step 1: a horseshoe on its last point breaks on one durability-consuming event and the slot empties. */
    public static void breaksOnLastPoint(GameTestHelper helper) {
        helper.makeFloor();
        ServerLevel level = helper.getLevel();
        Horse horse = stillHorse(helper, 2, 1, 2);
        ItemStack stack = shoe(helper, ModItems.IRON_HORSESHOE.get(), HorseshoeEnchantments.FEATHER_STEP);
        stack.setDamageValue(stack.getMaxDamage() - 1);
        HorseshoeAccess.set(horse, stack);

        horse.causeFallDamage(20.0, 1.0F, level.damageSources().fall()); // negated by Feather Step -> 1 point
        helper.assertTrue(HorseshoeAccess.get(horse).isEmpty(), "horseshoe on its last point must break and empty the slot");
        helper.succeed();
    }

    /** A non-breaking event adds exactly one point of damage and leaves the horseshoe equipped; harmless falls cost nothing. */
    public static void nonBreakingStepAddsOneDamage(GameTestHelper helper) {
        helper.makeFloor();
        ServerLevel level = helper.getLevel();
        Horse horse = stillHorse(helper, 2, 1, 2);
        ItemStack stack = shoe(helper, ModItems.IRON_HORSESHOE.get(), HorseshoeEnchantments.FEATHER_STEP);
        stack.setDamageValue(5);
        HorseshoeAccess.set(horse, stack);

        horse.causeFallDamage(1.0, 1.0F, level.damageSources().fall()); // would not have hurt: free
        helper.assertValueEqual(damageOf(horse), 5, "damage after a harmless fall");

        horse.causeFallDamage(20.0, 1.0F, level.damageSources().fall());
        ItemStack after = HorseshoeAccess.get(horse);
        helper.assertTrue(after.is(ModItems.IRON_HORSESHOE.get()), "horseshoe must still be equipped, was " + after);
        helper.assertValueEqual(after.getDamageValue(), 6, "damage after one negated fall");
        helper.succeed();
    }

    /** Fire negation fires every tick while exposed; durability is throttled to at most 1 point per 20 ticks. */
    public static void fireNegationIsThrottled(GameTestHelper helper) {
        helper.makeFloor();
        ServerLevel level = helper.getLevel();
        Horse horse = stillHorse(helper, 2, 1, 2);
        HorseshoeAccess.set(horse, shoe(helper, ModItems.NETHERITE_HORSESHOE.get()));
        int ticks = 50;
        int maxAllowed = ticks / HorseshoeDurabilityHandler.THROTTLE_TICKS + 1; // 3

        helper.startSequence()
            .thenExecuteFor(ticks, () -> horse.hurtServer(level, level.damageSources().lava(), 4.0F))
            .thenExecute(() -> {
                int damage = damageOf(horse);
                helper.assertTrue(damage >= 1, "fire negation must consume durability, damage=" + damage);
                helper.assertTrue(damage <= maxAllowed,
                    "fire negation must be throttled: " + ticks + " ticks gave damage " + damage + " > " + maxAllowed);
            })
            .thenSucceed();
    }

    /** Breaking a gold horseshoe removes the gold speed modifier with it. */
    public static void goldBreakRemovesSpeedModifier(GameTestHelper helper) {
        helper.makeFloor();
        Horse horse = stillHorse(helper, 2, 1, 2);
        ItemStack stack = shoe(helper, ModItems.GOLD_HORSESHOE.get());
        stack.setDamageValue(stack.getMaxDamage() - 1);
        HorseshoeAccess.set(horse, stack);
        helper.assertTrue(horse.getAttribute(Attributes.MOVEMENT_SPEED)
            .hasModifier(HorseshoePassiveEffectHandler.GOLD_SPEED_MODIFIER_ID), "gold speed modifier missing before break");

        HorseshoeDurabilityHandler.consume(horse, HorseshoeDurabilityHandler.Trigger.FEATHER_STEP_FALL);
        helper.assertTrue(HorseshoeAccess.get(horse).isEmpty(), "gold horseshoe must break");
        helper.assertTrue(!horse.getAttribute(Attributes.MOVEMENT_SPEED)
            .hasModifier(HorseshoePassiveEffectHandler.GOLD_SPEED_MODIFIER_ID), "gold speed modifier must go with the broken horseshoe");
        helper.succeed();
    }

    /**
     * A ServerPlayer with a real (embedded) connection, placed in the level: {@code ExperienceOrb.playerTouch} only
     * works for ServerPlayers and {@code ServerPlayer.startRiding} needs a connection. Removed again in the same tick.
     */
    @SuppressWarnings("removal")
    private static ServerPlayer serverPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static void removePlayer(GameTestHelper helper, ServerPlayer player) {
        player.stopRiding();
        helper.getLevel().getServer().getPlayerList().remove(player);
    }

    private static void touchOrb(GameTestHelper helper, ServerPlayer player, int value) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        ExperienceOrb orb = new ExperienceOrb(helper.getLevel(), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, value);
        helper.getLevel().addFreshEntity(orb);
        player.takeXpDelay = 0;
        orb.playerTouch(player);
    }

    /** XP picked up by the rider repairs the mount's damaged Mending horseshoe at vanilla's rate, consuming the XP. */
    public static void mendingRepairsFromRiderXp(GameTestHelper helper) {
        helper.makeFloor();
        Horse horse = stillHorse(helper, 2, 1, 2);
        ItemStack stack = shoe(helper, ModItems.IRON_HORSESHOE.get(), Enchantments.MENDING);
        stack.setDamageValue(50);
        HorseshoeAccess.set(horse, stack);
        ServerPlayer rider = serverPlayer(helper);
        try {
            helper.assertTrue(rider.startRiding(horse, true, false), "could not mount horse");
            touchOrb(helper, rider, 10);
            // Vanilla Mending: 2 durability per XP point; 10 XP repairs 20 and is fully consumed.
            helper.assertValueEqual(damageOf(horse), 30, "horseshoe damage after the rider picked up 10 XP");
            helper.assertValueEqual(rider.totalExperience, 0, "rider XP (all spent on repair)");

            // 20 XP: 15 repair the remaining 30 points, the surplus 5 goes to the rider.
            touchOrb(helper, rider, 20);
            helper.assertValueEqual(damageOf(horse), 0, "horseshoe fully repaired");
            helper.assertValueEqual(rider.totalExperience, 5, "rider keeps the XP not needed for the repair");
        } finally {
            removePlayer(helper, rider);
        }
        helper.succeed();
    }

    /** Control: XP picked up by a player not riding does not repair a horse's horseshoe. */
    public static void mendingNeedsRider(GameTestHelper helper) {
        helper.makeFloor();
        Horse horse = stillHorse(helper, 2, 1, 2);
        ItemStack stack = shoe(helper, ModItems.IRON_HORSESHOE.get(), Enchantments.MENDING);
        stack.setDamageValue(50);
        HorseshoeAccess.set(horse, stack);
        ServerPlayer player = serverPlayer(helper);
        try {
            touchOrb(helper, player, 10);
            helper.assertValueEqual(damageOf(horse), 50, "horseshoe damage when nobody rides the horse");
            helper.assertValueEqual(player.totalExperience, 10, "player XP");
        } finally {
            removePlayer(helper, player);
        }
        helper.succeed();
    }

    /** Frost Step walking on land never drains; walking over water drains at most 1 point per 20 ticks. */
    public static void frostStepDrainsOnlyOverWater(GameTestHelper helper) {
        // 11x5x11: land strip x=0..2 (stone), pool x=4..10 with a stone path at z=5 x=6..8 to start on.
        for (int x = 0; x <= 10; x++) {
            for (int z = 0; z <= 10; z++) {
                boolean land = x <= 2;
                boolean rim = x == 3 || z == 0 || x == 10 || z == 10;
                boolean path = z == 5 && x >= 6 && x <= 8;
                Block block = land || rim || path ? Blocks.STONE : Blocks.WATER;
                helper.setBlock(x, 0, z, block);
            }
        }
        Horse onLand = stillHorse(helper, 1, 1, 2);
        HorseshoeAccess.set(onLand, shoe(helper, ModItems.COPPER_HORSESHOE.get(), HorseshoeEnchantments.FROST_STEP));
        Horse onWater = stillHorse(helper, 7, 1, 5);
        HorseshoeAccess.set(onWater, shoe(helper, ModItems.COPPER_HORSESHOE.get(), HorseshoeEnchantments.FROST_STEP));
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        int ticks = 60;
        int[] tick = {0};

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(onLand.onGround() && onWater.onGround(), "horses not on ground yet"))
            .thenExecuteFor(ticks, () -> {
                // Step into a new block every tick: land horse along z on stone, water horse along z over the pool.
                int z = 2 + (tick[0]++ % 6);
                onLand.snapTo(origin.getX() + 1.5, origin.getY() + 1, origin.getZ() + z + 0.5);
                onWater.snapTo(origin.getX() + 7.5, origin.getY() + 1, origin.getZ() + z + 0.5);
            })
            .thenExecute(() -> {
                helper.assertValueEqual(damageOf(onLand), 0, "frost-step horse walking on land must not lose durability");
                int damage = damageOf(onWater);
                int maxAllowed = ticks / HorseshoeDurabilityHandler.THROTTLE_TICKS + 1;
                helper.assertTrue(damage >= 1, "frost-step horse walking over water must lose durability");
                helper.assertTrue(damage <= maxAllowed, "frost step drain must be throttled: damage " + damage + " > " + maxAllowed);
            })
            .thenSucceed();
    }

    /** Lava Walker support drains at most 1 point per 20 ticks (shared with the fire negation it also triggers). */
    public static void lavaWalkerDrainIsThrottled(GameTestHelper helper) {
        for (int x = 0; x <= 10; x++) {
            for (int z = 0; z <= 10; z++) {
                boolean rim = x == 0 || z == 0 || x == 10 || z == 10;
                helper.setBlock(x, 0, z, rim ? Blocks.STONE : Blocks.LAVA);
            }
        }
        Horse walker = stillHorse(helper, 5, 2, 5);
        HorseshoeAccess.set(walker, shoe(helper, ModItems.NETHERITE_HORSESHOE.get(), HorseshoeEnchantments.LAVA_WALKER));
        int ticks = 60;

        helper.startSequence()
            .thenIdle(ticks)
            .thenExecute(() -> {
                int damage = damageOf(walker);
                int maxAllowed = ticks / HorseshoeDurabilityHandler.THROTTLE_TICKS + 2; // + the settling ticks before
                helper.assertTrue(damage >= 1, "lava walker must lose durability while supported");
                helper.assertTrue(damage <= maxAllowed, "lava walker drain must be throttled: damage " + damage + " > " + maxAllowed);
            })
            .thenSucceed();
    }
}
