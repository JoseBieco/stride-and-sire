package com.josebieco.strideandsire.item.saddlebag;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaddlebagMaterialTest {

    @Test
    void leatherHasNoBonuses() {
        assertFalse(SaddlebagMaterial.LEATHER.protectsContentsFromExplosion());
        assertFalse(SaddlebagMaterial.LEATHER.protectsContentsFromFire());
        assertFalse(SaddlebagMaterial.LEATHER.hasGoldPiglinPassive());
    }

    @Test
    void ironProtectsFromExplosionOnly() {
        assertTrue(SaddlebagMaterial.IRON.protectsContentsFromExplosion());
        assertFalse(SaddlebagMaterial.IRON.protectsContentsFromFire());
        assertFalse(SaddlebagMaterial.IRON.hasGoldPiglinPassive());
    }

    @Test
    void diamondProtectsFromExplosionOnly() {
        assertEquals(9, SaddlebagMaterial.DIAMOND.slotCount());
        assertTrue(SaddlebagMaterial.DIAMOND.protectsContentsFromExplosion());
        assertFalse(SaddlebagMaterial.DIAMOND.protectsContentsFromFire());
        assertFalse(SaddlebagMaterial.DIAMOND.hasGoldPiglinPassive());
    }

    @Test
    void goldHasPiglinPassiveAndExplosionProtection() {
        assertTrue(SaddlebagMaterial.GOLD.protectsContentsFromExplosion());
        assertTrue(SaddlebagMaterial.GOLD.hasGoldPiglinPassive());
        assertFalse(SaddlebagMaterial.GOLD.protectsContentsFromFire());
    }

    @Test
    void netheriteProtectsFromFireAndExplosion() {
        assertTrue(SaddlebagMaterial.NETHERITE.protectsContentsFromFire());
        assertTrue(SaddlebagMaterial.NETHERITE.protectsContentsFromExplosion());
    }

    @Test
    void everyTierHasNineSlots() {
        for (SaddlebagMaterial m : SaddlebagMaterial.values()) {
            assertEquals(9, m.slotCount());
        }
    }
}
