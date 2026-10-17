package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.attachment.HorseGeneticsAccess;
import com.josebieco.strideandsire.genetics.LineageTrait;
import com.josebieco.strideandsire.genetics.TraitState;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.equine.Horse;

/**
 * Test instance: data/strideandsire/test_instance/genetics_breeding_trait_carrier_vs_absent.json
 */
public final class HorseBreedingTraitGameTest {
    private HorseBreedingTraitGameTest() {}

    /**
     * Task 5 Step 5: breeds a MANIFEST-Fearless parent with an ABSENT-Fearless parent repeatedly and asserts the foal
     * ends up LATENT or MANIFEST noticeably more often than a parallel ABSENT x ABSENT breeding over the same number
     * of trials.
     */
    public static void carrierParentPassesTraitMoreOftenThanAbsentParents(GameTestHelper helper) {
        helper.makeFloor();
        ServerLevel level = helper.getLevel();

        Horse carrierA = helper.spawn(EntityTypes.HORSE, 1, 1, 1);
        Horse carrierB = helper.spawn(EntityTypes.HORSE, 2, 1, 1);
        carrierA.setTamed(true);
        carrierB.setTamed(true);
        HorseGeneticsAccess.set(carrierA, LineageTrait.FEARLESS, TraitState.MANIFEST);

        Horse absentA = helper.spawn(EntityTypes.HORSE, 3, 1, 1);
        Horse absentB = helper.spawn(EntityTypes.HORSE, 3, 1, 2);
        absentA.setTamed(true);
        absentB.setTamed(true);

        int trials = 40;
        int carrierInheritedCount = 0;
        int absentInheritedCount = 0;

        for (int i = 0; i < trials; i++) {
            carrierA.setAge(0);
            carrierB.setAge(0);
            carrierA.setInLove(null);
            carrierB.setInLove(null);
            carrierA.spawnChildFromBreeding(level, carrierB);
            Horse carrierFoal = findNewestBaby(level);
            helper.assertTrue(carrierFoal != null, "carrier breeding trial " + i + " should spawn a foal");
            if (HorseGeneticsAccess.getState(carrierFoal, LineageTrait.FEARLESS).isCarrier()) {
                carrierInheritedCount++;
            }
            carrierFoal.discard();

            absentA.setAge(0);
            absentB.setAge(0);
            absentA.setInLove(null);
            absentB.setInLove(null);
            absentA.spawnChildFromBreeding(level, absentB);
            Horse absentFoal = findNewestBaby(level);
            helper.assertTrue(absentFoal != null, "absent breeding trial " + i + " should spawn a foal");
            if (HorseGeneticsAccess.getState(absentFoal, LineageTrait.FEARLESS).isCarrier()) {
                absentInheritedCount++;
            }
            absentFoal.discard();
        }

        helper.assertTrue(carrierInheritedCount > absentInheritedCount + 5,
            "expected carrier breeding to pass FEARLESS noticeably more often than absent breeding: carrier="
                + carrierInheritedCount + ", absent=" + absentInheritedCount);
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
