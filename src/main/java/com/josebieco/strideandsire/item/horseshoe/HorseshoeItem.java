package com.josebieco.strideandsire.item.horseshoe;

import com.josebieco.strideandsire.enchantment.HorseshoeEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * A horseshoe of a given {@link HorseshoeMaterial}. Max damage comes from
 * {@link HorseshoeMaterial#baseDurability()} (which also forces a stack size of 1),
 * horseshoes are enchantable, and Netherite horseshoes are fire-resistant as items.
 */
public class HorseshoeItem extends Item {
    private final HorseshoeMaterial material;

    public HorseshoeItem(HorseshoeMaterial material, Item.Properties properties) {
        super(applyMaterial(material, properties));
        this.material = material;
    }

    public HorseshoeMaterial getMaterial() {
        return material;
    }

    /**
     * Enchanting-table gate: vanilla's {@code primary_items} check, AND the horseshoe eligibility matrix for the three
     * horseshoe enchantments (the anvil path is enforced by the enchantments' {@code supported_items} tags).
     */
    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Holder<Enchantment> enchantment) {
        return super.canApplyAtEnchantingTable(stack, enchantment) && HorseshoeEnchantments.allowsOnHorseshoe(stack, enchantment);
    }

    private static Item.Properties applyMaterial(HorseshoeMaterial material, Item.Properties properties) {
        properties.durability(material.baseDurability()).enchantable(enchantability(material));
        if (material == HorseshoeMaterial.NETHERITE) {
            properties.fireResistant();
        }
        return properties;
    }

    /** Enchanting-table enchantability, mirroring the vanilla armor material of the same metal. */
    static int enchantability(HorseshoeMaterial material) {
        return switch (material) {
            case COPPER -> 8;
            case IRON -> 9;
            case DIAMOND -> 10;
            case GOLD -> 25;
            case NETHERITE -> 15;
        };
    }
}
