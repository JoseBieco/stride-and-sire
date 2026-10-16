package com.josebieco.strideandsire.item.saddlebag;

import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * A saddlebag of a given {@link SaddlebagMaterial}. Stack size 1, no durability, not enchantable; its contents live in
 * the vanilla {@code container} data component (present, empty, on every fresh stack). Netherite is fire-resistant as
 * an item. The component tooltip lists contained items, shulker-box style.
 */
public class SaddlebagItem extends Item {
    private final SaddlebagMaterial material;

    public SaddlebagItem(SaddlebagMaterial material, Item.Properties properties) {
        super(applyMaterial(material, properties));
        this.material = material;
    }

    public SaddlebagMaterial getMaterial() {
        return material;
    }

    /** Number of container slots of this stack's saddlebag (0 if the stack is not a saddlebag). */
    public static int containerSize(ItemStack stack) {
        return stack.getItem() instanceof SaddlebagItem bag ? bag.material.slotCount() : 0;
    }

    /** Copy of the container contents, always exactly {@link #containerSize} entries (empties padded). */
    public static NonNullList<ItemStack> getContents(ItemStack stack) {
        NonNullList<ItemStack> list = NonNullList.withSize(containerSize(stack), ItemStack.EMPTY);
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents != null) {
            contents.copyInto(list);
        }
        return list;
    }

    /** Replaces the container contents (list may be shorter than the slot count; extra entries are ignored). */
    public static void setContents(ItemStack stack, List<ItemStack> items) {
        int size = containerSize(stack);
        List<ItemStack> bounded = items.size() > size ? items.subList(0, size) : items;
        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(bounded));
    }

    private static Item.Properties applyMaterial(SaddlebagMaterial material, Item.Properties properties) {
        properties.stacksTo(1).component(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        if (material.protectsContentsFromFire()) {
            properties.fireResistant();
        }
        return properties;
    }
}
