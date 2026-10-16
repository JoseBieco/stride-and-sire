package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.attachment.HorseshoeAccess;
import com.josebieco.strideandsire.attachment.SaddlebagAccess;
import com.josebieco.strideandsire.event.HorseshoePassiveEffectHandler;
import com.josebieco.strideandsire.item.saddlebag.SaddlebagItem;
import com.josebieco.strideandsire.registry.ModItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Task 6: saddlebag tier bonuses, exercised through the real death/drop path.
 * Test instances: data/strideandsire/test_instance/saddlebag_bonus_*.json
 */
public final class SaddlebagBonusEffectGameTest {
    private SaddlebagBonusEffectGameTest() {}

    // ---- explosion ---------------------------------------------------------------------------------------------

    /** (a) Iron bag with items, horse killed by a real explosion: dropped bag exists, contents intact. */
    public static void ironBagKeepsContentsOnExplosionDeath(GameTestHelper helper) {
        helper.makeFloor();
        Horse horse = bagHorse(helper, 2, ModItems.IRON_SADDLEBAG.get(), true);
        explodeAt(helper, horse.position());
        helper.assertTrue(horse.isDeadOrDying(), "horse should die to the explosion");
        expectSingleDrop(helper, ModItems.IRON_SADDLEBAG.get(), true);
    }

    /** (b) Leather bag with items, horse killed by a real explosion: dropped bag exists, container empty. */
    public static void leatherBagLosesContentsOnExplosionDeath(GameTestHelper helper) {
        helper.makeFloor();
        Horse horse = bagHorse(helper, 2, ModItems.LEATHER_SADDLEBAG.get(), true);
        explodeAt(helper, horse.position());
        helper.assertTrue(horse.isDeadOrDying(), "horse should die to the explosion");
        expectSingleDrop(helper, ModItems.LEATHER_SADDLEBAG.get(), false);
    }

    /** (d) Iron and Leather horses killed by the same single explosion in the same tick: only Iron keeps contents. */
    public static void sameExplosionOnlyIronKeepsContents(GameTestHelper helper) {
        helper.makeFloor();
        Horse iron = bagHorse(helper, 1, ModItems.IRON_SADDLEBAG.get(), true);
        Horse leather = bagHorse(helper, 3, ModItems.LEATHER_SADDLEBAG.get(), true);
        explodeAt(helper, iron.position().add(leather.position()).scale(0.5));
        helper.assertTrue(iron.isDeadOrDying() && leather.isDeadOrDying(), "both horses should die to the one explosion");
        helper.succeedWhen(() -> {
            assertDrop(helper, ModItems.IRON_SADDLEBAG.get(), true);
            assertDrop(helper, ModItems.LEATHER_SADDLEBAG.get(), false);
        });
    }

    /** Spec section 10: a saddled horse without saddlebag killed by an explosion drops exactly as vanilla. */
    public static void noBagExplosionDeathDropsVanilla(GameTestHelper helper) {
        helper.makeFloor();
        Horse horse = helper.spawn(EntityTypes.HORSE, 2, 1, 2);
        horse.setTamed(true);
        horse.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        horse.setGuaranteedDrop(EquipmentSlot.SADDLE); // as when a player saddles it
        horse.setHealth(1.0F);
        explodeAt(helper, horse.position());
        helper.assertTrue(horse.isDeadOrDying(), "horse should die to the explosion");
        helper.succeedWhen(() -> {
            List<ItemEntity> drops = helper.getEntities(EntityTypes.ITEM);
            helper.assertValueEqual(drops.stream().filter(e -> e.getItem().is(Items.SADDLE)).count(), 1L, "saddle drops");
            for (ItemEntity drop : drops) {
                ItemStack s = drop.getItem();
                helper.assertTrue(s.is(Items.SADDLE) || s.is(Items.LEATHER), "unexpected non-vanilla drop " + s);
            }
        });
    }

    // ---- fire ----------------------------------------------------------------------------------------------------

    /** (c) Empty Netherite bag, horse killed by lava: no exception, dropped bag exists and is fire-resistant. */
    public static void emptyNetheriteBagSurvivesFireDeath(GameTestHelper helper) {
        helper.makeFloor();
        Horse horse = bagHorse(helper, 2, ModItems.NETHERITE_SADDLEBAG.get(), false);
        ServerLevel level = helper.getLevel();
        horse.hurtServer(level, level.damageSources().lava(), Float.MAX_VALUE);
        helper.assertTrue(horse.isDeadOrDying(), "horse should die to lava");
        Item bag = ModItems.NETHERITE_SADDLEBAG.get();
        helper.succeedWhen(() -> {
            ItemEntity drop = singleDrop(helper, bag);
            helper.assertTrue(drop.fireImmune(), "netherite saddlebag drop must be fire-immune");
            helper.assertTrue(!drop.hurtServer(level, level.damageSources().lava(), 100.0F) && drop.isAlive(),
                "netherite saddlebag drop must survive lava damage");
            helper.assertTrue(SaddlebagItem.getContents(drop.getItem()).stream().allMatch(ItemStack::isEmpty),
                "empty bag should still be empty");
        });
    }

    /** Netherite bag with items, fire death: contents intact. */
    public static void netheriteBagKeepsContentsOnFireDeath(GameTestHelper helper) {
        helper.makeFloor();
        Horse horse = bagHorse(helper, 2, ModItems.NETHERITE_SADDLEBAG.get(), true);
        killBy(helper, horse, helper.getLevel().damageSources().inFire());
        expectSingleDrop(helper, ModItems.NETHERITE_SADDLEBAG.get(), true);
    }

    /** Iron bag with items, fire death: container cleared (Iron only protects from explosions). */
    public static void ironBagLosesContentsOnFireDeath(GameTestHelper helper) {
        helper.makeFloor();
        Horse horse = bagHorse(helper, 2, ModItems.IRON_SADDLEBAG.get(), true);
        killBy(helper, horse, helper.getLevel().damageSources().lava());
        expectSingleDrop(helper, ModItems.IRON_SADDLEBAG.get(), false);
    }

    /** Review Focus #4: an empty Iron bag killed by an explosion does not throw and still drops. */
    public static void emptyIronBagExplosionDeathDoesNotThrow(GameTestHelper helper) {
        helper.makeFloor();
        Horse horse = bagHorse(helper, 2, ModItems.IRON_SADDLEBAG.get(), false);
        explodeAt(helper, horse.position());
        helper.assertTrue(horse.isDeadOrDying(), "horse should die to the explosion");
        Item bag = ModItems.IRON_SADDLEBAG.get();
        helper.succeedWhen(() -> helper.assertTrue(
            SaddlebagItem.getContents(singleDrop(helper, bag).getItem()).stream().allMatch(ItemStack::isEmpty),
            "empty bag should still be empty"));
    }

    // ---- gold Piglin passive -------------------------------------------------------------------------------------

    /** (e) Gold saddlebag + Iron horseshoe: neutral. Leather saddlebag + Iron horseshoe: not neutral. */
    public static void goldBagMakesPiglinsNeutral(GameTestHelper helper) {
        helper.makeFloor();
        Horse gold = helper.spawn(EntityTypes.HORSE, 1, 1, 2);
        Horse leather = helper.spawn(EntityTypes.HORSE, 3, 1, 2);
        HorseshoeAccess.set(gold, new ItemStack(ModItems.IRON_HORSESHOE.get()));
        HorseshoeAccess.set(leather, new ItemStack(ModItems.IRON_HORSESHOE.get()));
        SaddlebagAccess.set(gold, new ItemStack(ModItems.GOLD_SADDLEBAG.get()));
        SaddlebagAccess.set(leather, new ItemStack(ModItems.LEATHER_SADDLEBAG.get()));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        try {
            helper.assertTrue(player.startRiding(gold, true, false), "could not mount gold-bag horse");
            helper.assertTrue(HorseshoePassiveEffectHandler.mountMakesPiglinsNeutral(player),
                "rider of a gold-saddlebag horse must be neutral even with an iron horseshoe");
            player.stopRiding();
            helper.assertTrue(player.startRiding(leather, true, false), "could not mount leather-bag horse");
            helper.assertTrue(!HorseshoePassiveEffectHandler.mountMakesPiglinsNeutral(player),
                "rider of a leather-saddlebag, iron-shod horse must not be neutral");
        } finally {
            player.stopRiding();
        }
        helper.succeed();
    }

    // ---- helpers -------------------------------------------------------------------------------------------------

    private static Horse bagHorse(GameTestHelper helper, int x, Item bagItem, boolean withItems) {
        Horse horse = helper.spawn(EntityTypes.HORSE, x, 1, 2);
        ItemStack bag = new ItemStack(bagItem);
        if (withItems) {
            SaddlebagItem.setContents(bag, List.of(ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.DIAMOND, 5)));
        }
        SaddlebagAccess.set(horse, bag);
        horse.setHealth(1.0F); // a small explosion is enough; keeps the blast away from neighbouring tests
        return horse;
    }

    /** A small real explosion at absolute {@code pos} that breaks no blocks (damage type {@code minecraft:explosion}). */
    private static void explodeAt(GameTestHelper helper, Vec3 pos) {
        Vec3 abs = pos.add(0.0, 0.5, 0.0);
        helper.getLevel().explode(null, abs.x, abs.y, abs.z, 1.5F, Level.ExplosionInteraction.NONE);
    }

    private static void killBy(GameTestHelper helper, Horse horse, DamageSource source) {
        horse.hurtServer(helper.getLevel(), source, Float.MAX_VALUE);
        helper.assertTrue(horse.isDeadOrDying(), "horse should die to " + source);
    }

    private static void expectSingleDrop(GameTestHelper helper, Item bag, boolean contentsIntact) {
        helper.succeedWhen(() -> assertDrop(helper, bag, contentsIntact));
    }

    private static ItemEntity singleDrop(GameTestHelper helper, Item bag) {
        List<ItemEntity> drops = helper.getEntities(EntityTypes.ITEM).stream().filter(e -> e.getItem().is(bag)).toList();
        helper.assertValueEqual(drops.size(), 1, "dropped " + bag + " count");
        return drops.get(0);
    }

    private static void assertDrop(GameTestHelper helper, Item bag, boolean contentsIntact) {
        ItemEntity drop = singleDrop(helper, bag);
        helper.assertTrue(drop.isAlive(), "dropped " + bag + " must not be destroyed");
        NonNullList<ItemStack> contents = SaddlebagItem.getContents(drop.getItem());
        helper.assertValueEqual(contents.size(), 9, "contents size");
        for (int i = 0; i < contents.size(); i++) {
            ItemStack s = contents.get(i);
            if (i == 2 && contentsIntact) {
                helper.assertTrue(s.is(Items.DIAMOND) && s.getCount() == 5,
                    bag + " slot 2 should still hold 5 diamonds, got " + s);
            } else {
                helper.assertTrue(s.isEmpty(), bag + " slot " + i + " should be empty, got " + s);
            }
        }
    }
}
