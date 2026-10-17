package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.attachment.HorseGeneticsAccess;
import com.josebieco.strideandsire.genetics.LineageTrait;
import com.josebieco.strideandsire.genetics.TraitState;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

/**
 * Test instance: data/strideandsire/test_instance/genetics_fearless_panic_suppression.json
 */
public final class FearlessPanicSuppressionGameTest {
    /**
     * Vanilla 26.3 {@code AbstractHorse.hurtServer} rears with probability 1/3 per hit. Over {@value} hits a horse with
     * vanilla behaviour fails to rear with probability (2/3)^40 = 9.0e-8, which bounds both the false-failure rate of
     * (b) and the chance that (a) passes without the feature.
     */
    static final int HITS = 40;

    private FearlessPanicSuppressionGameTest() {}

    /**
     * Task 9 / spec section 8, one zombie hit every 2 ticks (damage cooldown and health reset before each hit):
     * (a) a ridden MANIFEST Fearless horse takes the damage every time, keeps its rider and is never standing on the
     * following tick;
     * (b) a ridden LATENT Fearless horse keeps vanilla behaviour: it takes the damage, rears at least once, and keeps
     * its rider (vanilla never ejects on damage).
     */
    public static void fearlessSuppressesRearingWhileLatentKeepsVanillaRearing(GameTestHelper helper) {
        helper.makeFloor();
        ServerLevel level = helper.getLevel();
        Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 3, 1, 2);
        DamageSource zombieAttack = level.damageSources().mobAttack(zombie);

        Horse fearless = helper.spawn(EntityTypes.HORSE, 1, 1, 1);
        fearless.setTamed(true);
        HorseGeneticsAccess.set(fearless, LineageTrait.FEARLESS, TraitState.MANIFEST);
        Horse latent = helper.spawn(EntityTypes.HORSE, 1, 1, 3);
        latent.setTamed(true);
        HorseGeneticsAccess.set(latent, LineageTrait.FEARLESS, TraitState.LATENT);

        Player riderA = helper.makeMockPlayer(GameType.SURVIVAL);
        Player riderB = helper.makeMockPlayer(GameType.SURVIVAL);
        riderA.startRiding(fearless, true, false);
        riderB.startRiding(latent, true, false);
        helper.assertTrue(riderA.getVehicle() == fearless && riderB.getVehicle() == latent, "riders should be mounted");

        int[] latentRears = {0};
        for (int i = 0; i < HITS; i++) {
            int hit = i;
            helper.runAfterDelay(1 + 2L * i, () -> {
                hitOnce(helper, level, fearless, zombieAttack, "MANIFEST", hit);
                latent.clearStanding();
                hitOnce(helper, level, latent, zombieAttack, "LATENT", hit);
                if (latent.isStanding()) {
                    latentRears[0]++;
                }
                helper.assertTrue(riderA.getVehicle() == fearless && fearless.hasPassenger(riderA),
                    "MANIFEST Fearless horse must keep its rider (hit " + hit + ")");
                helper.assertTrue(riderB.getVehicle() == latent && latent.hasPassenger(riderB),
                    "LATENT horse keeps vanilla behaviour: never ejects its rider on damage (hit " + hit + ")");
            });
            helper.runAfterDelay(2 + 2L * i, () -> helper.assertTrue(!fearless.isStanding(),
                "MANIFEST Fearless horse must not rear on zombie damage (after hit " + hit + ")"));
        }
        helper.runAfterDelay(2L * HITS + 2, () -> {
            helper.assertTrue(latentRears[0] > 0,
                "LATENT Fearless horse should still rear (vanilla 1/3 per hit) at least once in " + HITS + " hits");
            riderA.stopRiding();
            riderB.stopRiding();
            helper.succeed();
        });
    }

    private static void hitOnce(GameTestHelper helper, ServerLevel level, Horse horse, DamageSource source, String label, int hit) {
        horse.setHealth(horse.getMaxHealth());
        horse.damageCooldownTime = 0; // 26.3's post-hit invulnerability window (LivingEntity.hurtServer)
        float before = horse.getHealth();
        boolean hurt = horse.hurtServer(level, source, 2.0F);
        helper.assertTrue(hurt && horse.getHealth() < before,
            label + " horse must take the zombie damage (hit " + hit + ", before=" + before + ", after=" + horse.getHealth() + ")");
    }
}
