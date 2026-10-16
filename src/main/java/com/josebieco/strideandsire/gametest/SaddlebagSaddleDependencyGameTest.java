package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.attachment.SaddlebagAccess;
import com.josebieco.strideandsire.item.saddlebag.SaddlebagItem;
import com.josebieco.strideandsire.menu.StrideAndSireHorseMenu;
import com.josebieco.strideandsire.registry.ModItems;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/**
 * Test instances: data/strideandsire/test_instance/saddlebag_saddle_*.json
 */
public final class SaddlebagSaddleDependencyGameTest {
    private static final int SETTLE_TICKS = 6;

    private SaddlebagSaddleDependencyGameTest() {}

    private static Horse saddledHorse(GameTestHelper helper) {
        helper.makeFloor();
        Horse horse = helper.spawn(EntityTypes.HORSE, 2, 1, 2);
        horse.setTamed(true);
        horse.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        return horse;
    }

    private static ItemStack leatherBagWithThreeItems() {
        ItemStack bag = new ItemStack(ModItems.LEATHER_SADDLEBAG.get());
        SaddlebagItem.setContents(bag, List.of(new ItemStack(Items.DIAMOND, 5), ItemStack.EMPTY, new ItemStack(Items.APPLE, 3),
            ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.STICK, 7)));
        return bag;
    }

    private static List<ItemEntity> bagEntities(GameTestHelper helper) {
        Item bag = ModItems.LEATHER_SADDLEBAG.get();
        return helper.getEntities(EntityTypes.ITEM).stream().filter(e -> e.getItem().is(bag)).toList();
    }

    private static void assertBagDroppedOnceIntact(GameTestHelper helper, Horse horse) {
        helper.assertTrue(SaddlebagAccess.get(horse).isEmpty(), "horse should no longer hold the saddlebag");
        List<ItemEntity> drops = bagEntities(helper);
        helper.assertValueEqual(drops.size(), 1, "saddlebag item entity count");
        NonNullList<ItemStack> contents = SaddlebagItem.getContents(drops.get(0).getItem());
        helper.assertTrue(contents.get(0).is(Items.DIAMOND) && contents.get(0).getCount() == 5, "slot 0: " + contents.get(0));
        helper.assertTrue(contents.get(1).isEmpty(), "slot 1: " + contents.get(1));
        helper.assertTrue(contents.get(2).is(Items.APPLE) && contents.get(2).getCount() == 3, "slot 2: " + contents.get(2));
        helper.assertTrue(contents.get(5).is(Items.STICK) && contents.get(5).getCount() == 7, "slot 5: " + contents.get(5));
    }

    /** Brief (a): no saddlebag equipped - removing the saddle just works and spawns nothing extra. */
    public static void noBagRemovalDoesNothingExtra(GameTestHelper helper) {
        Horse horse = saddledHorse(helper);
        helper.runAfterDelay(2, () -> {
            horse.setItemSlot(EquipmentSlot.SADDLE, ItemStack.EMPTY);
            helper.runAfterDelay(SETTLE_TICKS, () -> {
                helper.assertTrue(horse.getItemBySlot(EquipmentSlot.SADDLE).isEmpty(), "saddle should be removed");
                helper.assertTrue(SaddlebagAccess.get(horse).isEmpty(), "no bag expected");
                helper.assertValueEqual(helper.getEntities(EntityTypes.ITEM).size(), 0, "spawned item entities");
                helper.succeed();
            });
        });
    }

    /** Brief (b): bag with three items is unequipped and dropped once, contents intact; saddle removal is not blocked. */
    public static void removingSaddleDropsBag(GameTestHelper helper) {
        Horse horse = saddledHorse(helper);
        SaddlebagAccess.set(horse, leatherBagWithThreeItems());
        helper.runAfterDelay(2, () -> {
            horse.setItemSlot(EquipmentSlot.SADDLE, ItemStack.EMPTY);
            helper.runAfterDelay(SETTLE_TICKS, () -> {
                helper.assertTrue(horse.getItemBySlot(EquipmentSlot.SADDLE).isEmpty(), "saddle should be removed");
                assertBagDroppedOnceIntact(helper, horse);
                helper.succeed();
            });
        });
    }

    /** Taking the saddle out of the mod's horse menu drops the bag exactly once, contents intact. */
    public static void menuSaddleRemovalDropsBag(GameTestHelper helper) {
        Horse horse = saddledHorse(helper);
        SaddlebagAccess.set(horse, leatherBagWithThreeItems());
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        StrideAndSireHorseMenu menu = new StrideAndSireHorseMenu(1, player.getInventory(), horse);
        helper.runAfterDelay(2, () -> {
            menu.clicked(0, 0, ContainerInput.PICKUP, player);
            helper.assertTrue(menu.getCarried().is(Items.SADDLE), "saddle should be on the cursor, got " + menu.getCarried());
            helper.runAfterDelay(SETTLE_TICKS, () -> {
                helper.assertTrue(horse.getItemBySlot(EquipmentSlot.SADDLE).isEmpty(), "saddle should be removed");
                assertBagDroppedOnceIntact(helper, horse);
                helper.succeed();
            });
        });
    }

    /** Shearing the saddle off (26.x: Equippable can_be_sheared) goes through setItemSlot and drops the bag once. */
    public static void shearsRemovalDropsBag(GameTestHelper helper) {
        Horse horse = saddledHorse(helper);
        SaddlebagAccess.set(horse, leatherBagWithThreeItems());
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        helper.runAfterDelay(2, () -> {
            player.interactOn(horse, InteractionHand.MAIN_HAND, horse.position());
            helper.assertTrue(horse.getItemBySlot(EquipmentSlot.SADDLE).isEmpty(), "shears should have removed the saddle");
            helper.runAfterDelay(SETTLE_TICKS, () -> {
                assertBagDroppedOnceIntact(helper, horse);
                helper.succeed();
            });
        });
    }

    /** A saddled horse with a bag dies: the bag is dropped by the death drop only, exactly once in total. */
    public static void deathDropsBagExactlyOnce(GameTestHelper helper) {
        Horse horse = saddledHorse(helper);
        SaddlebagAccess.set(horse, leatherBagWithThreeItems());
        helper.runAfterDelay(2, () -> {
            horse.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), Float.MAX_VALUE);
            helper.assertTrue(horse.isDeadOrDying(), "horse should be dead");
            helper.runAfterDelay(SETTLE_TICKS, () -> {
                assertBagDroppedOnceIntact(helper, horse);
                helper.succeed();
            });
        });
    }
}
