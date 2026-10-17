package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.menu.HorseshoeSlot;
import com.josebieco.strideandsire.menu.SaddlebagSlot;
import com.josebieco.strideandsire.menu.StrideAndSireHorseMenu;
import com.josebieco.strideandsire.registry.ModItems;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/**
 * Test instances: data/strideandsire/test_instance/equipment_slot_icons.json
 */
public final class EquipmentSlotIconGameTest {
    private EquipmentSlotIconGameTest() {}

    /** Brief Step 1: both equipment slots report their silhouette; the bag slot still refuses a bag without a saddle. */
    public static void equipmentSlotsReportEmptyIcons(GameTestHelper helper) {
        helper.makeFloor();
        Horse horse = helper.spawn(EntityTypes.HORSE, 2, 1, 2);
        horse.setTamed(true);
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        StrideAndSireHorseMenu menu = new StrideAndSireHorseMenu(1, player.getInventory(), horse);

        helper.assertValueEqual(menu.getHorseshoeSlot().getNoItemIcon(),
            Identifier.fromNamespaceAndPath("strideandsire", "container/slot/horseshoe"), "horseshoe slot icon");
        helper.assertValueEqual(menu.getSaddlebagSlot().getNoItemIcon(),
            Identifier.fromNamespaceAndPath("strideandsire", "container/slot/saddlebag"), "saddlebag slot icon");
        helper.assertValueEqual(HorseshoeSlot.EMPTY_ICON, menu.getHorseshoeSlot().getNoItemIcon(), "horseshoe constant");
        helper.assertValueEqual(SaddlebagSlot.EMPTY_ICON, menu.getSaddlebagSlot().getNoItemIcon(), "saddlebag constant");
        helper.assertTrue(!horse.isSaddled(), "precondition: horse is not saddled");
        helper.assertTrue(!menu.getSaddlebagSlot().mayPlace(new ItemStack(ModItems.LEATHER_SADDLEBAG.get())),
            "saddlebag slot must refuse a bag on a saddle-less horse");
        helper.succeed();
    }
}
