package com.josebieco.strideandsire.client;

import com.josebieco.strideandsire.menu.StrideAndSireHorseMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Inventory;

/**
 * Vanilla's horse screen ({@code HorseInventoryScreen}: background, saddle/armor/chest slots, horse preview) made
 * {@link StrideAndSireHorseMenu#PLAYER_INVENTORY_Y_OFFSET} taller, plus the horseshoe and saddlebag slot frames and,
 * while a saddlebag is equipped, the frames of its 9-slot row between the horse area and the player inventory.
 *
 * <p>Placeholder art: the extra height is filled by repeating a blank strip of vanilla's {@code horse.png}.
 */
public class StrideAndSireHorseScreen extends AbstractContainerScreen<StrideAndSireHorseMenu> {
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
    private static final Identifier CHEST_SLOTS_SPRITE = Identifier.withDefaultNamespace("container/horse/chest_slots");
    private static final Identifier HORSE_INVENTORY_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/horse.png");
    private static final int VANILLA_HEIGHT = 166;
    /** horse.png rows: everything above the player inventory frames, and a blank strip used as filler. */
    private static final int TOP_PART_HEIGHT = 83;
    private static final int FILLER_V = 72;
    private static final int FILLER_HEIGHT = 10;

    private final AbstractHorse horse;
    private final int inventoryColumns;
    private float xMouse;
    private float yMouse;

    public StrideAndSireHorseScreen(StrideAndSireHorseMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, menu.getHorse().getDisplayName(), 176, VANILLA_HEIGHT + StrideAndSireHorseMenu.PLAYER_INVENTORY_Y_OFFSET);
        this.horse = menu.getHorse();
        this.inventoryColumns = menu.getInventoryColumns();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = (this.width - this.imageWidth) / 2;
        int yo = (this.height - this.imageHeight) / 2;
        int offset = StrideAndSireHorseMenu.PLAYER_INVENTORY_Y_OFFSET;

        // Vanilla background, split above the player inventory, with a blank filler strip in between.
        graphics.blit(RenderPipelines.GUI_TEXTURED, HORSE_INVENTORY_LOCATION, xo, yo, 0.0F, 0.0F, this.imageWidth, TOP_PART_HEIGHT, 256, 256);
        for (int dy = 0; dy < offset; dy += FILLER_HEIGHT) {
            int h = Math.min(FILLER_HEIGHT, offset - dy);
            graphics.blit(RenderPipelines.GUI_TEXTURED, HORSE_INVENTORY_LOCATION, xo, yo + TOP_PART_HEIGHT + dy,
                0.0F, FILLER_V, this.imageWidth, h, 256, 256);
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, HORSE_INVENTORY_LOCATION, xo, yo + TOP_PART_HEIGHT + offset,
            0.0F, TOP_PART_HEIGHT, this.imageWidth, VANILLA_HEIGHT - TOP_PART_HEIGHT, 256, 256);

        if (this.inventoryColumns > 0) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, CHEST_SLOTS_SPRITE, 90, 54, 0, 0, xo + 79, yo + 17, this.inventoryColumns * 18, 54);
        }
        if (this.horse.canUseSlot(EquipmentSlot.SADDLE) && this.horse.is(EntityTypeTags.CAN_EQUIP_SADDLE)) {
            this.extractSlot(graphics, xo + 7, yo + 17);
        }
        if (this.horse.canUseSlot(EquipmentSlot.BODY) && this.horse.is(EntityTypeTags.CAN_WEAR_HORSE_ARMOR)) {
            this.extractSlot(graphics, xo + 7, yo + 35);
        }
        this.extractSlot(graphics, xo + StrideAndSireHorseMenu.HORSESHOE_SLOT_X - 1, yo + StrideAndSireHorseMenu.HORSESHOE_SLOT_Y - 1);
        this.extractSlot(graphics, xo + StrideAndSireHorseMenu.SADDLEBAG_SLOT_X - 1, yo + StrideAndSireHorseMenu.SADDLEBAG_SLOT_Y - 1);
        if (this.menu.hasSaddlebagContents()) {
            for (int i = 0; i < StrideAndSireHorseMenu.SADDLEBAG_CONTENT_SLOTS; i++) {
                this.extractSlot(graphics, xo + StrideAndSireHorseMenu.SADDLEBAG_CONTENT_X - 1 + i * 18,
                    yo + StrideAndSireHorseMenu.SADDLEBAG_CONTENT_Y - 1);
            }
        }

        InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, xo + 26, yo + 18, xo + 78, yo + 70, 17, 0.25F, this.xMouse, this.yMouse, this.horse);
    }

    private void extractSlot(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x, y, 18, 18);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.xMouse = mouseX;
        this.yMouse = mouseY;
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }
}
