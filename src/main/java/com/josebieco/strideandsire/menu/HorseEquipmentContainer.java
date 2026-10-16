package com.josebieco.strideandsire.menu;

import com.josebieco.strideandsire.attachment.HorseEquipmentSlot;
import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * One-slot {@link Container} view over one of the horse's mod equipment slots ({@link HorseEquipmentSlot}); it stores
 * nothing itself. Reads go through the slot's {@code *Access.get} (the live stack), every write through its
 * {@code *Access.set} (the write choke point).
 */
final class HorseEquipmentContainer implements Container {
    private final AbstractHorse horse;
    private final HorseEquipmentSlot slot;

    HorseEquipmentContainer(AbstractHorse horse, HorseEquipmentSlot slot) {
        this.horse = horse;
        this.slot = slot;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return slot.get(horse).isEmpty();
    }

    @Override
    public ItemStack getItem(int index) {
        return index == 0 ? slot.get(horse) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        ItemStack stored = getItem(index);
        if (stored.isEmpty() || count <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack taken = stored.split(count);
        slot.set(horse, stored.isEmpty() ? ItemStack.EMPTY : stored);
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        ItemStack stored = getItem(index);
        if (stored.isEmpty()) {
            return ItemStack.EMPTY;
        }
        slot.set(horse, ItemStack.EMPTY);
        return stored;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        if (index != 0) {
            return;
        }
        stack.limitSize(getMaxStackSize(stack)); // same clamp as vanilla SimpleContainer
        slot.set(horse, stack.isEmpty() ? ItemStack.EMPTY : stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    /** Re-publishes the stored stack, so in-place changes (e.g. a shrink during shift-click) are synced and hooked. */
    @Override
    public void setChanged() {
        ItemStack stored = slot.get(horse);
        slot.set(horse, stored.isEmpty() ? ItemStack.EMPTY : stored);
    }

    @Override
    public boolean stillValid(Player player) {
        return true; // validity is decided by the menu (distance, horse alive, inventory unchanged)
    }

    @Override
    public void clearContent() {
        slot.set(horse, ItemStack.EMPTY);
    }
}
