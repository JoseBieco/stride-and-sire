package com.josebieco.strideandsire.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The 3D bags move clear of a donkey/mule chest instead of being swallowed by it (client.SaddlebagModel). */
class SaddlebagPlacementTest {
    @Test
    void chestMovesBagsUnderTheChestLine() {
        assertEquals(SaddlebagRendering.Placement.FLANK, SaddlebagRendering.placementFor(false));
        assertEquals(SaddlebagRendering.Placement.UNDER_CHEST, SaddlebagRendering.placementFor(true));
    }
}
