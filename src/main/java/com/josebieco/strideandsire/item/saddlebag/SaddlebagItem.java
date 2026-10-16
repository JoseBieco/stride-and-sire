package com.josebieco.strideandsire.item.saddlebag;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * A saddlebag of a given {@link SaddlebagMaterial}. Stack size 1, no durability, not enchantable; its contents live in
 * the vanilla {@code container} data component (present, empty, on every fresh stack). Netherite is fire-resistant as
 * an item. The component tooltip lists contained items, shulker-box style; one extra line describes the tier bonus.
 *
 * <p>Like shulker boxes, a saddlebag never fits inside another container item (bundles, shulker boxes, other bags), so
 * nesting depth stays bounded; and a destroyed saddlebag item entity spills its contents instead of deleting them.
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

    /** Bags hold a container, so they may not go inside container items (bundle / shulker box): no unbounded nesting. */
    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    /** Destroyed as an item entity (lava, fire, cactus, explosion...): spill the contents, like bundles/shulker boxes. */
    @Override
    public void onDestroyed(ItemEntity entity) {
        ItemContainerContents contents = entity.getItem().set(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        if (contents != null) {
            ItemUtils.onContainerDestroyed(entity, contents.nonEmptyItemCopyStream());
        }
    }

    /** Translation key of this tier's bonus tooltip line ({@code item.strideandsire.saddlebag.bonus.<tier>}). */
    public String bonusTooltipKey() {
        return "item.strideandsire.saddlebag.bonus." + material.name().toLowerCase(Locale.ROOT);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable(bonusTooltipKey()).withStyle(ChatFormatting.GRAY));
    }

    private static Item.Properties applyMaterial(SaddlebagMaterial material, Item.Properties properties) {
        properties.stacksTo(1).component(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        if (material.protectsContentsFromFire()) {
            properties.fireResistant();
        }
        return properties;
    }
}
