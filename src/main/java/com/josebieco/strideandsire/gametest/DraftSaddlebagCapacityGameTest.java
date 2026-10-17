package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.attachment.HorseGeneticsAccess;
import com.josebieco.strideandsire.attachment.SaddlebagAccess;
import com.josebieco.strideandsire.genetics.LineageTrait;
import com.josebieco.strideandsire.genetics.TraitState;
import com.josebieco.strideandsire.item.saddlebag.SaddlebagItem;
import com.josebieco.strideandsire.menu.StrideAndSireHorseMenu;
import com.josebieco.strideandsire.registry.ModItems;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/**
 * Test instance: data/strideandsire/test_instance/genetics_draft_saddlebag_capacity.json
 */
public final class DraftSaddlebagCapacityGameTest {
    private DraftSaddlebagCapacityGameTest() {}

    /**
     * Task 10 Step 1:
     * (a) a saddlebag on a horse without Draft exposes exactly 9 addressable container slots in the menu;
     * (b) the same saddlebag, once the horse's Draft trait becomes MANIFEST, exposes all 18 on the next menu open;
     * (c) items already placed in slots 1–9 survive the trait being gained or lost.
     * Controller ruling (I3/I5): every menu always has all 18 bag sub-slots (constant slot count and indices, so server
     * and client never disagree); slots 10–18 are gated live through {@code isActive}/{@code mayPlace}/{@code mayPickup};
     * items hidden in them are preserved and reappear; the second row never overlaps the player inventory or its label.
     */
    public static void draftTraitUnlocksEighteenSlotsAndPreservesItems(GameTestHelper helper) {
        helper.makeFloor();
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);

        Horse horse = helper.spawn(EntityTypes.HORSE, 1, 1, 1);
        horse.setTamed(true);
        horse.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        SaddlebagAccess.set(horse, new ItemStack(ModItems.LEATHER_SADDLEBAG.get()));

        // (a) Without Draft, menu exposes exactly 9 addressable saddlebag container slots.
        StrideAndSireHorseMenu menuWithoutDraft = new StrideAndSireHorseMenu(1, player.getInventory(), horse);
        helper.assertValueEqual(
            countActiveSaddlebagSlots(menuWithoutDraft),
            9,
            "horse without Draft should expose 9 active saddlebag slots"
        );

        // Place items in slots 0..8 (1..9).
        int startWithoutDraft = menuWithoutDraft.getSaddlebagContentStart();
        for (int i = 0; i < 9; i++) {
            menuWithoutDraft.getSlot(startWithoutDraft + i).set(new ItemStack(Items.DIAMOND, i + 1));
        }

        // LATENT Draft must still expose only 9 slots.
        HorseGeneticsAccess.set(horse, LineageTrait.DRAFT, TraitState.LATENT);
        StrideAndSireHorseMenu menuLatentDraft = new StrideAndSireHorseMenu(2, player.getInventory(), horse);
        helper.assertValueEqual(
            countActiveSaddlebagSlots(menuLatentDraft),
            9,
            "horse with LATENT Draft should still expose only 9 active saddlebag slots"
        );

        // (b) Once Draft becomes MANIFEST, the next menu open exposes all 18 slots.
        HorseGeneticsAccess.set(horse, LineageTrait.DRAFT, TraitState.MANIFEST);
        StrideAndSireHorseMenu menuWithDraft = new StrideAndSireHorseMenu(3, player.getInventory(), horse);
        helper.assertValueEqual(
            countActiveSaddlebagSlots(menuWithDraft),
            18,
            "horse with MANIFEST Draft should expose 18 active saddlebag slots on next menu open"
        );

        // (c) Items placed in slots 1–9 survived gaining Draft; also write into slot 17 (18th slot).
        int startWithDraft = menuWithDraft.getSaddlebagContentStart();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = menuWithDraft.getSlot(startWithDraft + i).getItem();
            helper.assertTrue(
                stack.is(Items.DIAMOND) && stack.getCount() == i + 1,
                "slot " + i + " should preserve its diamonds after gaining Draft, got " + stack
            );
        }
        menuWithDraft.getSlot(startWithDraft + 17).set(new ItemStack(Items.EMERALD, 5));
        helper.assertTrue(
            menuWithDraft.getSlot(startWithDraft + 17).getItem().is(Items.EMERALD),
            "18th slot should accept items when Draft is MANIFEST"
        );

        // Remove Draft trait: next menu open exposes 9 slots again, and items in slots 1–9 are still intact.
        HorseGeneticsAccess.set(horse, LineageTrait.DRAFT, TraitState.ABSENT);
        StrideAndSireHorseMenu menuAfterLosingDraft = new StrideAndSireHorseMenu(4, player.getInventory(), horse);
        helper.assertValueEqual(
            countActiveSaddlebagSlots(menuAfterLosingDraft),
            9,
            "horse after losing Draft should expose 9 active saddlebag slots"
        );
        int startAfterLoss = menuAfterLosingDraft.getSaddlebagContentStart();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = menuAfterLosingDraft.getSlot(startAfterLoss + i).getItem();
            helper.assertTrue(
                stack.is(Items.DIAMOND) && stack.getCount() == i + 1,
                "slot " + i + " should preserve its diamonds after losing Draft, got " + stack
            );
        }

        // Constant slot list: every menu, Draft or not, has the same slots and all 18 bag sub-slots.
        int slotCount = menuWithoutDraft.slots.size();
        for (StrideAndSireHorseMenu menu : new StrideAndSireHorseMenu[] {menuLatentDraft, menuWithDraft, menuAfterLosingDraft}) {
            helper.assertValueEqual(menu.slots.size(), slotCount, "menu slot count must not depend on the Draft trait");
            helper.assertValueEqual(menu.getSaddlebagContentSlotCount(), 18, "bag sub-slots are always appended");
            helper.assertValueEqual(menu.getSaddlebagContentStart(), startWithoutDraft, "bag sub-slot indices are stable");
        }

        // Live gating on an already-open menu: the 18th slot locks while Draft is absent, keeps its item, and unlocks again.
        Slot eighteenth = menuWithDraft.getSlot(startWithDraft + 17);
        helper.assertTrue(!menuWithDraft.isDraftManifest(), "data slot must report the live (absent) Draft state");
        helper.assertTrue(!eighteenth.isActive() && !eighteenth.mayPlace(new ItemStack(Items.EMERALD))
                && !eighteenth.mayPickup(player),
            "18th slot must be inactive, reject placement and pickup while Draft is not MANIFEST");
        ItemStack hidden = SaddlebagItem.getContents(SaddlebagAccess.get(horse), SaddlebagItem.DRAFT_CONTAINER_SIZE).get(17);
        helper.assertTrue(hidden.is(Items.EMERALD) && hidden.getCount() == 5,
            "item in the hidden 18th slot must be preserved in the bag, got " + hidden);
        HorseGeneticsAccess.set(horse, LineageTrait.DRAFT, TraitState.MANIFEST);
        helper.assertTrue(menuWithDraft.isDraftManifest() && eighteenth.isActive() && eighteenth.mayPickup(player),
            "18th slot must unlock again when Draft is regained");
        helper.assertTrue(eighteenth.getItem().is(Items.EMERALD) && eighteenth.getItem().getCount() == 5,
            "hidden item must reappear in the 18th slot, got " + eighteenth.getItem());

        // Layout: the second bag row (frames 1px around each 16px item) ends above the "Inventory" label (y = first
        // player slot - 12) and the player inventory.
        int playerTop = Integer.MAX_VALUE;
        for (Slot slot : menuWithDraft.slots) {
            if (slot.container instanceof Inventory) {
                playerTop = Math.min(playerTop, slot.y);
            }
        }
        int secondRowFrameBottom = eighteenth.y + 17;
        helper.assertTrue(secondRowFrameBottom < playerTop - 12,
            "second bag row (frame bottom y=" + secondRowFrameBottom + ") overlaps the inventory label/slots (top y=" + playerTop + ")");
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
}
