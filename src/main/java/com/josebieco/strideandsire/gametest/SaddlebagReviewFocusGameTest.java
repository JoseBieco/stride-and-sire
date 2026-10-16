package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.attachment.SaddlebagAccess;
import com.josebieco.strideandsire.item.saddlebag.SaddlebagItem;
import com.josebieco.strideandsire.menu.StrideAndSireHorseMenu;
import com.josebieco.strideandsire.registry.ModItems;
import java.util.List;
import java.util.Set;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.Donkey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractMountInventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

/**
 * Task 9: end-to-end Review Focus items 2 and 5. Test instance:
 * data/strideandsire/test_instance/saddlebag_review_focus_chest_bag_isolation_and_explosion.json
 */
public final class SaddlebagReviewFocusGameTest {
    private SaddlebagReviewFocusGameTest() {}

    private static final Set<Item> CHEST_ITEMS = Set.of(Items.COBBLESTONE, Items.COAL, Items.APPLE);
    private static final Set<Item> BAG_ITEMS = Set.of(Items.EMERALD, Items.DIAMOND, Items.GOLD_NUGGET);

    public static void chestBagIsolationAndSameTickExplosion(GameTestHelper helper) {
        helper.makeFloor();
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        var inventory = player.getInventory();
        Donkey gold = helper.spawn(EntityTypes.DONKEY, 1, 1, 2);
        gold.setTamed(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CHEST));
        gold.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(gold.hasChest(), "donkey should have a chest");
        gold.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        SaddlebagAccess.set(gold, bagWithItems(ModItems.GOLD_SADDLEBAG.get()));
        inventory.clearContent();

        StrideAndSireHorseMenu menu = new StrideAndSireHorseMenu(1, inventory, gold);
        Container chest = StrideAndSireHorseMenu.horseInventory(gold);
        int chestSize = AbstractMountInventoryMenu.getInventorySize(gold.getInventoryColumns());
        int chestStart = 2;
        int bagStart = menu.getSaddlebagContentStart();
        int firstMainSlot = 2 + chestSize;
        chest.setItem(0, new ItemStack(Items.COBBLESTONE, 10));
        chest.setItem(1, new ItemStack(Items.COAL, 4));
        assertIsolated(helper, menu, chest, gold, "initial");

        // chest -> player
        menu.quickMoveStack(player, chestStart);
        assertIsolated(helper, menu, chest, gold, "after chest->player");
        helper.assertValueEqual(inventory.countItem(Items.COBBLESTONE), 10, "chest item should reach the player");
        // bag -> player
        menu.quickMoveStack(player, bagStart);
        assertIsolated(helper, menu, chest, gold, "after bag->player");
        helper.assertValueEqual(inventory.countItem(Items.DIAMOND), 3, "bag item should reach the player");
        // player -> horse storage: chest first while it has room
        inventory.clearContent();
        inventory.setItem(9, new ItemStack(Items.APPLE, 5));
        menu.quickMoveStack(player, firstMainSlot);
        assertIsolated(helper, menu, chest, gold, "after player->chest");
        helper.assertValueEqual(chest.countItem(Items.APPLE), 5, "apple should land in the chest");
        // chest full: next player item may only go to the bag
        for (int i = 0; i < chestSize; i++) {
            if (chest.getItem(i).isEmpty()) {
                chest.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
            }
        }
        inventory.setItem(9, new ItemStack(Items.GOLD_NUGGET, 6));
        menu.quickMoveStack(player, firstMainSlot);
        assertIsolated(helper, menu, chest, gold, "after player->bag");
        helper.assertValueEqual(chest.countItem(Items.GOLD_NUGGET), 0, "player item leaked into the full chest");
        helper.assertValueEqual(count(SaddlebagItem.getContents(SaddlebagAccess.get(gold)), Items.GOLD_NUGGET), 6,
            "player item should overflow into the bag");

        // Restore known bag contents for the explosion phase.
        SaddlebagAccess.set(gold, bagWithItems(ModItems.GOLD_SADDLEBAG.get()));

        Donkey leather = helper.spawn(EntityTypes.DONKEY, 3, 1, 2);
        leather.setTamed(true);
        leather.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        SaddlebagAccess.set(leather, bagWithItems(ModItems.LEATHER_SADDLEBAG.get()));
        gold.setHealth(1.0F);
        leather.setHealth(1.0F);

        var mid = gold.position().add(leather.position()).scale(0.5);
        helper.getLevel().explode(null, mid.x, mid.y + 0.5, mid.z, 1.5F, Level.ExplosionInteraction.NONE);
        helper.assertTrue(gold.isDeadOrDying() && leather.isDeadOrDying(), "both donkeys should die to the one explosion");
        helper.succeedWhen(() -> {
            assertBagDrop(helper, ModItems.GOLD_SADDLEBAG.get(), true);
            assertBagDrop(helper, ModItems.LEATHER_SADDLEBAG.get(), false);
        });
    }

    private static ItemStack bagWithItems(Item bagItem) {
        ItemStack bag = new ItemStack(bagItem);
        SaddlebagItem.setContents(bag, List.of(new ItemStack(Items.DIAMOND, 3), ItemStack.EMPTY, ItemStack.EMPTY,
            new ItemStack(Items.EMERALD, 7)));
        return bag;
    }

    private static int count(List<ItemStack> stacks, Item item) {
        return stacks.stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    /** Chest holds only chest-origin items, bag only bag-origin items, in the live containers and the menu slots. */
    private static void assertIsolated(GameTestHelper helper, StrideAndSireHorseMenu menu, Container chest, Donkey donkey,
                                       String step) {
        for (int i = 0; i < chest.getContainerSize(); i++) {
            ItemStack s = chest.getItem(i);
            helper.assertTrue(s.isEmpty() || CHEST_ITEMS.contains(s.getItem()), step + ": bag item in chest slot " + i + ": " + s);
            ItemStack shown = menu.getSlot(2 + i).getItem();
            helper.assertTrue(shown.isEmpty() || CHEST_ITEMS.contains(shown.getItem()), step + ": bag item in chest menu slot " + i);
        }
        NonNullList<ItemStack> contents = SaddlebagItem.getContents(SaddlebagAccess.get(donkey));
        for (int i = 0; i < contents.size(); i++) {
            ItemStack s = contents.get(i);
            helper.assertTrue(s.isEmpty() || BAG_ITEMS.contains(s.getItem()), step + ": chest item in bag slot " + i + ": " + s);
            ItemStack shown = menu.getSlot(menu.getSaddlebagContentStart() + i).getItem();
            helper.assertTrue(shown.isEmpty() || BAG_ITEMS.contains(shown.getItem()), step + ": chest item in bag menu slot " + i);
        }
    }

    private static void assertBagDrop(GameTestHelper helper, Item bag, boolean contentsIntact) {
        List<ItemEntity> drops = helper.getEntities(EntityTypes.ITEM).stream().filter(e -> e.getItem().is(bag)).toList();
        helper.assertValueEqual(drops.size(), 1, "dropped " + bag + " count");
        NonNullList<ItemStack> contents = SaddlebagItem.getContents(drops.get(0).getItem());
        if (contentsIntact) {
            helper.assertTrue(contents.get(0).is(Items.DIAMOND) && contents.get(0).getCount() == 3
                && contents.get(3).is(Items.EMERALD) && contents.get(3).getCount() == 7, bag + " should keep its contents: " + contents);
        } else {
            helper.assertTrue(contents.stream().allMatch(ItemStack::isEmpty), bag + " should be emptied: " + contents);
        }
    }
}
