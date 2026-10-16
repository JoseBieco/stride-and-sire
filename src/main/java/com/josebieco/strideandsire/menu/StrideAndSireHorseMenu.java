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
 * Vanilla's horse inventory plus the mod's equipment: a horseshoe slot, a saddlebag slot and the equipped saddlebag's
 * container slots.
 *
 * <p>Extends {@link HorseInventoryMenu}, so the saddle slot (index 0), body-armor slot (1), chest slots (2..) and the
 * 36 player slots are built by vanilla code and keep their vanilla indices; the player slots are only drawn
 * {@link #PLAYER_INVENTORY_Y_OFFSET} lower, to make room for the saddlebag row. Appended after them, in this order
 * (C = chest size, 0 or {@code 3 * inventoryColumns}):
 * <ul>
 *   <li>{@code 2 + C + 36}: the {@link HorseshoeSlot} ({@link #getHorseshoeSlotIndex()});</li>
 *   <li>{@code 2 + C + 37}: the {@link SaddlebagSlot} ({@link #getSaddlebagSlotIndex()});</li>
 *   <li>{@code 2 + C + 38} to {@code 2 + C + 46}: the 9 {@link SaddlebagContentSlot}s
 *       ({@link #getSaddlebagContentStart()}), bound to the equipped bag's container and active only while a bag is
 *       equipped.</li>
 * </ul>
 * The native chest (Donkey/Mule) and the saddlebag container are separate containers; {@link #quickMoveStack} only
 * moves items between either of them and the player inventory, never between the two. Only used for horses that
 * {@link HorseshoeAccess#isEligible} accepts; Llamas and Camels keep the vanilla menu.
 */
public class StrideAndSireHorseMenu extends HorseInventoryMenu {
    /** Slot positions (item origins) in the screen: the left equipment column, below the body-armor slot. */
    public static final int HORSESHOE_SLOT_X = 8;
    public static final int HORSESHOE_SLOT_Y = 54;
    public static final int SADDLEBAG_SLOT_X = 8;
    public static final int SADDLEBAG_SLOT_Y = 72;
    /** The saddlebag's container: one row of 9 between the horse area and the player inventory. */
    public static final int SADDLEBAG_CONTENT_SLOTS = 9;
    public static final int SADDLEBAG_CONTENT_X = 8;
    public static final int SADDLEBAG_CONTENT_Y = 94;
    /** How much lower than vanilla (y 84 / 142) the player inventory and hotbar are drawn. */
    public static final int PLAYER_INVENTORY_Y_OFFSET = 44;
    private static final int PLAYER_INVENTORY_SLOTS = 36;

    private static final Field HORSE_INVENTORY_FIELD = horseInventoryField();

    private final AbstractHorse horse;
    private final int inventoryColumns;
    private final HorseshoeSlot horseshoeSlot;
    private final int horseshoeSlotIndex;
    private final SaddlebagSlot saddlebagSlot;
    private final int saddlebagSlotIndex;
    private final int saddlebagContentStart;

    /** Server-side constructor: uses the horse's real inventory, exactly like {@code AbstractHorse.openCustomInventoryScreen}. */
    public StrideAndSireHorseMenu(int containerId, Inventory playerInventory, AbstractHorse horse) {
        this(containerId, playerInventory, horseInventory(horse), horse, horse.getInventoryColumns());
    }

    /**
     * On the server the saddlebag container slots view the equipped bag's container component
     * ({@link SaddlebagContentsContainer}); on the client they are a plain 9-slot container filled by the menu's normal
     * slot sync, so the client never needs a copy of the bag's contents from the horse tracking sync.
     */
    public StrideAndSireHorseMenu(int containerId, Inventory playerInventory, Container horseInventory, AbstractHorse horse, int inventoryColumns) {
        if (!HorseshoeAccess.isEligible(horse)) {
            throw new IllegalArgumentException("No horseshoe slot for " + horse.getType());
        }
        super(containerId, playerInventory, horseInventory, horse, inventoryColumns);
        this.horse = horse;
        this.inventoryColumns = inventoryColumns;
        this.horseshoeSlot = (HorseshoeSlot) this.addSlot(new HorseshoeSlot(horse, HORSESHOE_SLOT_X, HORSESHOE_SLOT_Y));
        this.horseshoeSlotIndex = this.horseshoeSlot.index;
        this.saddlebagSlot = (SaddlebagSlot) this.addSlot(new SaddlebagSlot(horse, SADDLEBAG_SLOT_X, SADDLEBAG_SLOT_Y));
        this.saddlebagSlotIndex = this.saddlebagSlot.index;
        Container bagContents = horse.level().isClientSide()
            ? new SimpleContainer(SADDLEBAG_CONTENT_SLOTS)
            : new SaddlebagContentsContainer(horse);
        this.saddlebagContentStart = this.slots.size();
        for (int i = 0; i < SADDLEBAG_CONTENT_SLOTS; i++) {
            this.addSlot(new SaddlebagContentSlot(bagContents, i, horse, SADDLEBAG_CONTENT_X + i * 18, SADDLEBAG_CONTENT_Y));
        }
    }

    /** Vanilla's player inventory and hotbar, moved down by {@link #PLAYER_INVENTORY_Y_OFFSET} (same slots, same order). */
    @Override
    protected void addStandardInventorySlots(Container container, int left, int top) {
        super.addStandardInventorySlots(container, left, top + PLAYER_INVENTORY_Y_OFFSET);
    }

    /** Client-side factory for the registered {@link MenuType}; mirrors vanilla {@code handleMountScreenOpen}. */
    public static StrideAndSireHorseMenu fromNetwork(int containerId, Inventory playerInventory, FriendlyByteBuf data) {
        int entityId = data.readVarInt();
        int inventoryColumns = data.readVarInt();
        if (!(playerInventory.player.level().getEntity(entityId) instanceof AbstractHorse horse)) {
            throw new IllegalStateException("Horse " + entityId + " for the horseshoe menu is not loaded on the client");
        }
        SimpleContainer container = new SimpleContainer(AbstractMountInventoryMenu.getInventorySize(inventoryColumns));
        return new StrideAndSireHorseMenu(containerId, playerInventory, container, horse, inventoryColumns);
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

    public SaddlebagSlot getSaddlebagSlot() {
        return saddlebagSlot;
    }

    public int getSaddlebagSlotIndex() {
        return saddlebagSlotIndex;
    }

    /** Menu index of the first of the {@link #SADDLEBAG_CONTENT_SLOTS} saddlebag container slots. */
    public int getSaddlebagContentStart() {
        return saddlebagContentStart;
    }

    public boolean isSaddlebagContentSlot(int slotIndex) {
        return slotIndex >= saddlebagContentStart && slotIndex < saddlebagContentStart + SADDLEBAG_CONTENT_SLOTS;
    }

    /** True while a saddlebag is equipped, i.e. its container slots are usable. */
    public boolean hasSaddlebagContents() {
        return this.getSlot(saddlebagContentStart).isActive();
    }

    /**
     * Vanilla {@code AbstractMountInventoryMenu.quickMoveStack}, with the player range bounded to the 36 player slots
     * (vanilla uses {@code slots.size()}, which would now include the appended slots), plus:
     * <ul>
     *   <li>from the horseshoe slot, the saddlebag slot or a saddlebag container slot: into the player inventory only
     *       (like vanilla's saddle/armor/chest slots);</li>
     *   <li>from the player inventory, after vanilla's armor and saddle checks: a horseshoe to the empty horseshoe slot,
     *       a saddlebag to the empty saddlebag slot (saddled horse only), then other items to the native chest if
     *       there is one and it takes anything, else to the equipped saddlebag's container, else vanilla's
     *       main-inventory/hotbar swap.</li>
     * </ul>
     * So chest items and saddlebag items only ever move to or from the player inventory, never between each other.
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
            if (slotIndex < playerContainerStart || slotIndex >= playerHotBarEnd) {
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
            } else if (this.saddlebagSlot.mayPlace(stack) && !this.saddlebagSlot.hasItem()) {
                if (!this.moveItemStackTo(stack, this.saddlebagSlotIndex, this.saddlebagSlotIndex + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveToHorseStorage(stack, playerContainerStart)) {
                if (slotIndex >= playerHotBarStart) {
                    if (!this.moveItemStackTo(stack, playerContainerStart, playerContainerEnd, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(stack, playerHotBarStart, playerHotBarEnd, false)) {
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

    /** A player-inventory item into the native chest (if there is one and it takes anything), else the equipped saddlebag. */
    private boolean moveToHorseStorage(ItemStack stack, int chestEnd) {
        if (this.mountContainer.getContainerSize() > 0 && this.moveItemStackTo(stack, 2, chestEnd, false)) {
            return true;
        }
        return this.hasSaddlebagContents() && SaddlebagContentSlot.canStore(stack)
            && this.moveItemStackTo(stack, this.saddlebagContentStart, this.saddlebagContentStart + SADDLEBAG_CONTENT_SLOTS, false);
    }
}
