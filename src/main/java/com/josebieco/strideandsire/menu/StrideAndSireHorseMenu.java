package com.josebieco.strideandsire.menu;

import com.josebieco.strideandsire.attachment.HorseGeneticsAccess;
import com.josebieco.strideandsire.attachment.HorseshoeAccess;
import com.josebieco.strideandsire.genetics.LineageTrait;
import com.josebieco.strideandsire.item.saddlebag.SaddlebagItem;
import com.josebieco.strideandsire.registry.ModMenus;
import java.lang.reflect.Field;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractMountInventoryMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Vanilla's horse inventory plus the mod's equipment: a horseshoe slot, a saddlebag slot and the equipped saddlebag's
 * container slots. All {@link #DRAFT_SADDLEBAG_CONTENT_SLOTS} (18) bag sub-slots are always present (stable indices,
 * identical slot lists on server and client); slots 10-18 are only active while the horse's {@link LineageTrait#DRAFT}
 * is {@code MANIFEST}. The server checks that live on every access (Review Focus item 5); the client learns it through a
 * menu {@link DataSlot}, never from tracking-synced genetics.
 */
public class StrideAndSireHorseMenu extends HorseInventoryMenu {
    /** Slot positions (item origins) in the screen: the left equipment column, below the body-armor slot. */
    public static final int HORSESHOE_SLOT_X = 8;
    public static final int HORSESHOE_SLOT_Y = 54;
    public static final int SADDLEBAG_SLOT_X = 8;
    public static final int SADDLEBAG_SLOT_Y = 72;
    /** Base saddlebag container slots (one row of 9) for horses without the Draft trait. */
    public static final int SADDLEBAG_CONTENT_SLOTS = 9;
    /** Expanded saddlebag container slots (two rows of 9 = 18) for horses with the Draft trait manifest. */
    public static final int DRAFT_SADDLEBAG_CONTENT_SLOTS = SaddlebagItem.DRAFT_CONTAINER_SIZE;
    public static final int SADDLEBAG_CONTENT_X = 8;
    public static final int SADDLEBAG_CONTENT_Y = 94;
    /**
     * How much lower than vanilla (y 84 / 142) the player inventory and hotbar are drawn: room for both bag rows
     * (y 94 and 112, frames down to y 130) plus the inventory label, for every horse so the layout never shifts.
     */
    public static final int PLAYER_INVENTORY_Y_OFFSET = 62;
    private static final int PLAYER_INVENTORY_SLOTS = 36;

    private static final Field HORSE_INVENTORY_FIELD = horseInventoryField();

    private final AbstractHorse horse;
    private final int inventoryColumns;
    private final HorseshoeSlot horseshoeSlot;
    private final int horseshoeSlotIndex;
    private final SaddlebagSlot saddlebagSlot;
    private final int saddlebagSlotIndex;
    private final int saddlebagContentStart;
    /** Server: the horse's live Draft state; client: the value the server sent through this data slot. */
    private final DataSlot draftManifest;

    /** Server-side constructor: uses the horse's real inventory, exactly like {@code AbstractHorse.openCustomInventoryScreen}. */
    public StrideAndSireHorseMenu(int containerId, Inventory playerInventory, AbstractHorse horse) {
        this(containerId, playerInventory, horseInventory(horse), horse, horse.getInventoryColumns());
    }

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
        boolean clientSide = horse.level().isClientSide();
        this.draftManifest = this.addDataSlot(clientSide ? DataSlot.standalone() : new DataSlot() {
            @Override
            public int get() {
                return HorseGeneticsAccess.hasManifest(horse, LineageTrait.DRAFT) ? 1 : 0;
            }

            @Override
            public void set(int value) {
                // server side: always the live trait state
            }
        });
        Container bagContents = clientSide
            ? new SimpleContainer(DRAFT_SADDLEBAG_CONTENT_SLOTS)
            : new SaddlebagContentsContainer(horse);
        this.saddlebagContentStart = this.slots.size();
        for (int i = 0; i < DRAFT_SADDLEBAG_CONTENT_SLOTS; i++) {
            int col = i % SADDLEBAG_CONTENT_SLOTS;
            int row = i / SADDLEBAG_CONTENT_SLOTS;
            this.addSlot(new SaddlebagContentSlot(
                bagContents,
                i,
                horse,
                this::isDraftManifest,
                SADDLEBAG_CONTENT_X + col * 18,
                SADDLEBAG_CONTENT_Y + row * 18
            ));
        }
    }

    /** Whether the Draft bag slots (10-18) are unlocked: live on the server, synced value on the client. */
    public boolean isDraftManifest() {
        return this.draftManifest.get() != 0;
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

    /** Menu index of the first saddlebag container slot. */
    public int getSaddlebagContentStart() {
        return saddlebagContentStart;
    }

    /** Number of saddlebag container slots in every menu instance (always 18; see {@link #getActiveSaddlebagSlotCount}). */
    public int getSaddlebagContentSlotCount() {
        return DRAFT_SADDLEBAG_CONTENT_SLOTS;
    }

    /** Number of currently active (addressable) saddlebag container slots in this menu (0, 9, or 18); always a prefix. */
    public int getActiveSaddlebagSlotCount() {
        int active = 0;
        for (int i = 0; i < DRAFT_SADDLEBAG_CONTENT_SLOTS; i++) {
            if (this.getSlot(saddlebagContentStart + i).isActive()) {
                active++;
            }
        }
        return active;
    }

    public boolean isSaddlebagContentSlot(int slotIndex) {
        return slotIndex >= saddlebagContentStart && slotIndex < saddlebagContentStart + DRAFT_SADDLEBAG_CONTENT_SLOTS;
    }

    /** True while a saddlebag is equipped, i.e. its container slots are usable. */
    public boolean hasSaddlebagContents() {
        return this.getSlot(saddlebagContentStart).isActive();
    }

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
        int activeSlots = this.getActiveSaddlebagSlotCount();
        return activeSlots > 0 && SaddlebagContentSlot.canStore(stack)
            && this.moveItemStackTo(stack, this.saddlebagContentStart, this.saddlebagContentStart + activeSlots, false);
    }
}
