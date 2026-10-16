package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.attachment.HorseshoeAccess;
import com.josebieco.strideandsire.menu.StrideAndSireHorseMenu;
import com.josebieco.strideandsire.menu.HorseshoeSlot;
import com.josebieco.strideandsire.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Donkey;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/**
 * Test instances: data/strideandsire/test_instance/horseshoe_menu_*.json
 */
public final class HorseshoeMenuGameTest {
    private HorseshoeMenuGameTest() {}

    private static Horse tamedHorse(GameTestHelper helper) {
        helper.makeFloor();
        Horse horse = helper.spawn(EntityTypes.HORSE, 2, 1, 2);
        horse.setTamed(true);
        return horse;
    }

    /** Brief Step 1: the slot rejects a diamond, accepts a gold horseshoe, and the saddle slot is unaffected. */
    public static void slotAcceptsOnlyHorseshoes(GameTestHelper helper) {
        Horse horse = tamedHorse(helper);
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        StrideAndSireHorseMenu menu = new StrideAndSireHorseMenu(1, player.getInventory(), horse);
        HorseshoeSlot shoeSlot = menu.getHorseshoeSlot();

        helper.assertTrue(!shoeSlot.mayPlace(new ItemStack(Items.DIAMOND)), "horseshoe slot must reject a diamond");
        helper.assertTrue(shoeSlot.mayPlace(new ItemStack(ModItems.GOLD_HORSESHOE.get())), "horseshoe slot must accept a gold horseshoe");
        helper.assertValueEqual(shoeSlot.getMaxStackSize(), 1, "horseshoe slot max stack size");

        Slot saddleSlot = menu.getSlot(0);
        helper.assertTrue(saddleSlot.mayPlace(new ItemStack(Items.SADDLE)), "saddle slot must still accept a saddle");
        helper.assertTrue(!saddleSlot.mayPlace(new ItemStack(ModItems.GOLD_HORSESHOE.get())), "saddle slot must not accept a horseshoe");
        saddleSlot.setByPlayer(new ItemStack(Items.SADDLE));
        helper.assertTrue(horse.getItemBySlot(EquipmentSlot.SADDLE).is(Items.SADDLE), "saddle slot must still equip the saddle");
        helper.assertTrue(HorseshoeAccess.get(horse).isEmpty(), "saddling must not touch the horseshoe storage");
        helper.succeed();
    }

    /** Strictly additive: every vanilla slot keeps its index, type, container index and position; one extra slot is last. */
    public static void keepsVanillaSlotLayout(GameTestHelper helper) {
        helper.makeFloor();
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        Horse horse = helper.spawn(EntityTypes.HORSE, 1, 1, 1);
        horse.setTamed(true);
        Donkey donkey = helper.spawn(EntityTypes.DONKEY, 3, 1, 3);
        donkey.setTamed(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CHEST));
        donkey.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(donkey.hasChest() && donkey.getInventoryColumns() > 0, "donkey should have a chest");

        for (AbstractHorse animal : new AbstractHorse[] {horse, donkey}) {
            StrideAndSireHorseMenu menu = new StrideAndSireHorseMenu(1, player.getInventory(), animal);
            HorseInventoryMenu vanilla = new HorseInventoryMenu(
                2, player.getInventory(), StrideAndSireHorseMenu.horseInventory(animal), animal, animal.getInventoryColumns());
            // + horseshoe slot, saddlebag slot and the saddlebag's 9 container slots (Fase B)
            helper.assertValueEqual(menu.slots.size(), vanilla.slots.size() + 2 + StrideAndSireHorseMenu.SADDLEBAG_CONTENT_SLOTS,
                animal.getType() + " slot count");
            for (int i = 0; i < vanilla.slots.size(); i++) {
                Slot ours = menu.getSlot(i);
                Slot theirs = vanilla.getSlot(i);
                helper.assertTrue(ours.container == theirs.container || ours.container.getClass() == theirs.container.getClass(),
                    animal.getType() + " slot " + i + " container differs");
                // Player slots are drawn lower to make room for the saddlebag row; everything else is vanilla.
                int expectedY = theirs.y + (theirs.container instanceof Inventory ? StrideAndSireHorseMenu.PLAYER_INVENTORY_Y_OFFSET : 0);
                helper.assertTrue(ours.getContainerSlot() == theirs.getContainerSlot() && ours.x == theirs.x && ours.y == expectedY
                    && ours.getClass() == theirs.getClass(), animal.getType() + " slot " + i + " differs");
            }
            helper.assertValueEqual(menu.getHorseshoeSlotIndex(), vanilla.slots.size(), animal.getType() + " horseshoe slot index");
            helper.assertTrue(menu.getSlot(menu.getHorseshoeSlotIndex()) == menu.getHorseshoeSlot(), "horseshoe slot not at its index");
        }

        // A horseshoe shift-clicked out of the donkey's chest goes to the player inventory, never the horseshoe slot (vanilla parity).
        StrideAndSireHorseMenu menu = new StrideAndSireHorseMenu(1, player.getInventory(), donkey);
        player.getInventory().clearContent();
        menu.getSlot(2).set(new ItemStack(ModItems.IRON_HORSESHOE.get()));
        menu.quickMoveStack(player, 2);
        helper.assertTrue(HorseshoeAccess.get(donkey).isEmpty(), "chest horseshoe must not be shift-moved into the horseshoe slot");
        helper.assertTrue(player.getInventory().countItem(ModItems.IRON_HORSESHOE.get()) == 1, "chest horseshoe should reach the player inventory");
        helper.succeed();
    }

    /** Shift-click never puts a non-horseshoe in the slot; a horseshoe shift-clicked or placed lands in HorseshoeAccess. */
    public static void quickMoveAndPlaceRouteToStorage(GameTestHelper helper) {
        Horse horse = tamedHorse(helper);
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        Inventory inventory = player.getInventory();
        StrideAndSireHorseMenu menu = new StrideAndSireHorseMenu(1, inventory, horse);
        int shoeIndex = menu.getHorseshoeSlotIndex();
        int playerStart = 2; // horse without chest: saddle, armor, then 27 main + 9 hotbar
        int firstMainSlot = playerStart; // inventory slot 9

        // A diamond shift-clicked from the player inventory never lands in the horseshoe slot.
        inventory.setItem(9, new ItemStack(Items.DIAMOND));
        menu.quickMoveStack(player, firstMainSlot);
        helper.assertTrue(HorseshoeAccess.get(horse).isEmpty(), "diamond leaked into horseshoe storage");
        helper.assertTrue(!menu.getHorseshoeSlot().hasItem(), "diamond leaked into horseshoe slot");
        helper.assertValueEqual(inventory.countItem(Items.DIAMOND), 1, "diamond count");
        inventory.clearContent();

        // A diamond placed by click is rejected and stays on the cursor.
        menu.setCarried(new ItemStack(Items.DIAMOND));
        menu.clicked(shoeIndex, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(HorseshoeAccess.get(horse).isEmpty(), "clicked diamond accepted into horseshoe storage");
        helper.assertTrue(menu.getCarried().is(Items.DIAMOND), "rejected diamond should stay carried");
        menu.setCarried(ItemStack.EMPTY);

        // A gold horseshoe shift-clicked from the player inventory goes to the horseshoe slot and storage.
        inventory.setItem(9, new ItemStack(ModItems.GOLD_HORSESHOE.get()));
        menu.quickMoveStack(player, firstMainSlot);
        helper.assertTrue(HorseshoeAccess.get(horse).is(ModItems.GOLD_HORSESHOE.get()), "shift-clicked horseshoe not stored on horse");
        helper.assertTrue(inventory.getItem(9).isEmpty(), "shift-clicked horseshoe should leave the player inventory");

        // Shift-clicking it back out returns it to the player and clears storage.
        menu.quickMoveStack(player, shoeIndex);
        helper.assertTrue(HorseshoeAccess.get(horse).isEmpty(), "horseshoe should be removed from storage");
        helper.assertValueEqual(inventory.countItem(ModItems.GOLD_HORSESHOE.get()), 1, "horseshoe back in player inventory");
        inventory.clearContent();

        // Placing a horseshoe by click stores it; picking it up clears storage.
        menu.setCarried(new ItemStack(ModItems.IRON_HORSESHOE.get()));
        menu.clicked(shoeIndex, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(HorseshoeAccess.get(horse).is(ModItems.IRON_HORSESHOE.get()), "clicked horseshoe not stored on horse");
        helper.assertTrue(menu.getCarried().isEmpty(), "placed horseshoe should leave the cursor");
        menu.clicked(shoeIndex, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(HorseshoeAccess.get(horse).isEmpty(), "picked-up horseshoe should leave storage");
        helper.assertTrue(menu.getCarried().is(ModItems.IRON_HORSESHOE.get()), "picked-up horseshoe should be carried");
        menu.setCarried(ItemStack.EMPTY);

        // A horseshoe set through HorseshoeAccess shows up in the slot (the slot reads storage).
        HorseshoeAccess.set(horse, new ItemStack(ModItems.NETHERITE_HORSESHOE.get()));
        helper.assertTrue(menu.getHorseshoeSlot().getItem().is(ModItems.NETHERITE_HORSESHOE.get()), "slot should read storage");
        helper.succeed();
    }

    /** A ServerPlayer with a real (embedded) connection, which {@code openMenu} needs; removed when the test ends. */
    @SuppressWarnings("removal")
    private static ServerPlayer connectedPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        helper.addCleanup(passed -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    /** Sneak + right-click on a tamed adult horse with {@code held} in the main hand, as the server sees it. */
    private static void sneakInteract(ServerPlayer player, AbstractHorse horse, ItemStack held) {
        player.closeContainer();
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        player.setShiftKeyDown(true);
        player.interactOn(horse, InteractionHand.MAIN_HAND, horse.position());
    }

    /**
     * Vanilla's tamed-adult sneak path ({@code Horse}/{@code AbstractChestedHorse.mobInteract} skip item handling, then
     * {@code AbstractHorse.mobInteract} opens the inventory) runs whatever the player holds; so must ours.
     */
    public static void sneakInteractWithItemOpensHorseshoeMenu(GameTestHelper helper) {
        Horse horse = tamedHorse(helper);
        Donkey donkey = helper.spawn(EntityTypes.DONKEY, 0, 1, 0);
        donkey.setTamed(true);
        ServerPlayer player = connectedPlayer(helper);

        ItemStack[] held = {ItemStack.EMPTY, new ItemStack(ModItems.IRON_HORSESHOE.get()), new ItemStack(Items.APPLE),
            new ItemStack(Items.NAME_TAG)}; // an unnamed name tag does nothing in vanilla, so the inventory opens
        for (AbstractHorse target : new AbstractHorse[] {horse, donkey}) {
            for (ItemStack stack : held) {
                sneakInteract(player, target, stack.copy());
                helper.assertTrue(player.containerMenu instanceof StrideAndSireHorseMenu,
                    "sneak-click on " + target.getType() + " holding " + stack + " opened " + player.containerMenu);
            }
        }
        helper.assertTrue(HorseshoeAccess.get(horse).isEmpty(), "sneak-click must not equip the held horseshoe");
        helper.succeed();
    }

    /** Interactions vanilla runs before {@code mobInteract} (a named name tag) still win over our menu. */
    public static void sneakInteractNameTagLeftToVanilla(GameTestHelper helper) {
        Horse horse = tamedHorse(helper);
        ServerPlayer player = connectedPlayer(helper);
        ItemStack tag = new ItemStack(Items.NAME_TAG);
        tag.set(DataComponents.CUSTOM_NAME, Component.literal("Epona"));

        sneakInteract(player, horse, tag);
        helper.assertTrue(!(player.containerMenu instanceof StrideAndSireHorseMenu), "named name tag opened the horseshoe menu");
        helper.assertTrue(horse.getCustomName() != null && "Epona".equals(horse.getCustomName().getString()),
            "named name tag should rename the horse, name is " + horse.getCustomName());
        helper.succeed();
    }
}
