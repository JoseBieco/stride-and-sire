package com.josebieco.strideandsire.menu;

import com.josebieco.strideandsire.attachment.SaddlebagAccess;
import com.josebieco.strideandsire.genetics.LineageTrait;
import com.josebieco.strideandsire.item.saddlebag.SaddlebagItem;
import java.util.function.BooleanSupplier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * One of the equipped saddlebag's container slots. Active (shown, clickable, placeable, pickable) only while the horse
 * has a saddlebag whose effective capacity (9 by default, 18 when {@link LineageTrait#DRAFT} is {@code MANIFEST}, as
 * reported by {@code draftManifest}) is greater than {@code containerSlot}. Never accepts a saddlebag, or any item that
 * may not go inside a container item (shulker boxes), so bags never nest.
 */
public class SaddlebagContentSlot extends Slot {
    private final AbstractHorse horse;
    private final BooleanSupplier draftManifest;

    public SaddlebagContentSlot(Container container, int containerSlot, AbstractHorse horse, BooleanSupplier draftManifest,
            int x, int y) {
        super(container, containerSlot, x, y);
        this.horse = horse;
        this.draftManifest = draftManifest;
    }

    /** True if {@code stack} may be stored inside a saddlebag. */
    public static boolean canStore(ItemStack stack) {
        return !(stack.getItem() instanceof SaddlebagItem) && stack.getItem().canFitInsideContainerItems();
    }

    @Override
    public boolean isActive() {
        return SaddlebagItem.containerSize(SaddlebagAccess.get(horse), draftManifest.getAsBoolean()) > this.getContainerSlot();
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
