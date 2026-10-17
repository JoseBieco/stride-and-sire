package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.attachment.HorseGeneticsAccess;
import com.josebieco.strideandsire.attachment.SaddlebagAccess;
import com.josebieco.strideandsire.genetics.LineageTrait;
import com.josebieco.strideandsire.genetics.TraitState;
import com.josebieco.strideandsire.menu.StrideAndSireHorseMenu;
import com.josebieco.strideandsire.registry.ModItems;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/**
 * Test instance: data/strideandsire/test_instance/genetics_review_focus_edge_cases.json
 */
public final class GeneticsReviewFocusGameTest {
    static final int DUAL_CARRIER_TRIALS = 6000;

    private GeneticsReviewFocusGameTest() {}

    /**
     * Task 11 Step 1 (Review Focus items 3, 4, and 5):
     * <ul>
     *   <li>Item 3: breeds two horses that both carry Sure-footed MANIFEST {@value #DUAL_CARRIER_TRIALS} times and asserts
     *       the carrier rate matches the "at least one parent" table (35% LATENT + 15% MANIFEST = 50% carrier, 50%
     *       ABSENT) rather than an invented higher two-carrier tier. Bounds [0.465, 0.535]: with p = 0.5 the standard
     *       error is 0.00645, so a correct implementation fails with probability 2 * P(Z > 5.42) = 6e-8, while a 55%
     *       tier lands 2.3 standard errors above the upper bound and is rejected ~99% of the time (60%+ always);</li>
     *   <li>Item 4: damages a MANIFEST Fearless horse and asserts its health dropped by the expected amount (not zero);</li>
     *   <li>Item 5: equips a saddlebag on a Draft horse, removes the Draft trait via {@link HorseGeneticsAccess}, and
     *       asserts the menu now exposes only 9 slots with the first 9 items still intact.</li>
     * </ul>
     */
    public static void dualCarrierInheritanceFearlessDamageAndDraftLiveRecompute(GameTestHelper helper) {
        helper.makeFloor();
        ServerLevel level = helper.getLevel();
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);

        // 1. Review Focus item 3: two MANIFEST Sure-footed parents use the "at least one parent" table (50% carrier).
        Horse parentA = helper.spawn(EntityTypes.HORSE, 1, 1, 1);
        Horse parentB = helper.spawn(EntityTypes.HORSE, 2, 1, 1);
        parentA.setTamed(true);
        parentB.setTamed(true);
        HorseGeneticsAccess.set(parentA, LineageTrait.SURE_FOOTED, TraitState.MANIFEST);
        HorseGeneticsAccess.set(parentB, LineageTrait.SURE_FOOTED, TraitState.MANIFEST);

        int trials = DUAL_CARRIER_TRIALS;
        int carrierCount = 0;
        int absentCount = 0;
        for (int i = 0; i < trials; i++) {
            parentA.setAge(0);
            parentB.setAge(0);
            parentA.setInLove(null);
            parentB.setInLove(null);
            parentA.spawnChildFromBreeding(level, parentB);
            Horse foal = findNewestBaby(level);
            helper.assertTrue(foal != null, "dual-carrier breeding trial " + i + " should spawn a foal");
            TraitState state = HorseGeneticsAccess.getState(foal, LineageTrait.SURE_FOOTED);
            if (state.isCarrier()) {
                carrierCount++;
            } else {
                absentCount++;
            }
            foal.discard();
            // Breeding drops XP orbs; discard them so thousands of trials don't pile entities into the level.
            level.getEntitiesOfClass(ExperienceOrb.class, helper.getBounds()).forEach(ExperienceOrb::discard);
        }
        double carrierRate = (double) carrierCount / trials;
        helper.assertTrue(
            absentCount > 0 && carrierRate >= 0.465D && carrierRate <= 0.535D,
            "dual-MANIFEST parents must use the 50% 'at least one parent' table rather than a higher one; got carrierRate="
                + carrierRate + " (carriers=" + carrierCount + "/" + trials + ")"
        );

        // 2. Review Focus item 4: MANIFEST Fearless horse takes zombie damage and loses the expected health (> 0).
        Horse fearlessHorse = helper.spawn(EntityTypes.HORSE, 1, 1, 2);
        fearlessHorse.setTamed(true);
        HorseGeneticsAccess.set(fearlessHorse, LineageTrait.FEARLESS, TraitState.MANIFEST);
        Player rider = helper.makeMockPlayer(GameType.SURVIVAL);
        try {
            rider.startRiding(fearlessHorse, true, false);
            Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 3, 1, 2);
            DamageSource zombieHit = level.damageSources().mobAttack(zombie);

            float healthBefore = fearlessHorse.getHealth();
            float damage = 6.0F;
            fearlessHorse.hurtServer(level, zombieHit, damage);
            float healthDrop = healthBefore - fearlessHorse.getHealth();
            helper.assertTrue(
                Math.abs(healthDrop - damage) < 0.01F,
                "MANIFEST Fearless horse health should drop by expected " + damage + ", dropped by " + healthDrop
            );
            helper.assertTrue(
                rider.getVehicle() == fearlessHorse,
                "MANIFEST Fearless horse should keep rider mounted after taking full damage"
            );
        } finally {
            rider.stopRiding();
        }

        // 3. Review Focus item 5: equip a saddlebag on a Draft horse, populate slots 1–9, remove Draft via
        //    HorseGeneticsAccess, and assert the menu exposes only 9 slots with the first 9 items intact.
        Horse draftHorse = helper.spawn(EntityTypes.HORSE, 2, 1, 2);
        draftHorse.setTamed(true);
        draftHorse.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        HorseGeneticsAccess.set(draftHorse, LineageTrait.DRAFT, TraitState.MANIFEST);
        SaddlebagAccess.set(draftHorse, new ItemStack(ModItems.IRON_SADDLEBAG.get()));

        StrideAndSireHorseMenu draftMenu = new StrideAndSireHorseMenu(10, player.getInventory(), draftHorse);
        helper.assertValueEqual(countActiveSaddlebagSlots(draftMenu), 18, "Draft horse should expose 18 slots initially");
        int draftStart = draftMenu.getSaddlebagContentStart();
        for (int i = 0; i < 9; i++) {
            draftMenu.getSlot(draftStart + i).set(new ItemStack(Items.GOLD_INGOT, i + 1));
        }

        // Remove Draft trait via HorseGeneticsAccess: both the already-open menu's active slot check and a newly opened
        // menu must expose only 9 slots while keeping the first 9 items intact.
        HorseGeneticsAccess.set(draftHorse, LineageTrait.DRAFT, TraitState.ABSENT);
        helper.assertValueEqual(
            countActiveSaddlebagSlots(draftMenu),
            9,
            "open menu should immediately gate active saddlebag slots to 9 when Draft is removed"
        );

        StrideAndSireHorseMenu reopenedMenu = new StrideAndSireHorseMenu(11, player.getInventory(), draftHorse);
        helper.assertValueEqual(
            countActiveSaddlebagSlots(reopenedMenu),
            9,
            "reopened menu after removing Draft should expose only 9 slots"
        );
        int reopenedStart = reopenedMenu.getSaddlebagContentStart();
        for (int i = 0; i < 9; i++) {
            ItemStack item = reopenedMenu.getSlot(reopenedStart + i).getItem();
            helper.assertTrue(
                item.is(Items.GOLD_INGOT) && item.getCount() == i + 1,
                "first 9 items must remain intact after removing Draft; slot " + i + " had " + item
            );
        }
        helper.succeed();
    }

    private static int countActiveSaddlebagSlots(StrideAndSireHorseMenu menu) {
        int count = 0;
        for (Slot slot : menu.slots) {
            if (menu.isSaddlebagContentSlot(slot.index) && slot.isActive()) {
                count++;
            }
        }
        return count;
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
