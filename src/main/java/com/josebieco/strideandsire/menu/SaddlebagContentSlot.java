package com.josebieco.strideandsire.menu;

import com.josebieco.strideandsire.attachment.HorseGeneticsAccess;
import com.josebieco.strideandsire.attachment.SaddlebagAccess;
import com.josebieco.strideandsire.genetics.LineageTrait;
import com.josebieco.strideandsire.item.saddlebag.SaddlebagItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * One of the equipped saddlebag's container slots. Active (shown, clickable, placeable, pickable) only while the horse
 * has a saddlebag whose effective capacity (9 by default, 18 when {@link LineageTrait#DRAFT} is {@code MANIFEST}) is
 * greater than {@code containerSlot}. Never accepts a saddlebag, or any item that may not go inside a container item
 * (shulker boxes), so bags never nest.
 */
public class SaddlebagContentSlot extends Slot {
    private final AbstractHorse horse;

    public SaddlebagContentSlot(Container container, int containerSlot, AbstractHorse horse, int x, int y) {
        super(container, containerSlot, x, y);
        this.horse = horse;
    }

    /** True if {@code stack} may be stored inside a saddlebag. */
    public static boolean canStore(ItemStack stack) {
        return !(stack.getItem() instanceof SaddlebagItem) && stack.getItem().canFitInsideContainerItems();
    }

    @Override
    public boolean isActive() {
        boolean draftManifest = HorseGeneticsAccess.hasManifest(horse, LineageTrait.DRAFT);
        return SaddlebagItem.containerSize(SaddlebagAccess.get(horse), draftManifest) > this.getContainerSlot();
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return isActive() && canStore(stack);
    }

    @Override
    public boolean mayPickup(Player player) {
        return isActive() && super.mayPickup(player);
    }
}
