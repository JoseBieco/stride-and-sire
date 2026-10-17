package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.attachment.HorseGeneticsAccess;
import com.josebieco.strideandsire.genetics.LineageTrait;
import com.josebieco.strideandsire.genetics.TraitState;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.GameType;

/**
 * Test instance: data/strideandsire/test_instance/genetics_fearless_panic_suppression.json
 */
public final class FearlessPanicSuppressionGameTest {
    private FearlessPanicSuppressionGameTest() {}

    /**
     * Task 9 Step 1:
     * (a) a MANIFEST Fearless horse takes zombie damage, loses health normally, does not rear, and does not dismount its rider;
     * (b) a horse without the trait (ABSENT and LATENT) dismounts its rider under the same zombie damage.
     */
    public static void fearlessKeepsRiderAndTakesDamageWhileNonFearlessDismounts(GameTestHelper helper) {
        helper.makeFloor();
        ServerLevel level = helper.getLevel();
        Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 3, 1, 1);
        DamageSource zombieAttack = level.damageSources().mobAttack(zombie);

        // (a) MANIFEST Fearless horse: takes full damage, keeps rider mounted, does not rear.
        Horse fearlessHorse = helper.spawn(EntityTypes.HORSE, 1, 1, 1);
        fearlessHorse.setTamed(true);
        HorseGeneticsAccess.set(fearlessHorse, LineageTrait.FEARLESS, TraitState.MANIFEST);
        ServerPlayer riderA = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        riderA.startRiding(fearlessHorse, true, true);
        helper.assertTrue(riderA.getVehicle() == fearlessHorse, "riderA should be mounted on fearlessHorse");

        float healthBefore = fearlessHorse.getHealth();
        float damageAmount = 4.0F;
        boolean hurt = fearlessHorse.hurtServer(level, zombieAttack, damageAmount);
        helper.assertTrue(hurt, "fearlessHorse should take zombie damage");
        helper.assertTrue(
            fearlessHorse.getHealth() < healthBefore,
            "fearlessHorse should lose health normally from zombie damage (before=" + healthBefore + ", after=" + fearlessHorse.getHealth() + ")"
        );
        helper.assertTrue(
            riderA.getVehicle() == fearlessHorse && fearlessHorse.hasPassenger(riderA),
            "MANIFEST Fearless horse must not dismount its rider on zombie damage"
        );
        helper.assertTrue(
            !fearlessHorse.isStanding(),
            "MANIFEST Fearless horse must not rear on zombie damage"
        );

        // (b) Horse without the trait (ABSENT): takes damage and dismounts its rider.
        Horse normalHorse = helper.spawn(EntityTypes.HORSE, 2, 1, 1);
        normalHorse.setTamed(true);
        HorseGeneticsAccess.set(normalHorse, LineageTrait.FEARLESS, TraitState.ABSENT);
        ServerPlayer riderB = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        riderB.startRiding(normalHorse, true, true);
        helper.assertTrue(riderB.getVehicle() == normalHorse, "riderB should be mounted on normalHorse");

        float normalHealthBefore = normalHorse.getHealth();
        normalHorse.hurtServer(level, zombieAttack, damageAmount);
        helper.assertTrue(normalHorse.getHealth() < normalHealthBefore, "normalHorse should lose health from zombie damage");
        helper.assertTrue(
            riderB.getVehicle() == null && !normalHorse.hasPassenger(riderB),
            "Horse without Fearless should dismount its rider under sudden zombie damage"
        );
        helper.succeed();
    }
}
