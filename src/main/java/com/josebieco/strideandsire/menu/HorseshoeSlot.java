package com.josebieco.strideandsire.menu;

import com.josebieco.strideandsire.attachment.HorseEquipmentSlot;
import com.josebieco.strideandsire.attachment.HorseshoeAccess;
import com.josebieco.strideandsire.item.horseshoe.HorseshoeItem;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The horse's horseshoe slot. It holds at most one item, only accepts {@link HorseshoeItem}s, and is backed by the
 * horse's horseshoe storage: reads go through {@link HorseshoeAccess#get}, every write through {@link HorseshoeAccess#set}.
 */
public class HorseshoeSlot extends Slot {
    public HorseshoeSlot(AbstractHorse horse, int x, int y) {
        super(new HorseEquipmentContainer(horse, HorseEquipmentSlot.HORSESHOE), 0, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.getItem() instanceof HorseshoeItem;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
