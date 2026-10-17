package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.attachment.HorseshoeAccess;
import com.josebieco.strideandsire.event.HorseshoePassiveEffectHandler;
import com.josebieco.strideandsire.registry.ModItems;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * Task 6 passive effects. Test instances: data/strideandsire/test_instance/horseshoe_passive_*.json
 */
public final class HorseshoePassiveEffectGameTest {
    private HorseshoePassiveEffectGameTest() {}

    private static AttributeInstance speed(AbstractHorse horse) {
        return horse.getAttribute(Attributes.MOVEMENT_SPEED);
    }

    private static boolean hasGoldSpeed(AbstractHorse horse) {
        return speed(horse).hasModifier(HorseshoePassiveEffectHandler.GOLD_SPEED_MODIFIER_ID);
    }

    private static long goldSpeedCount(AbstractHorse horse) {
        return speed(horse).getModifiers().stream()
            .filter(m -> m.id().equals(HorseshoePassiveEffectHandler.GOLD_SPEED_MODIFIER_ID)).count();
    }

    private static ItemStack shoe(Item item) {
        return new ItemStack(item);
    }

    /** (a) Gold adds the speed modifier exactly once; other tiers and unequipping remove it. */
    public static void goldSpeedModifier(GameTestHelper helper) {
        helper.makeFloor();
        Horse horse = helper.spawn(EntityTypes.HORSE, 2, 1, 2);
        double base = speed(horse).getBaseValue();
        helper.assertTrue(!hasGoldSpeed(horse), "unshod horse must not have the gold speed modifier");

        HorseshoeAccess.set(horse, shoe(ModItems.GOLD_HORSESHOE.get()));
        helper.assertTrue(hasGoldSpeed(horse), "gold horseshoe must add the speed modifier");
        // The menu writes through set twice per slot change: must be idempotent, never stack.
        HorseshoeAccess.set(horse, HorseshoeAccess.get(horse));
        HorseshoeAccess.set(horse, shoe(ModItems.GOLD_HORSESHOE.get()));
        helper.assertValueEqual(goldSpeedCount(horse), 1L, "gold speed modifier count after repeated set");
        double expected = base * (1.0 + HorseshoePassiveEffectHandler.GOLD_SPEED_BONUS);
        helper.assertTrue(Math.abs(speed(horse).getValue() - expected) < 1.0E-9,
            "speed should be " + expected + " but was " + speed(horse).getValue());

        HorseshoeAccess.set(horse, ItemStack.EMPTY);
        helper.assertTrue(!hasGoldSpeed(horse), "unequipping gold must remove the modifier");

        for (Item other : new Item[] {ModItems.COPPER_HORSESHOE.get(), ModItems.IRON_HORSESHOE.get(), ModItems.DIAMOND_HORSESHOE.get(), ModItems.NETHERITE_HORSESHOE.get()}) {
            HorseshoeAccess.set(horse, shoe(ModItems.GOLD_HORSESHOE.get()));
            HorseshoeAccess.set(horse, shoe(other));
            helper.assertTrue(!hasGoldSpeed(horse), "switching gold -> " + other + " must remove the modifier");
        }
        helper.succeed();
    }

    /** (b) Netherite negates every is_fire damage source but not other damage; Iron takes fire damage. */
    public static void netheriteFireImmunity(GameTestHelper helper) {
        helper.makeFloor();
        ServerLevel level = helper.getLevel();
        Horse netherite = helper.spawn(EntityTypes.HORSE, 1, 1, 1);
        Horse iron = helper.spawn(EntityTypes.HORSE, 3, 1, 3);
        HorseshoeAccess.set(netherite, shoe(ModItems.NETHERITE_HORSESHOE.get()));
        HorseshoeAccess.set(iron, shoe(ModItems.IRON_HORSESHOE.get()));

        float start = netherite.getHealth();
        DamageSource[] fire = {level.damageSources().lava(), level.damageSources().inFire(),
            level.damageSources().onFire(), level.damageSources().hotFloor()};
        for (DamageSource source : fire) {
            boolean hurt = netherite.hurtServer(level, source, 4.0F);
            helper.assertTrue(!hurt, "netherite horse was hurt by " + source.getMsgId());
            helper.assertValueEqual(netherite.getHealth(), start, "netherite health after " + source.getMsgId());
        }
        helper.assertTrue(netherite.hurtServer(level, level.damageSources().generic(), 4.0F),
            "netherite horse must still take generic damage");
        helper.assertTrue(netherite.getHealth() < start, "generic damage did not lower netherite horse health");

        float ironStart = iron.getHealth();
        helper.assertTrue(iron.hurtServer(level, level.damageSources().lava(), 4.0F), "iron horse must take lava damage");
        helper.assertTrue(iron.getHealth() < ironStart, "lava did not lower iron horse health");
        helper.succeed();
    }

    /** (c) Two horses side by side: only the gold-shod one carries the modifier. */
    public static void speedModifierIsolation(GameTestHelper helper) {
        helper.makeFloor();
        Horse gold = helper.spawn(EntityTypes.HORSE, 1, 1, 2);
        Horse iron = helper.spawn(EntityTypes.HORSE, 3, 1, 2);
        HorseshoeAccess.set(gold, shoe(ModItems.GOLD_HORSESHOE.get()));
        HorseshoeAccess.set(iron, shoe(ModItems.IRON_HORSESHOE.get()));
        helper.assertTrue(hasGoldSpeed(gold), "gold horse must have the modifier");
        helper.assertTrue(!hasGoldSpeed(iron), "iron horse next to a gold horse must not have the modifier");

        HorseshoeAccess.set(iron, shoe(ModItems.GOLD_HORSESHOE.get()));
        HorseshoeAccess.set(gold, ItemStack.EMPTY);
        helper.assertTrue(!hasGoldSpeed(gold), "unshod horse must lose the modifier");
        helper.assertTrue(hasGoldSpeed(iron), "newly gold-shod horse must have the modifier");
        helper.assertValueEqual(goldSpeedCount(iron), 1L, "modifier count on newly gold-shod horse");
        helper.succeed();
    }

    /** (d) Piglin neutrality predicate: only a player whose vehicle is a gold-shod horse. */
    public static void piglinNeutralWhenRidingGold(GameTestHelper helper) {
        helper.makeFloor();
        Horse gold = helper.spawn(EntityTypes.HORSE, 1, 1, 2);
        Horse iron = helper.spawn(EntityTypes.HORSE, 3, 1, 2);
        HorseshoeAccess.set(gold, shoe(ModItems.GOLD_HORSESHOE.get()));
        HorseshoeAccess.set(iron, shoe(ModItems.IRON_HORSESHOE.get()));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        try {
            helper.assertTrue(!HorseshoePassiveEffectHandler.mountMakesPiglinsNeutral(player), "player on foot must not be neutral");

            helper.assertTrue(player.startRiding(gold, true, false), "could not mount gold horse");
            helper.assertTrue(HorseshoePassiveEffectHandler.mountMakesPiglinsNeutral(player), "rider of gold horse must be neutral");
            player.stopRiding();

            helper.assertTrue(player.startRiding(iron, true, false), "could not mount iron horse");
            helper.assertTrue(!HorseshoePassiveEffectHandler.mountMakesPiglinsNeutral(player), "rider of iron horse must not be neutral");
            player.stopRiding();
        } finally {
            player.stopRiding();
        }
        helper.succeed();
    }

    /** Piglin target hook: suppressed for a gold-mounted rider unless the piglin is angry at that player. */
    public static void piglinTargetSuppressed(GameTestHelper helper) {
        helper.makeFloor();
        Horse gold = helper.spawn(EntityTypes.HORSE, 1, 1, 2);
        HorseshoeAccess.set(gold, shoe(ModItems.GOLD_HORSESHOE.get()));
        Piglin piglin = helper.spawn(EntityTypes.PIGLIN, 3, 1, 3);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        try {
            helper.assertTrue(!HorseshoePassiveEffectHandler.shouldSuppressPiglinTarget(piglin, player),
                "player on foot must stay targetable");
            helper.assertTrue(player.startRiding(gold, true, false), "could not mount gold horse");
            helper.assertTrue(HorseshoePassiveEffectHandler.shouldSuppressPiglinTarget(piglin, player),
                "gold-mounted rider must not be targeted on sight");

            piglin.getBrain().setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, player.getUUID(), 600L);
            helper.assertTrue(!HorseshoePassiveEffectHandler.shouldSuppressPiglinTarget(piglin, player),
                "a piglin angry at the rider must still target them (as with gold armor)");
        } finally {
            player.stopRiding();
        }
        helper.succeed();
    }

    /** Transient modifier is re-applied (once) when a gold-shod horse is loaded back into the level. */
    public static void goldSpeedSurvivesReload(GameTestHelper helper) {
        helper.makeFloor();
        ServerLevel level = helper.getLevel();
        Horse horse = helper.spawn(EntityTypes.HORSE, 2, 1, 2);
        HorseshoeAccess.set(horse, shoe(ModItems.GOLD_HORSESHOE.get()));

        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        horse.saveWithoutId(output);
        CompoundTag tag = output.buildResult();
        horse.discard();

        Horse fresh = EntityTypes.HORSE.create(level, EntitySpawnReason.LOAD);
        helper.assertTrue(fresh != null, "could not create fresh horse");
        fresh.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
        helper.assertTrue(level.addFreshEntity(fresh), "could not add reloaded horse to level");
        helper.addCleanup(passed -> fresh.discard());

        helper.assertTrue(HorseshoeAccess.get(fresh).is(ModItems.GOLD_HORSESHOE.get()), "gold horseshoe lost on reload");
        helper.assertValueEqual(goldSpeedCount(fresh), 1L, "gold speed modifier count after reload");
        helper.succeed();
    }
}
