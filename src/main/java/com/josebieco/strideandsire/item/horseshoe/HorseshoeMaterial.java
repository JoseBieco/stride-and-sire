package com.josebieco.strideandsire.item.horseshoe;

import java.util.EnumSet;
import java.util.Set;

/**
 * Horseshoe material tiers. Pure Java: no Minecraft imports, so it stays unit-testable.
 */
public enum HorseshoeMaterial {
    COPPER(100, EnumSet.of(HorseshoeEnchantmentKind.FEATHER_STEP, HorseshoeEnchantmentKind.FROST_STEP), false, false),
    IRON(200, EnumSet.of(HorseshoeEnchantmentKind.FEATHER_STEP, HorseshoeEnchantmentKind.FROST_STEP), false, false),
    GOLD(80, EnumSet.noneOf(HorseshoeEnchantmentKind.class), true, false),
    NETHERITE(400, EnumSet.of(HorseshoeEnchantmentKind.FEATHER_STEP, HorseshoeEnchantmentKind.LAVA_WALKER), false, true);

    private final int baseDurability;
    private final Set<HorseshoeEnchantmentKind> allowed;
    private final boolean goldPassive;
    private final boolean netheritePassive;

    HorseshoeMaterial(int baseDurability,
                      Set<HorseshoeEnchantmentKind> allowed,
                      boolean goldPassive,
                      boolean netheritePassive) {
        this.baseDurability = baseDurability;
        this.allowed = allowed;
        this.goldPassive = goldPassive;
        this.netheritePassive = netheritePassive;
    }

    /** Base durability before Unbreaking. */
    public int baseDurability() {
        return baseDurability;
    }

    /** Whether this material may carry the given enchantment. */
    public boolean allowsEnchantment(HorseshoeEnchantmentKind kind) {
        return allowed.contains(kind);
    }

    /** Always-on gold passive (movement speed boost, Piglin neutrality while mounted). */
    public boolean hasGoldPassive() {
        return goldPassive;
    }

    /** Always-on netherite passive (full fire/lava damage immunity). */
    public boolean hasNetheritePassive() {
        return netheritePassive;
    }
}
