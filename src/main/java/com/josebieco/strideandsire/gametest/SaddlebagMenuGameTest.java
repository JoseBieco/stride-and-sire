package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.attachment.HorseEquipmentSlot;
import com.josebieco.strideandsire.attachment.HorseshoeAccess;
import com.josebieco.strideandsire.attachment.SaddlebagAccess;
import com.josebieco.strideandsire.item.saddlebag.SaddlebagItem;
import com.josebieco.strideandsire.menu.SaddlebagSlot;
import com.josebieco.strideandsire.menu.StrideAndSireHorseMenu;
import com.josebieco.strideandsire.network.HorseEquipmentSyncPacket;
import com.josebieco.strideandsire.registry.ModItems;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Donkey;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractMountInventoryMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/**
 * Test instances: data/strideandsire/test_instance/saddlebag_menu_*.json
 */
public final class SaddlebagMenuGameTest {
    private SaddlebagMenuGameTest() {}

    private static Horse tamedHorse(GameTestHelper helper) {
        helper.makeFloor();
        Horse horse = helper.spawn(EntityTypes.HORSE, 2, 1, 2);
        horse.setTamed(true);
        return horse;
    }

    private static void saddle(AbstractHorse horse) {
        horse.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
    }

    private static ItemStack bagWith(Item bagItem, List<ItemStack> contents) {
        ItemStack bag = new ItemStack(bagItem);
        SaddlebagItem.setContents(bag, contents);
        return bag;
    }

    private static int count(List<ItemStack> stacks, Item item) {
        return stacks.stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    private static NonNullList<ItemStack> bagContents(AbstractHorse horse) {
        return SaddlebagItem.getContents(SaddlebagAccess.get(horse));
    }

    /** Brief (a) + Review Focus 1: the slot needs a saddled horse; a bag clicked onto a saddle-less horse stays carried. */
    public static void slotRequiresSaddle(GameTestHelper helper) {
        Horse horse = tamedHorse(helper);
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        StrideAndSireHorseMenu menu = new StrideAndSireHorseMenu(1, player.getInventory(), horse);
        SaddlebagSlot bagSlot = menu.getSaddlebagSlot();
        ItemStack gold = new ItemStack(ModItems.GOLD_SADDLEBAG.get());

        helper.assertTrue(!bagSlot.mayPlace(gold), "saddlebag slot must reject a bag on a saddle-less horse");
        menu.setCarried(gold.copy());
        menu.clicked(menu.getSaddlebagSlotIndex(), 0, ContainerInput.PICKUP, player);
        helper.assertTrue(SaddlebagAccess.get(horse).isEmpty(), "bag accepted on a saddle-less horse");
        helper.assertTrue(menu.getCarried().is(ModItems.GOLD_SADDLEBAG.get()) && menu.getCarried().getCount() == 1,
            "rejected bag should stay carried, exactly once");
        menu.setCarried(ItemStack.EMPTY);

        saddle(horse);
        helper.assertTrue(bagSlot.mayPlace(gold), "saddlebag slot must accept a bag once saddled");
        helper.assertTrue(!bagSlot.mayPlace(new ItemStack(Items.DIAMOND)), "saddlebag slot must reject a diamond");
        helper.assertTrue(!bagSlot.mayPlace(new ItemStack(ModItems.GOLD_HORSESHOE.get())), "saddlebag slot must reject a horseshoe");
        helper.assertValueEqual(bagSlot.getMaxStackSize(), 1, "saddlebag slot max stack size");

        menu.setCarried(gold.copy());
        menu.clicked(menu.getSaddlebagSlotIndex(), 0, ContainerInput.PICKUP, player);
        helper.assertTrue(SaddlebagAccess.get(horse).is(ModItems.GOLD_SADDLEBAG.get()), "clicked bag should be equipped");
        helper.assertTrue(menu.getCarried().isEmpty(), "placed bag should leave the cursor");
        helper.assertTrue(HorseshoeAccess.get(horse).isEmpty(), "saddlebag must not touch the horseshoe slot");
        helper.succeed();
    }

    /**
     * Brief (b) + Review Focus 2: a Donkey with its native chest and a Gold saddlebag exposes chest (vanilla size) + 9
     * independent container slots, and quick-move never crosses between them, in either direction, even when the player
     * inventory is full.
     */
    public static void donkeyChestAndBagAreIsolated(GameTestHelper helper) {
        helper.makeFloor();
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        Inventory inventory = player.getInventory();
        Donkey donkey = helper.spawn(EntityTypes.DONKEY, 2, 1, 2);
        donkey.setTamed(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CHEST));
        donkey.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(donkey.hasChest(), "donkey should have a chest");
        saddle(donkey);
        SaddlebagAccess.set(donkey, new ItemStack(ModItems.GOLD_SADDLEBAG.get()));
        inventory.clearContent();

        StrideAndSireHorseMenu menu = new StrideAndSireHorseMenu(1, inventory, donkey);
        Container chest = StrideAndSireHorseMenu.horseInventory(donkey);
        int chestSize = AbstractMountInventoryMenu.getInventorySize(donkey.getInventoryColumns());
        helper.assertValueEqual(chestSize, 15, "vanilla donkey chest size");
        int chestSlots = 0;
        int bagSlots = 0;
        for (Slot slot : menu.slots) {
            if (slot.container == chest) {
                chestSlots++;
            }
            if (menu.isSaddlebagContentSlot(slot.index) && slot.isActive()) {
                bagSlots++;
            }
        }
        helper.assertValueEqual(chestSlots, chestSize, "addressable chest slots");
        helper.assertValueEqual(bagSlots, StrideAndSireHorseMenu.SADDLEBAG_CONTENT_SLOTS, "addressable saddlebag slots");
        helper.assertValueEqual(chestSlots + bagSlots, chestSize + 9, "total addressable container slots");

        int chestStart = 2;
        int bagStart = menu.getSaddlebagContentStart();
        int firstMainSlot = 2 + chestSize; // player inventory slot 9

        // chest -> player (never the bag)
        menu.getSlot(chestStart).set(new ItemStack(Items.COBBLESTONE, 10));
        menu.quickMoveStack(player, chestStart);
        helper.assertValueEqual(count(bagContents(donkey), Items.COBBLESTONE), 0, "chest item leaked into the bag");
        helper.assertValueEqual(inventory.countItem(Items.COBBLESTONE), 10, "chest item should reach the player");
        inventory.clearContent();

        // bag -> player (never the chest)
        menu.getSlot(bagStart).set(new ItemStack(Items.EMERALD, 7));
        helper.assertValueEqual(count(bagContents(donkey), Items.EMERALD), 7, "sub-slot write should reach the equipped bag");
        menu.quickMoveStack(player, bagStart);
        helper.assertValueEqual(chest.countItem(Items.EMERALD), 0, "bag item leaked into the chest");
        helper.assertValueEqual(count(bagContents(donkey), Items.EMERALD), 0, "bag item should have left the bag");
        helper.assertValueEqual(inventory.countItem(Items.EMERALD), 7, "bag item should reach the player");
        inventory.clearContent();

        // With the player inventory full, neither side may fall back into the other.
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, new ItemStack(Items.DIRT, 64));
        }
        menu.getSlot(chestStart).set(new ItemStack(Items.COBBLESTONE, 10));
        menu.getSlot(bagStart).set(new ItemStack(Items.EMERALD, 7));
        menu.quickMoveStack(player, chestStart);
        menu.quickMoveStack(player, bagStart);
        helper.assertValueEqual(chest.countItem(Items.COBBLESTONE), 10, "chest item should stay in the chest");
        helper.assertValueEqual(count(bagContents(donkey), Items.COBBLESTONE), 0, "full inventory: chest item leaked into the bag");
        helper.assertValueEqual(count(bagContents(donkey), Items.EMERALD), 7, "bag item should stay in the bag");
        helper.assertValueEqual(chest.countItem(Items.EMERALD), 0, "full inventory: bag item leaked into the chest");
        inventory.clearContent();
        chest.clearContent();
        SaddlebagAccess.set(donkey, new ItemStack(ModItems.GOLD_SADDLEBAG.get()));

        // player -> chest first while the chest has room; the bag only takes what the chest cannot.
        inventory.setItem(9, new ItemStack(Items.APPLE, 5));
        menu.quickMoveStack(player, firstMainSlot);
        helper.assertValueEqual(chest.countItem(Items.APPLE), 5, "player item should go to the chest first");
        helper.assertValueEqual(count(bagContents(donkey), Items.APPLE), 0, "player item went to the bag while the chest had room");
        helper.succeed();
    }

    /** Taking the bag out (click, swap, quick-move) carries its contents and leaves the sub-slots empty and inactive. */
    public static void removingBagCarriesContents(GameTestHelper helper) {
        Horse horse = tamedHorse(helper);
        saddle(horse);
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        Inventory inventory = player.getInventory();
        inventory.clearContent();
        SaddlebagAccess.set(horse, bagWith(ModItems.IRON_SADDLEBAG.get(),
            List.of(new ItemStack(Items.DIAMOND, 5), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 3))));
        StrideAndSireHorseMenu menu = new StrideAndSireHorseMenu(1, inventory, horse);
        int bagIndex = menu.getSaddlebagSlotIndex();
        int bagStart = menu.getSaddlebagContentStart();
        helper.assertTrue(menu.getSlot(bagStart).isActive(), "sub-slots should be active with a bag equipped");
        helper.assertTrue(menu.getSlot(bagStart).getItem().is(Items.DIAMOND), "sub-slot 0 should show the bag's diamonds");

        // Click the bag out onto the cursor.
        menu.clicked(bagIndex, 0, ContainerInput.PICKUP, player);
        ItemStack carried = menu.getCarried();
        helper.assertTrue(carried.is(ModItems.IRON_SADDLEBAG.get()), "picked-up bag should be carried");
        helper.assertValueEqual(count(SaddlebagItem.getContents(carried), Items.DIAMOND), 5, "carried bag diamonds");
        helper.assertValueEqual(count(SaddlebagItem.getContents(carried), Items.EMERALD), 3, "carried bag emeralds");
        helper.assertTrue(SaddlebagAccess.get(horse).isEmpty(), "bag should leave the horse");
        for (int i = 0; i < StrideAndSireHorseMenu.SADDLEBAG_CONTENT_SLOTS; i++) {
            Slot sub = menu.getSlot(bagStart + i);
            helper.assertTrue(!sub.isActive() && sub.getItem().isEmpty(), "sub-slot " + i + " should be empty and inactive");
            helper.assertTrue(!sub.mayPickup(player) && !sub.mayPlace(new ItemStack(Items.DIRT)), "sub-slot " + i + " should be locked");
        }
        // Clicking a now-inactive sub-slot with the bag must change nothing.
        menu.clicked(bagStart, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(menu.getCarried().is(ModItems.IRON_SADDLEBAG.get()), "bag should still be carried");
        helper.assertValueEqual(count(SaddlebagItem.getContents(menu.getCarried()), Items.DIAMOND), 5, "carried bag diamonds unchanged");

        // Put it back: contents visible again, exactly once.
        menu.clicked(bagIndex, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(menu.getCarried().isEmpty(), "bag should be equipped again");
        helper.assertValueEqual(count(bagContents(horse), Items.DIAMOND), 5, "re-equipped bag diamonds");
        helper.assertTrue(menu.getSlot(bagStart + 2).getItem().is(Items.EMERALD), "sub-slot 2 should show emeralds again");

        // Swap with an empty Leather bag: the Iron bag (with contents) goes to the cursor, the sub-slots show the new bag.
        menu.setCarried(new ItemStack(ModItems.LEATHER_SADDLEBAG.get()));
        menu.clicked(bagIndex, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(SaddlebagAccess.get(horse).is(ModItems.LEATHER_SADDLEBAG.get()), "leather bag should be equipped");
        helper.assertValueEqual(count(SaddlebagItem.getContents(menu.getCarried()), Items.DIAMOND), 5, "swapped-out bag diamonds");
        helper.assertTrue(menu.getSlot(bagStart).isActive() && menu.getSlot(bagStart).getItem().isEmpty(),
            "sub-slots should show the empty leather bag");
        // Writing into the new bag does not touch the swapped-out one.
        menu.getSlot(bagStart).set(new ItemStack(Items.GOLD_INGOT, 2));
        helper.assertValueEqual(count(SaddlebagItem.getContents(menu.getCarried()), Items.GOLD_INGOT), 0, "write leaked into the removed bag");
        helper.assertValueEqual(count(bagContents(horse), Items.GOLD_INGOT), 2, "write should reach the equipped bag");

        // Equip the Iron bag again, then quick-move it into the player inventory.
        menu.clicked(bagIndex, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(menu.getCarried().is(ModItems.LEATHER_SADDLEBAG.get()), "leather bag should now be carried");
        helper.assertValueEqual(count(SaddlebagItem.getContents(menu.getCarried()), Items.GOLD_INGOT), 2, "leather bag keeps its gold");
        menu.setCarried(ItemStack.EMPTY);
        menu.quickMoveStack(player, bagIndex);
        helper.assertTrue(SaddlebagAccess.get(horse).isEmpty(), "quick-moved bag should leave the horse");
        helper.assertValueEqual(inventory.countItem(ModItems.IRON_SADDLEBAG.get()), 1, "quick-moved bag in the player inventory");
        ItemStack moved = ItemStack.EMPTY;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(ModItems.IRON_SADDLEBAG.get())) {
                moved = inventory.getItem(i);
            }
        }
        helper.assertValueEqual(count(SaddlebagItem.getContents(moved), Items.DIAMOND), 5, "quick-moved bag diamonds");
        helper.assertValueEqual(inventory.countItem(Items.DIAMOND), 0, "contents must not spill into the player inventory");
        helper.assertTrue(!menu.getSlot(bagStart).isActive() && menu.getSlot(bagStart).getItem().isEmpty(),
            "sub-slots should be inactive after the quick-move");
        helper.succeed();
    }

    /** Placing an item into a sub-slot updates the equipped bag; a no-chest horse takes shift-clicked items into the bag. */
    public static void subSlotWritesReachStorage(GameTestHelper helper) {
        Horse horse = tamedHorse(helper);
        saddle(horse);
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        Inventory inventory = player.getInventory();
        inventory.clearContent();
        SaddlebagAccess.set(horse, new ItemStack(ModItems.LEATHER_SADDLEBAG.get()));
        StrideAndSireHorseMenu menu = new StrideAndSireHorseMenu(1, inventory, horse);
        int bagStart = menu.getSaddlebagContentStart();

        menu.setCarried(new ItemStack(Items.DIAMOND, 5));
        menu.clicked(bagStart + 3, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(menu.getCarried().isEmpty(), "placed diamonds should leave the cursor");
        ItemStack stored = bagContents(horse).get(3);
        helper.assertTrue(stored.is(Items.DIAMOND) && stored.getCount() == 5, "sub-slot 3 should hold 5 diamonds in storage, got " + stored);

        // Right-click picks up half; storage follows.
        menu.clicked(bagStart + 3, 1, ContainerInput.PICKUP, player);
        helper.assertValueEqual(menu.getCarried().getCount(), 3, "half picked up");
        helper.assertValueEqual(bagContents(horse).get(3).getCount(), 2, "storage after picking half");
        menu.setCarried(ItemStack.EMPTY);

        // No chest: a shift-clicked non-equipment item from the player inventory goes into the bag.
        inventory.setItem(9, new ItemStack(Items.EMERALD, 4));
        menu.quickMoveStack(player, 2); // horse without chest: menu slot 2 is player inventory slot 9
        helper.assertValueEqual(count(bagContents(horse), Items.EMERALD), 4, "shift-clicked emeralds should go into the bag");
        helper.assertValueEqual(inventory.countItem(Items.EMERALD), 0, "emeralds should leave the player inventory");
        helper.succeed();
    }

    /** Tracking clients get the saddlebag without its contents (render needs only the item); the stored bag is untouched. */
    public static void trackingSyncOmitsContents(GameTestHelper helper) {
        ItemStack bag = bagWith(ModItems.GOLD_SADDLEBAG.get(), List.of(new ItemStack(Items.DIAMOND, 5)));
        ItemStack tracked = HorseEquipmentSyncPacket.trackedCopy(HorseEquipmentSlot.SADDLEBAG, bag);
        helper.assertTrue(tracked.is(ModItems.GOLD_SADDLEBAG.get()), "tracked copy keeps the bag item");
        helper.assertTrue(SaddlebagItem.getContents(tracked).stream().allMatch(ItemStack::isEmpty), "tracked copy must omit contents");
        helper.assertValueEqual(count(SaddlebagItem.getContents(bag), Items.DIAMOND), 5, "source bag keeps its contents");
        ItemStack shoe = new ItemStack(ModItems.IRON_HORSESHOE.get());
        helper.assertTrue(ItemStack.matches(HorseEquipmentSyncPacket.trackedCopy(HorseEquipmentSlot.HORSESHOE, shoe), shoe),
            "horseshoe tracking copy unchanged");
        helper.succeed();
    }

    /** No nesting: a sub-slot rejects saddlebags (and shulker boxes), by mayPlace, click and quick-move. */
    public static void subSlotRejectsSaddlebag(GameTestHelper helper) {
        Horse horse = tamedHorse(helper);
        saddle(horse);
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        Inventory inventory = player.getInventory();
        inventory.clearContent();
        SaddlebagAccess.set(horse, new ItemStack(ModItems.GOLD_SADDLEBAG.get()));
        StrideAndSireHorseMenu menu = new StrideAndSireHorseMenu(1, inventory, horse);
        int bagStart = menu.getSaddlebagContentStart();
        Slot sub = menu.getSlot(bagStart);

        helper.assertTrue(!sub.mayPlace(new ItemStack(ModItems.LEATHER_SADDLEBAG.get())), "sub-slot must reject a saddlebag");
        helper.assertTrue(!sub.mayPlace(new ItemStack(Items.SHULKER_BOX)), "sub-slot must reject a shulker box");
        helper.assertTrue(sub.mayPlace(new ItemStack(Items.DIRT)), "sub-slot must accept dirt");

        menu.setCarried(new ItemStack(ModItems.LEATHER_SADDLEBAG.get()));
        menu.clicked(bagStart, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(menu.getCarried().is(ModItems.LEATHER_SADDLEBAG.get()), "rejected bag should stay carried");
        helper.assertTrue(bagContents(horse).stream().allMatch(ItemStack::isEmpty), "bag nested into the bag by click");
        menu.setCarried(ItemStack.EMPTY);

        // Slot occupied, so quick-move cannot equip it; it must not be nested either.
        inventory.setItem(9, new ItemStack(ModItems.NETHERITE_SADDLEBAG.get()));
        menu.quickMoveStack(player, 2);
        helper.assertTrue(bagContents(horse).stream().allMatch(ItemStack::isEmpty), "bag nested into the bag by quick-move");
        helper.assertValueEqual(inventory.countItem(ModItems.NETHERITE_SADDLEBAG.get()), 1, "netherite bag stays with the player");
        helper.assertTrue(SaddlebagAccess.get(horse).is(ModItems.GOLD_SADDLEBAG.get()), "equipped bag unchanged");

        // With the bag slot free and the horse saddled, quick-move equips the bag instead.
        SaddlebagAccess.set(horse, ItemStack.EMPTY);
        int playerSlot = -1;
        for (int i = 2; i < 2 + 36; i++) {
            if (menu.getSlot(i).getItem().is(ModItems.NETHERITE_SADDLEBAG.get())) {
                playerSlot = i;
            }
        }
        menu.quickMoveStack(player, playerSlot);
        helper.assertTrue(SaddlebagAccess.get(horse).is(ModItems.NETHERITE_SADDLEBAG.get()), "quick-move should equip the bag");
        helper.assertValueEqual(inventory.countItem(ModItems.NETHERITE_SADDLEBAG.get()), 0, "bag should leave the player");
        helper.succeed();
    }
}
