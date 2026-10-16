package com.josebieco.strideandsire.menu;

import com.josebieco.strideandsire.attachment.HorseshoeAccess;
import com.josebieco.strideandsire.item.horseshoe.HorseshoeItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The horse's horseshoe slot. It holds at most one item, only accepts {@link HorseshoeItem}s, and is backed by the
 * horse's horseshoe storage: reads go through {@link HorseshoeAccess#get}, every write through {@link HorseshoeAccess#set}.
 */
public class HorseshoeSlot extends Slot {
    public HorseshoeSlot(AbstractHorse horse, int x, int y) {
        super(new StorageContainer(horse), 0, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.getItem() instanceof HorseshoeItem;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    /** One-slot {@link Container} view over {@link HorseshoeAccess}; it stores nothing itself. */
    private static final class StorageContainer implements Container {
        private final AbstractHorse horse;

        StorageContainer(AbstractHorse horse) {
            this.horse = horse;
        }

        @Override
        public int getContainerSize() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return HorseshoeAccess.get(horse).isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return slot == 0 ? HorseshoeAccess.get(horse) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int slot, int count) {
            ItemStack stored = getItem(slot);
            if (stored.isEmpty() || count <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack taken = stored.split(count);
            HorseshoeAccess.set(horse, stored.isEmpty() ? ItemStack.EMPTY : stored);
            return taken;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack stored = getItem(slot);
            if (stored.isEmpty()) {
                return ItemStack.EMPTY;
            }
            HorseshoeAccess.set(horse, ItemStack.EMPTY);
            return stored;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            if (slot != 0) {
                return;
            }
            stack.limitSize(getMaxStackSize(stack)); // same clamp as vanilla SimpleContainer
            HorseshoeAccess.set(horse, stack.isEmpty() ? ItemStack.EMPTY : stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        /** Re-publishes the stored stack, so in-place changes (e.g. a shrink during shift-click) are synced and hooked. */
        @Override
        public void setChanged() {
            ItemStack stored = HorseshoeAccess.get(horse);
            HorseshoeAccess.set(horse, stored.isEmpty() ? ItemStack.EMPTY : stored);
        }

        @Override
        public boolean stillValid(Player player) {
            return true; // validity is decided by the menu (distance, horse alive, inventory unchanged)
        }

        @Override
        public void clearContent() {
            HorseshoeAccess.set(horse, ItemStack.EMPTY);
        }
    }
}
