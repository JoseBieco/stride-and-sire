package com.josebieco.strideandsire.genetics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import org.junit.jupiter.api.Test;

class TraitInheritanceTest {
    /**
     * (a) Both parents ABSENT: draw in 2% band [0.00, 0.02) returns LATENT; in 1% band [0.02, 0.03) returns MANIFEST;
     * otherwise ABSENT.
     */
    @Test
    void bothParentsAbsentUsesSpontaneousMutationTable() {
        assertEquals(
            TraitState.LATENT,
            TraitInheritance.rollChildState(TraitState.ABSENT, TraitState.ABSENT, fixedDouble(0.01)));
        assertEquals(
            TraitState.MANIFEST,
            TraitInheritance.rollChildState(TraitState.ABSENT, TraitState.ABSENT, fixedDouble(0.025)));
        assertEquals(
            TraitState.ABSENT,
            TraitInheritance.rollChildState(TraitState.ABSENT, TraitState.ABSENT, fixedDouble(0.03)));
        assertEquals(
            TraitState.ABSENT,
            TraitInheritance.rollChildState(TraitState.ABSENT, TraitState.ABSENT, fixedDouble(0.50)));
    }

    /**
     * (b) One parent LATENT (the other ABSENT): draw in 35% band [0.00, 0.35) returns LATENT; in 15% band [0.35, 0.50)
     * returns MANIFEST; otherwise ABSENT.
     */
    @Test
    void oneParentLatentUsesCarrierTable() {
        assertEquals(
            TraitState.LATENT,
            TraitInheritance.rollChildState(TraitState.LATENT, TraitState.ABSENT, fixedDouble(0.20)));
        assertEquals(
            TraitState.LATENT,
            TraitInheritance.rollChildState(TraitState.ABSENT, TraitState.LATENT, fixedDouble(0.34)));
        assertEquals(
            TraitState.MANIFEST,
            TraitInheritance.rollChildState(TraitState.LATENT, TraitState.ABSENT, fixedDouble(0.40)));
        assertEquals(
            TraitState.ABSENT,
            TraitInheritance.rollChildState(TraitState.LATENT, TraitState.ABSENT, fixedDouble(0.50)));
    }

    /**
     * (c) Both parents MANIFEST uses the exact same 35%/15% thresholds as one parent (Review Focus item 3).
     */
    @Test
    void bothParentsManifestUsesSameThresholdsAsSingleCarrier() {
        for (double draw : new double[] {0.0, 0.10, 0.3499, 0.35, 0.42, 0.4999, 0.50, 0.51, 0.75, 0.99}) {
            TraitState singleCarrier = TraitInheritance.rollChildState(
                TraitState.MANIFEST, TraitState.ABSENT, fixedDouble(draw));
            TraitState dualCarrier = TraitInheritance.rollChildState(
                TraitState.MANIFEST, TraitState.MANIFEST, fixedDouble(draw));
            assertEquals(singleCarrier, dualCarrier, "Mismatch at draw=" + draw);
        }
        // Specifically at 0.50 (just outside 35% + 15% = 50%), dual-MANIFEST must still return ABSENT.
        assertEquals(
            TraitState.ABSENT,
            TraitInheritance.rollChildState(TraitState.MANIFEST, TraitState.MANIFEST, fixedDouble(0.50)));
    }

    private static RandomSource fixedDouble(double value) {
        return new LegacyRandomSource(0L) {
            @Override
            public double nextDouble() {
                return value;
            }
        };
    }
}
