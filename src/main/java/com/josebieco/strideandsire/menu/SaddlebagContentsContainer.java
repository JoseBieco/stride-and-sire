package com.josebieco.strideandsire.menu;

import com.josebieco.strideandsire.attachment.SaddlebagAccess;
import com.josebieco.strideandsire.item.saddlebag.SaddlebagItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jspecify.annotations.Nullable;

/**
 * Server-side {@link Container} view over the {@code container} component of the saddlebag currently equipped on a
 * horse (no vanilla helper binds a slot to an item's container component).
 *
 * <p>Always maintains a working copy of all {@link SaddlebagItem#DRAFT_CONTAINER_SIZE} (18) slots so items in slots
 * 1–9 (and 10–18) are preserved intact even when the horse's {@code Draft} trait is gained or lost.
 */
final class SaddlebagContentsContainer implements Container {
    private final AbstractHorse horse;
    private final NonNullList<ItemStack> items =
        NonNullList.withSize(SaddlebagItem.DRAFT_CONTAINER_SIZE, ItemStack.EMPTY);
    private ItemStack bag = ItemStack.EMPTY;
    private @Nullable ItemContainerContents loaded;

    SaddlebagContentsContainer(AbstractHorse horse) {
        this.horse = horse;
    }

    /** Re-binds to the currently equipped bag if it, or its stored contents, changed since the last access. */
    private void refresh() {
        ItemStack current = SaddlebagAccess.get(horse);
        if (SaddlebagItem.maxContainerSize(current) == 0) {
            current = ItemStack.EMPTY;
        }
        ItemContainerContents contents = current.get(DataComponents.CONTAINER);
        if (current == bag && contents == loaded) {
            return;
        }
        bag = current;
        loaded = contents;
        NonNullList<ItemStack> stored = SaddlebagItem.getContents(current, SaddlebagItem.DRAFT_CONTAINER_SIZE);
        for (int i = 0; i < items.size(); i++) {
            items.set(i, i < stored.size() ? stored.get(i) : ItemStack.EMPTY);
        }
    }

    /** Writes the working copy into the equipped bag and republishes it; no-op without a bag. */
    private void write() {
        if (bag.isEmpty()) {
            return;
        }
        SaddlebagItem.setContents(bag, items);
        loaded = bag.get(DataComponents.CONTAINER);
        SaddlebagAccess.set(horse, bag);
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        refresh();
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        refresh();
        return slot >= 0 && slot < items.size() ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        refresh();
        ItemStack taken = ContainerHelper.removeItem(items, slot, count);
        if (!taken.isEmpty()) {
            write();
        }
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        refresh();
        ItemStack taken = ContainerHelper.takeItem(items, slot);
        if (!taken.isEmpty()) {
            write();
        }
        return taken;
    }

    /** Ignored while no bag is equipped (the slots are inactive and reject placement then). */
    @Override
    public void setItem(int slot, ItemStack stack) {
        refresh();
        if (bag.isEmpty() || slot < 0 || slot >= items.size()) {
            return;
        }
        stack.limitSize(getMaxStackSize(stack));
        items.set(slot, stack);
        write();
    }

    @Override
    public void setChanged() {
        refresh();
        write();
    }

    @Override
    public boolean stillValid(Player player) {
        return true; // validity is decided by the menu
    }

    @Override
    public void clearContent() {
        refresh();
        items.replaceAll(stack -> ItemStack.EMPTY);
        write();
    }
}
