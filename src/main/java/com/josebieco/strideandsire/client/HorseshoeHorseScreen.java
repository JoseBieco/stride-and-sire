package com.josebieco.strideandsire.client;

import com.josebieco.strideandsire.menu.HorseshoeHorseMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.HorseInventoryMenu;

/** Vanilla's horse screen (background, saddle/armor/chest slots, horse preview) plus the horseshoe slot frame. */
public class HorseshoeHorseScreen extends HorseInventoryScreen {
    public HorseshoeHorseScreen(HorseInventoryMenu menu, Inventory inventory, Component title) {
        this((HorseshoeHorseMenu) menu, inventory);
    }

    private HorseshoeHorseScreen(HorseshoeHorseMenu menu, Inventory inventory) {
        super(menu, inventory, menu.getHorse(), menu.getInventoryColumns());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = (this.width - this.imageWidth) / 2;
        int yo = (this.height - this.imageHeight) / 2;
        this.extractSlot(graphics, xo + HorseshoeHorseMenu.HORSESHOE_SLOT_X - 1, yo + HorseshoeHorseMenu.HORSESHOE_SLOT_Y - 1);
    }
}
