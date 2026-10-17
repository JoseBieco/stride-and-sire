package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.genetics.GeneticAttribute;
import com.josebieco.strideandsire.genetics.Grade;
import com.josebieco.strideandsire.genetics.HorseAttributeRanges;
import com.josebieco.strideandsire.genetics.HorseAttributes;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/**
 * Test instance: data/strideandsire/test_instance/genetics_breeding_attribute_diet_vs_unguided.json
 */
public final class HorseBreedingAttributeGameTest {
    private HorseBreedingAttributeGameTest() {}

    /**
     * Task 4 Step 5: breeds two horses at vanilla-max Speed with a Golden Carrot repeatedly and asserts at least one
     * resulting foal's Speed grades A or S, while a parallel unguided breeding of the same two parents never produces
     * Grade A or S across the same number of trials.
     */
    public static void dietGuidedProducesGradeAOrSWhileUnguidedNeverDoes(GameTestHelper helper) {
        helper.makeFloor();
        ServerLevel level = helper.getLevel();
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);

        Horse parentA = helper.spawn(EntityTypes.HORSE, 1, 1, 1);
        Horse parentB = helper.spawn(EntityTypes.HORSE, 2, 1, 1);
        parentA.setTamed(true);
        parentB.setTamed(true);

        for (GeneticAttribute attr : GeneticAttribute.values()) {
            double max = HorseAttributeRanges.of(attr).vanillaMax();
            HorseAttributes.setBase(parentA, attr, max);
            HorseAttributes.setBase(parentB, attr, max);
        }
        parentA.setHealth(parentA.getMaxHealth());
        parentB.setHealth(parentB.getMaxHealth());

        int trials = 20;
        int guidedAOrSCount = 0;

        for (int i = 0; i < trials; i++) {
            parentA.setAge(0);
            parentB.setAge(0);
            parentA.resetLove();
            parentB.resetLove();

            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GOLDEN_CARROT, 64));
            player.interactOn(parentA, InteractionHand.MAIN_HAND, parentA.position());
            player.interactOn(parentB, InteractionHand.MAIN_HAND, parentB.position());
            helper.assertTrue(parentA.isInLove() && parentB.isInLove(), "Golden Carrot should put both horses in love");

            parentA.spawnChildFromBreeding(level, parentB);
            Horse guidedFoal = findNewestBaby(level);
            helper.assertTrue(guidedFoal != null, "guided breeding trial " + i + " should spawn a foal");

            double foalSpeed = HorseAttributes.get(guidedFoal, GeneticAttribute.SPEED);
            Grade speedGrade = HorseAttributeRanges.SPEED.gradeOf(foalSpeed);
            if (speedGrade == Grade.A || speedGrade == Grade.S) {
                guidedAOrSCount++;
            }
            guidedFoal.discard();

            // Parallel unguided breeding of the same two parents (setInLove directly without a diet item).
            parentA.setAge(0);
            parentB.setAge(0);
            parentA.setInLove(null);
            parentB.setInLove(null);
            parentA.spawnChildFromBreeding(level, parentB);
            Horse unguidedFoal = findNewestBaby(level);
            helper.assertTrue(unguidedFoal != null, "unguided breeding trial " + i + " should spawn a foal");

            for (GeneticAttribute attr : GeneticAttribute.values()) {
                HorseAttributeRanges range = HorseAttributeRanges.of(attr);
                double val = HorseAttributes.get(unguidedFoal, attr);
                Grade grade = range.gradeOf(val);
                helper.assertTrue(grade != Grade.A && grade != Grade.S,
                    "unguided foal " + attr + " graded " + grade + " (value=" + val + ") on trial " + i);
            }
            unguidedFoal.discard();
        }

        helper.assertTrue(guidedAOrSCount > 0,
            "expected at least one Golden Carrot foal to reach Grade A or S Speed across " + trials + " trials");
        helper.succeed();
    }

    private static Horse findNewestBaby(ServerLevel level) {
        Horse found = null;
        for (var entity : level.getAllEntities()) {
            if (entity instanceof Horse horse && horse.isBaby() && horse.isAlive()) {
                found = horse;
            }
        }
        return found;
    }
}
