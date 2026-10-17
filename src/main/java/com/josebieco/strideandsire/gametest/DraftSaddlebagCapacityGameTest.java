package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.attachment.HorseGeneticsAccess;
import com.josebieco.strideandsire.attachment.SaddlebagAccess;
import com.josebieco.strideandsire.genetics.LineageTrait;
import com.josebieco.strideandsire.genetics.TraitState;
import com.josebieco.strideandsire.menu.StrideAndSireHorseMenu;
import com.josebieco.strideandsire.registry.ModItems;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.Horse;
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
