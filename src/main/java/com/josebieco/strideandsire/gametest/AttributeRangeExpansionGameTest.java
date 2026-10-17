package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.attachment.HorseGeneticsAccess;
import com.josebieco.strideandsire.genetics.GeneticAttribute;
import com.josebieco.strideandsire.genetics.Grade;
import com.josebieco.strideandsire.genetics.HorseAttributeRanges;
import com.josebieco.strideandsire.genetics.HorseAttributes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Test instances: data/strideandsire/test_instance/genetics_attribute_range_*.json
 */
public final class AttributeRangeExpansionGameTest {
    private static final List<EntityType<? extends AbstractHorse>> ELIGIBLE_TYPES = List.of(
        EntityTypes.HORSE, EntityTypes.DONKEY, EntityTypes.MULE, EntityTypes.SKELETON_HORSE, EntityTypes.ZOMBIE_HORSE);

    private AttributeRangeExpansionGameTest() {}

    /**
     * Task 2 (no registry clamp to raise): every eligible horse type holds Speed / Jump / Health base values at the
     * 120% ceiling while living in the level (nothing clamps them back on later ticks), grades them S, and keeps them
     * through an NBT save/load.
     */
    public static void horsesHoldCeilingValuesGradeSAndPersist(GameTestHelper helper) {
        helper.makeFloor();
        ServerLevel level = helper.getLevel();
        List<AbstractHorse> horses = new ArrayList<>();
        for (EntityType<? extends AbstractHorse> type : ELIGIBLE_TYPES) {
            AbstractHorse horse = helper.spawn(type, 2, 1, 2);
            for (GeneticAttribute attr : GeneticAttribute.values()) {
                HorseAttributes.setBase(horse, attr, HorseAttributeRanges.of(attr).ceiling());
            }
            horse.setHealth(horse.getMaxHealth());
            horses.add(horse);
        }
        helper.runAfterDelay(3, () -> {
            for (AbstractHorse horse : horses) {
                assertAtCeiling(helper, horse, horse.getType() + " after 3 ticks in the level");

                TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
                horse.saveWithoutId(output);
                CompoundTag tag = output.buildResult();
                AbstractHorse reloaded = (AbstractHorse) horse.getType().create(level, EntitySpawnReason.LOAD);
                helper.assertTrue(reloaded != null, "could not create reloaded " + horse.getType());
                reloaded.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
                assertAtCeiling(helper, reloaded, horse.getType() + " after save/load");
            }
            helper.succeed();
        });
    }

    private static void assertAtCeiling(GameTestHelper helper, AbstractHorse horse, String context) {
        for (GeneticAttribute attr : GeneticAttribute.values()) {
            HorseAttributeRanges range = HorseAttributeRanges.of(attr);
            double actual = HorseAttributes.get(horse, attr);
            helper.assertTrue(Math.abs(actual - range.ceiling()) < 1e-6,
                context + ": " + attr + " expected " + range.ceiling() + " got " + actual);
            helper.assertValueEqual(range.gradeOf(actual), Grade.S, context + ": " + attr + " at 120% should grade S");
        }
    }

    /**
     * Spec sections 3 and 8: Horses, Donkeys and Mules spawned through the real path (NATURAL creation, finalizeSpawn,
     * added to the level) never exceed vanilla max in any attribute (always E–B) and carry no lineage traits.
     */
    public static void wildSpawnNeverExceedsVanillaMax(GameTestHelper helper) {
        helper.makeFloor();
        ServerLevel level = helper.getLevel();
        BlockPos spawnPos = helper.absolutePos(new BlockPos(2, 1, 2));
        Vec3 spawnVec = helper.absoluteVec(new Vec3(2.5, 1.0, 2.5));
        List<AbstractHorse> spawned = new ArrayList<>();
        for (EntityType<? extends AbstractHorse> type : List.of(EntityTypes.HORSE, EntityTypes.DONKEY, EntityTypes.MULE)) {
            for (int i = 0; i < 20; i++) {
                AbstractHorse wild = type.create(level, EntitySpawnReason.NATURAL);
                helper.assertTrue(wild != null, "could not create wild " + type);
                wild.snapTo(spawnVec.x, spawnVec.y, spawnVec.z, 0.0F, 0.0F);
                wild.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), EntitySpawnReason.NATURAL, null);
                helper.assertTrue(level.addFreshEntity(wild), "wild " + type + " should be added to the level");
                assertWild(helper, wild, "on spawn");
                spawned.add(wild);
            }
        }
        helper.runAfterDelay(1, () -> {
            for (AbstractHorse wild : spawned) {
                assertWild(helper, wild, "after a tick in the level");
                wild.discard();
            }
            helper.succeed();
        });
    }

    private static void assertWild(GameTestHelper helper, AbstractHorse wild, String context) {
        for (GeneticAttribute attr : GeneticAttribute.values()) {
            HorseAttributeRanges range = HorseAttributeRanges.of(attr);
            double val = HorseAttributes.get(wild, attr);
            helper.assertTrue(val <= range.vanillaMax() + 1e-6,
                "wild " + wild.getType() + " " + attr + " exceeded vanilla max " + context + ": " + val + " > " + range.vanillaMax());
            Grade grade = range.gradeOf(val);
            helper.assertTrue(grade != Grade.A && grade != Grade.S,
                "wild " + wild.getType() + " " + attr + " must never grade A or S " + context + ", got " + grade);
        }
        helper.assertTrue(HorseGeneticsAccess.get(wild).isEmpty(),
            "wild " + wild.getType() + " must spawn with all lineage traits ABSENT " + context + ", got " + HorseGeneticsAccess.get(wild));
    }
}
