package com.josebieco.strideandsire.item.horseshoe;

import net.minecraft.world.item.Item;

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
            case GOLD -> 25;
            case NETHERITE -> 15;
        };
    }
}
