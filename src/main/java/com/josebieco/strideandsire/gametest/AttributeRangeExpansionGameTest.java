package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.event.AttributeRangeExpansionHandler;
import com.josebieco.strideandsire.genetics.AttributeGrading;
import com.josebieco.strideandsire.genetics.GeneticAttribute;
import com.josebieco.strideandsire.genetics.Grade;
import com.josebieco.strideandsire.genetics.HorseAttributeRanges;
import com.josebieco.strideandsire.genetics.HorseAttributes;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * Test instances: data/strideandsire/test_instance/genetics_attribute_range_*.json
 */
public final class AttributeRangeExpansionGameTest {
    private AttributeRangeExpansionGameTest() {}

    /**
     * Task 2 Step 1: a horse's MOVEMENT_SPEED (and JUMP_STRENGTH / MAX_HEALTH) attribute instance accepts a base value
     * 15% above the pre-mod vanilla max without being clamped back down, grades as S against the original vanilla max,
     * and survives NBT save/load.
     */
    public static void movementSpeedAcceptsFifteenPercentAboveVanillaMax(GameTestHelper helper) {
        helper.makeFloor();
        ServerLevel level = helper.getLevel();
        for (EntityType<? extends AbstractHorse> type : List.of(
                EntityTypes.HORSE, EntityTypes.DONKEY, EntityTypes.MULE, EntityTypes.SKELETON_HORSE, EntityTypes.ZOMBIE_HORSE)) {
            AbstractHorse horse = helper.spawn(type, 2, 1, 2);

            double speedTarget = 1.15 * HorseAttributeRanges.SPEED.vanillaMax();
            AttributeInstance speedInst = horse.getAttribute(Attributes.MOVEMENT_SPEED);
            helper.assertTrue(speedInst != null, type + " missing MOVEMENT_SPEED attribute");
            speedInst.setBaseValue(speedTarget);
            AttributeRangeExpansionHandler.clampToExpandedCeiling(horse);

            helper.assertTrue(Math.abs(speedInst.getBaseValue() - speedTarget) < 1e-6,
                type + " MOVEMENT_SPEED base clamped: expected " + speedTarget + " got " + speedInst.getBaseValue());
            helper.assertTrue(Math.abs(speedInst.getValue() - speedTarget) < 1e-6,
                type + " MOVEMENT_SPEED value clamped: expected " + speedTarget + " got " + speedInst.getValue());
            Grade speedGrade = AttributeGrading.gradeOf(
                speedInst.getBaseValue(),
                HorseAttributeRanges.SPEED.vanillaMin(),
                HorseAttributeRanges.SPEED.vanillaMax());
            helper.assertValueEqual(speedGrade, Grade.S, type + " speed at 115% of vanilla max should grade S");

            for (GeneticAttribute attr : GeneticAttribute.values()) {
                HorseAttributeRanges range = HorseAttributeRanges.of(attr);
                double target = 1.15 * range.vanillaMax();
                HorseAttributes.setBase(horse, attr, target);
                AttributeRangeExpansionHandler.clampToExpandedCeiling(horse);
                double actual = HorseAttributes.get(horse, attr);
                helper.assertTrue(Math.abs(actual - target) < 1e-6,
                    type + " " + attr + " base mismatch: expected " + target + " got " + actual);
                helper.assertValueEqual(range.gradeOf(actual), Grade.S, type + " " + attr + " at 115% should grade S");
            }
        }

        // Verify persistence across NBT save and load on a Horse.
        Horse horse = helper.spawn(EntityTypes.HORSE, 2, 1, 2);
        double speed115 = 1.15 * HorseAttributeRanges.SPEED.vanillaMax();
        horse.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed115);
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        horse.saveWithoutId(output);
        CompoundTag tag = output.buildResult();

        Horse reloaded = EntityTypes.HORSE.create(level, EntitySpawnReason.LOAD);
        helper.assertTrue(reloaded != null, "could not create reloaded horse");
        reloaded.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
        AttributeRangeExpansionHandler.clampToExpandedCeiling(reloaded);
        double loadedSpeed = reloaded.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue();
        helper.assertTrue(Math.abs(loadedSpeed - speed115) < 1e-6,
            "reloaded horse MOVEMENT_SPEED clamped: expected " + speed115 + " got " + loadedSpeed);

        helper.succeed();
    }

    /**
     * Spec section 3 & 8: naturally spawned wild horses never exceed 100% of vanilla max in any attribute (always E–B).
     */
    public static void wildSpawnNeverExceedsVanillaMax(GameTestHelper helper) {
        helper.makeFloor();
        ServerLevel level = helper.getLevel();
        for (int i = 0; i < 20; i++) {
            Horse wild = EntityTypes.HORSE.create(level, EntitySpawnReason.NATURAL);
            helper.assertTrue(wild != null, "could not create wild horse");
            wild.finalizeSpawn(level, level.getCurrentDifficultyAt(helper.absolutePos(new net.minecraft.core.BlockPos(2, 1, 2))),
                EntitySpawnReason.NATURAL, null);
            for (GeneticAttribute attr : GeneticAttribute.values()) {
                HorseAttributeRanges range = HorseAttributeRanges.of(attr);
                double val = HorseAttributes.get(wild, attr);
                helper.assertTrue(val <= range.vanillaMax() + 1e-6,
                    "wild horse " + attr + " exceeded vanilla max: " + val + " > " + range.vanillaMax());
                Grade grade = range.gradeOf(val);
                helper.assertTrue(grade != Grade.A && grade != Grade.S,
                    "wild horse " + attr + " must never grade A or S, got " + grade);
            }
        }
        helper.succeed();
    }
}
