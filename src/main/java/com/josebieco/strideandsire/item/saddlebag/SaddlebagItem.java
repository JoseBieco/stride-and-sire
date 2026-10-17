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
 * <p>At the data-component level every saddlebag supports up to {@link #DRAFT_CONTAINER_SIZE} (18) slots so a horse
 * with the {@code Draft} lineage trait manifest can use all 18 slots ("Alforje Duplo"), while horses without {@code Draft}
 * expose the first 9 slots ({@link #containerSize(ItemStack)}).
 */
public class SaddlebagItem extends Item {
    /** Total container capacity unlocked when the mounted horse manifests the {@code Draft} lineage trait. */
    public static final int DRAFT_CONTAINER_SIZE = 18;

    private final SaddlebagMaterial material;

    public SaddlebagItem(SaddlebagMaterial material, Item.Properties properties) {
        super(applyMaterial(material, properties));
        this.material = material;
    }

    public SaddlebagMaterial getMaterial() {
        return material;
    }

    /** Base number of container slots of this stack's saddlebag (9, or 0 if the stack is not a saddlebag). */
    public static int containerSize(ItemStack stack) {
        return stack.getItem() instanceof SaddlebagItem bag ? bag.material.slotCount() : 0;
    }

    /**
     * Effective number of addressable container slots of {@code stack} for a horse where {@code draftManifest}
     * indicates whether {@code LineageTrait.DRAFT} is {@code MANIFEST} (18 when manifest, 9 otherwise, 0 for non-bags).
     */
    public static int containerSize(ItemStack stack, boolean draftManifest) {
        if (!(stack.getItem() instanceof SaddlebagItem bag)) {
            return 0;
        }
        return draftManifest ? DRAFT_CONTAINER_SIZE : bag.material.slotCount();
    }

    /** Maximum underlying container capacity for {@code stack} (18 for any saddlebag, 0 otherwise). */
    public static int maxContainerSize(ItemStack stack) {
        return stack.getItem() instanceof SaddlebagItem ? DRAFT_CONTAINER_SIZE : 0;
    }

    /**
     * Copy of the container contents: 9 entries when slots 9–17 are empty, or 18 entries when any Draft slot holds an
     * item (0 entries if not a saddlebag).
     */
    public static NonNullList<ItemStack> getContents(ItemStack stack) {
        int baseSize = containerSize(stack);
        if (baseSize == 0) {
            return NonNullList.withSize(0, ItemStack.EMPTY);
        }
        NonNullList<ItemStack> full = getContents(stack, DRAFT_CONTAINER_SIZE);
        for (int i = baseSize; i < full.size(); i++) {
            if (!full.get(i).isEmpty()) {
                return full;
            }
        }
        NonNullList<ItemStack> base = NonNullList.withSize(baseSize, ItemStack.EMPTY);
        for (int i = 0; i < baseSize; i++) {
            base.set(i, full.get(i));
        }
        return base;
    }

    /** Copy of the container contents sized to {@code slotCount} entries (empties padded). */
    public static NonNullList<ItemStack> getContents(ItemStack stack, int slotCount) {
        int max = maxContainerSize(stack);
        if (max == 0 || slotCount <= 0) {
            return NonNullList.withSize(0, ItemStack.EMPTY);
        }
        int targetSize = Math.min(slotCount, max);
        NonNullList<ItemStack> list = NonNullList.withSize(targetSize, ItemStack.EMPTY);
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents != null) {
            NonNullList<ItemStack> all = NonNullList.withSize(max, ItemStack.EMPTY);
            contents.copyInto(all);
            for (int i = 0; i < targetSize; i++) {
                list.set(i, all.get(i));
            }
        }
        return list;
    }

    /** Replaces the container contents (up to {@link #DRAFT_CONTAINER_SIZE} entries; extra entries are ignored). */
    public static void setContents(ItemStack stack, List<ItemStack> items) {
        int size = maxContainerSize(stack);
        if (size == 0) {
            return;
        }
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
