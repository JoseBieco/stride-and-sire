package com.josebieco.strideandsire.genetics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import org.junit.jupiter.api.Test;

class AttributeInheritanceTest {
    /**
     * (a) dietGuided=false never returns a value above vanillaMax across a wide spread of injected random draws,
     * even when both parents are already at vanillaMax or above it.
     */
    @Test
    void unguidedBreedingNeverExceedsVanillaMax() {
        double vanillaMax = 100.0;
        RandomSource seeded = RandomSource.create(123456789L);
        for (int i = 0; i < 10_000; i++) {
            double v1 = AttributeInheritance.rollChildValue(100.0, 100.0, vanillaMax, false, false, seeded);
            double v2 = AttributeInheritance.rollChildValue(100.0, 100.0, vanillaMax, true, false, seeded);
            double v3 = AttributeInheritance.rollChildValue(115.0, 118.0, vanillaMax, true, false, seeded);
            assertTrue(v1 <= vanillaMax, "unguided roll exceeded vanillaMax: " + v1);
            assertTrue(v2 <= vanillaMax, "unguided favored roll exceeded vanillaMax: " + v2);
            assertTrue(v3 <= vanillaMax, "unguided roll from A/S parents exceeded vanillaMax: " + v3);
            assertEquals(Grade.B, AttributeGrading.gradeOf(Math.max(v1, 80.0), 0.0, vanillaMax));
        }
        // Also test extreme random draws returning 1.0 (maximum upward variance).
        RandomSource alwaysMax = fixedDoubles(1.0, 1.0, 1.0, 1.0);
        double extreme = AttributeInheritance.rollChildValue(vanillaMax, vanillaMax, vanillaMax, true, false, alwaysMax);
        assertEquals(vanillaMax, extreme, 1e-12);
    }

    /**
     * (b) dietGuided=true, favored=false can return above vanillaMax via variance alone, but never invokes the
     * best-parent anchor path.
     */
    @Test
    void guidedUnfavoredCanExceedVanillaMaxViaVarianceOnlyAndNeverUsesBestParentAnchor() {
        double vanillaMax = 100.0;
        // High variance draw with parents at vanillaMax pushes above vanillaMax via average + variance.
        RandomSource highVariance = fixedDoubles(0.95, 0.95, 0.95);
        double aboveVanilla = AttributeInheritance.rollChildValue(100.0, 100.0, vanillaMax, false, true, highVariance);
        assertTrue(aboveVanilla > vanillaMax && aboveVanilla <= 120.0,
            "guided unfavored roll should exceed vanillaMax via variance: " + aboveVanilla);

        // With asymmetric parents (20 and 100) and a draw that WOULD trigger the 20% best-parent branch if favored
        // (first draw 0.05 < 0.20, followed by 0.50, 0.50 -> quality = (0.05 + 0.50 + 0.50)/3 - 0.5 = -0.15),
        // favored=false must still compute the average-based roll around 60, never anchoring on max(20, 100) = 100.
        RandomSource firstDrawInBestParentBand = fixedDoubles(0.05, 0.50, 0.50);
        double unfavoredResult = AttributeInheritance.rollChildValue(
            20.0, 100.0, vanillaMax, false, true, firstDrawInBestParentBand);
        double expectedAverage = 60.0;
        double expectedSpread = 80.0 + 2.0 * 15.0; // |20 - 100| + 2 * 0.15 * 100 = 110
        double expectedQuality = (0.05 + 0.50 + 0.50) / 3.0 - 0.5;
        double expected = expectedAverage + expectedSpread * expectedQuality;
        assertEquals(expected, unfavoredResult, 1e-9);
        assertTrue(unfavoredResult < 100.0, "unfavored must not anchor on best parent 100.0");
    }

    /**
     * (c) dietGuided=true, favored=true with a random draw forced into the 20% branch returns exactly
     * max(parent1, parent2) plus the bonus (clamped to 1.2 * vanillaMax), not the average-based roll.
     */
    @Test
    void guidedFavoredInTwentyPercentBranchUsesBestParentAnchorPlusBonus() {
        double vanillaMax = 100.0;
        // First draw 0.10 (< 0.20 -> best-parent branch), second draw 0.50 -> bonus = 0.50 * 0.20 * 100 = 10.0.
        RandomSource forcedBestParent = fixedDoubles(0.10, 0.50, 0.0, 0.0);
        double result = AttributeInheritance.rollChildValue(40.0, 95.0, vanillaMax, true, true, forcedBestParent);
        assertEquals(95.0 + 10.0, result, 1e-9);

        // Verify clamping at 1.2 * vanillaMax when bestParent + bonus exceeds 120%.
        RandomSource maxBonus = fixedDoubles(0.0, 1.0);
        double clamped = AttributeInheritance.rollChildValue(110.0, 115.0, vanillaMax, true, true, maxBonus);
        assertEquals(120.0, clamped, 1e-9);
    }

    private static RandomSource fixedDoubles(double... values) {
        return new LegacyRandomSource(0L) {
            private int index = 0;

            @Override
            public double nextDouble() {
                double v = values[index % values.length];
                index++;
                return v;
            }
        };
    }
}
