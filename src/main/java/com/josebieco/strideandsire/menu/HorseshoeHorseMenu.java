package com.josebieco.strideandsire.menu;

import com.josebieco.strideandsire.attachment.HorseshoeAccess;
import com.josebieco.strideandsire.registry.ModMenus;
import java.lang.reflect.Field;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractMountInventoryMenu;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Vanilla's horse inventory plus a horseshoe slot.
 *
 * <p>Extends {@link HorseInventoryMenu}, so the saddle slot (index 0), body-armor slot (1), chest slots (2..) and the
 * 36 player slots are built by vanilla code and keep their vanilla indices. The {@link HorseshoeSlot} is appended
 * last, at {@link #getHorseshoeSlotIndex()} ({@code 2 + chest size + 36}). Only used for horses that
 * {@link HorseshoeAccess#isEligible} accepts; Llamas and Camels keep the vanilla menu.
 */
public class HorseshoeHorseMenu extends HorseInventoryMenu {
    /** Slot position (item origin) in the screen: left column, below the body-armor slot. */
    public static final int HORSESHOE_SLOT_X = 8;
    public static final int HORSESHOE_SLOT_Y = 54;
    private static final int PLAYER_INVENTORY_SLOTS = 36;

    private static final Field HORSE_INVENTORY_FIELD = horseInventoryField();

    private final AbstractHorse horse;
    private final int inventoryColumns;
    private final HorseshoeSlot horseshoeSlot;
    private final int horseshoeSlotIndex;

    /** Server-side constructor: uses the horse's real inventory, exactly like {@code AbstractHorse.openCustomInventoryScreen}. */
    public HorseshoeHorseMenu(int containerId, Inventory playerInventory, AbstractHorse horse) {
        this(containerId, playerInventory, horseInventory(horse), horse, horse.getInventoryColumns());
    }

    public HorseshoeHorseMenu(int containerId, Inventory playerInventory, Container horseInventory, AbstractHorse horse, int inventoryColumns) {
        if (!HorseshoeAccess.isEligible(horse)) {
            throw new IllegalArgumentException("No horseshoe slot for " + horse.getType());
        }
        super(containerId, playerInventory, horseInventory, horse, inventoryColumns);
        this.horse = horse;
        this.inventoryColumns = inventoryColumns;
        this.horseshoeSlot = (HorseshoeSlot) this.addSlot(new HorseshoeSlot(horse, HORSESHOE_SLOT_X, HORSESHOE_SLOT_Y));
        this.horseshoeSlotIndex = this.horseshoeSlot.index;
    }

    /** Client-side factory for the registered {@link MenuType}; mirrors vanilla {@code handleMountScreenOpen}. */
    public static HorseshoeHorseMenu fromNetwork(int containerId, Inventory playerInventory, FriendlyByteBuf data) {
        int entityId = data.readVarInt();
        int inventoryColumns = data.readVarInt();
        if (!(playerInventory.player.level().getEntity(entityId) instanceof AbstractHorse horse)) {
            throw new IllegalStateException("Horse " + entityId + " for the horseshoe menu is not loaded on the client");
        }
        SimpleContainer container = new SimpleContainer(AbstractMountInventoryMenu.getInventorySize(inventoryColumns));
        return new HorseshoeHorseMenu(containerId, playerInventory, container, horse, inventoryColumns);
    }

    /** The extra data {@link #fromNetwork} reads. */
    public static void writeOpenData(FriendlyByteBuf data, AbstractHorse horse) {
        data.writeVarInt(horse.getId());
        data.writeVarInt(horse.getInventoryColumns());
    }

    /** The horse's own chest container ({@code AbstractHorse.inventory} is protected and has no getter). */
    public static Container horseInventory(AbstractHorse horse) {
        try {
            return (Container) HORSE_INVENTORY_FIELD.get(horse);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot read AbstractHorse.inventory", e);
        }
    }

    private static Field horseInventoryField() {
        try {
            Field field = AbstractHorse.class.getDeclaredField("inventory");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new IllegalStateException("Cannot access AbstractHorse.inventory", e);
        }
    }

    @Override
    public MenuType<?> getType() {
        return ModMenus.HORSESHOE_HORSE.get();
    }

    public AbstractHorse getHorse() {
        return horse;
    }

    public int getInventoryColumns() {
        return inventoryColumns;
    }

    public HorseshoeSlot getHorseshoeSlot() {
        return horseshoeSlot;
    }

    public int getHorseshoeSlotIndex() {
        return horseshoeSlotIndex;
    }

    /**
     * Vanilla {@code AbstractMountInventoryMenu.quickMoveStack}, with the player range bounded to the 36 player slots
     * (vanilla uses {@code slots.size()}, which would now include the horseshoe slot) and two additions: the horseshoe
     * slot shift-clicks into the player inventory, and a horseshoe from the player inventory goes to the empty
     * horseshoe slot after vanilla's armor/saddle checks. Only {@link HorseshoeSlot#mayPlace} items can enter it.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();
            int playerContainerStart = 2 + this.mountContainer.getContainerSize();
            int playerContainerEnd = playerContainerStart + 27;
            int playerHotBarStart = playerContainerEnd;
            int playerHotBarEnd = playerContainerStart + PLAYER_INVENTORY_SLOTS;
            if (slotIndex < playerContainerStart || slotIndex == this.horseshoeSlotIndex) {
                if (!this.moveItemStackTo(stack, playerContainerStart, playerHotBarEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.getSlot(1).mayPlace(stack) && !this.getSlot(1).hasItem()) {
                if (!this.moveItemStackTo(stack, 1, 2, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.getSlot(0).mayPlace(stack) && !this.getSlot(0).hasItem()) {
                if (!this.moveItemStackTo(stack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.horseshoeSlot.mayPlace(stack) && !this.horseshoeSlot.hasItem()) {
                if (!this.moveItemStackTo(stack, this.horseshoeSlotIndex, this.horseshoeSlotIndex + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.mountContainer.getContainerSize() == 0 || !this.moveItemStackTo(stack, 2, playerContainerStart, false)) {
                if (slotIndex >= playerHotBarStart && slotIndex < playerHotBarEnd) {
                    if (!this.moveItemStackTo(stack, playerContainerStart, playerContainerEnd, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (slotIndex >= playerContainerStart && slotIndex < playerContainerEnd) {
                    if (!this.moveItemStackTo(stack, playerHotBarStart, playerHotBarEnd, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(stack, playerHotBarStart, playerContainerEnd, false)) {
                    return ItemStack.EMPTY;
                }

                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return clicked;
    }
}
