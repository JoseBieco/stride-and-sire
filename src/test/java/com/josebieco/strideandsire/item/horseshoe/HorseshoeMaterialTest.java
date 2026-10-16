package com.josebieco.strideandsire.item.horseshoe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HorseshoeMaterialTest {

    @Test
    void copperAllowsFeatherStepAndFrostStepNotLavaWalker() {
        assertTrue(HorseshoeMaterial.COPPER.allowsEnchantment(HorseshoeEnchantmentKind.FEATHER_STEP));
        assertTrue(HorseshoeMaterial.COPPER.allowsEnchantment(HorseshoeEnchantmentKind.FROST_STEP));
        assertFalse(HorseshoeMaterial.COPPER.allowsEnchantment(HorseshoeEnchantmentKind.LAVA_WALKER));
    }

    @Test
    void goldAllowsNoExclusiveEnchantmentAndHasGoldPassive() {
        for (HorseshoeEnchantmentKind k : HorseshoeEnchantmentKind.values()) {
            assertFalse(HorseshoeMaterial.GOLD.allowsEnchantment(k));
        }
        assertTrue(HorseshoeMaterial.GOLD.hasGoldPassive());
    }

    @Test
    void netheriteAllowsOnlyLavaWalkerAndFeatherStep() {
        assertTrue(HorseshoeMaterial.NETHERITE.allowsEnchantment(HorseshoeEnchantmentKind.LAVA_WALKER));
        assertTrue(HorseshoeMaterial.NETHERITE.allowsEnchantment(HorseshoeEnchantmentKind.FEATHER_STEP));
        assertFalse(HorseshoeMaterial.NETHERITE.allowsEnchantment(HorseshoeEnchantmentKind.FROST_STEP));
        assertTrue(HorseshoeMaterial.NETHERITE.hasNetheritePassive());
    }

    @Test
    void baseDurabilityMatchesSpec() {
        assertEquals(100, HorseshoeMaterial.COPPER.baseDurability());
        assertEquals(200, HorseshoeMaterial.IRON.baseDurability());
        assertEquals(80, HorseshoeMaterial.GOLD.baseDurability());
        assertEquals(400, HorseshoeMaterial.NETHERITE.baseDurability());
    }
}
