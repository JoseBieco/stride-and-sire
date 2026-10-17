package com.josebieco.strideandsire.menu;

import com.josebieco.strideandsire.attachment.HorseEquipmentSlot;
import com.josebieco.strideandsire.attachment.SaddlebagAccess;
import com.josebieco.strideandsire.item.saddlebag.SaddlebagItem;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The horse's saddlebag slot. It holds at most one item and only accepts a {@link SaddlebagItem} while the horse has a
 * saddle equipped ({@code Mob.isSaddled}); taking the bag out is always allowed. Backed by the horse's saddlebag
 * storage: reads go through {@link SaddlebagAccess#get}, every write through {@link SaddlebagAccess#set}. The bag's
 * contents travel inside the stack (its {@code container} component), so moving the bag moves its contents.
 */
public class SaddlebagSlot extends Slot {
    /** Silhouette sprite (GUI atlas) drawn while the slot is empty. */
    public static final Identifier EMPTY_ICON = Identifier.fromNamespaceAndPath("strideandsire", "container/slot/saddlebag");

    private final AbstractHorse horse;

    public SaddlebagSlot(AbstractHorse horse, int x, int y) {
        super(new HorseEquipmentContainer(horse, HorseEquipmentSlot.SADDLEBAG), 0, x, y);
        this.horse = horse;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.getItem() instanceof SaddlebagItem && horse.isSaddled();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public Identifier getNoItemIcon() {
        return EMPTY_ICON;
    }
}
