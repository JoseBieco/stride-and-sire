package com.josebieco.strideandsire.genetics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

class AttributeInheritanceTest {
    private static final double MIN = 0.0;
    private static final double MAX = 100.0;

    /**
     * (a) dietGuided=false never returns a value above vanillaMax across a wide spread of draws, even when both parents
     * are already at or above vanillaMax (parents are clamped into the vanilla range first, like vanilla).
     */
    @Test
    void unguidedBreedingNeverExceedsVanillaMax() {
        RandomGenerator seeded = new SplittableRandom(123456789L);
        for (int i = 0; i < 10_000; i++) {
            double v1 = AttributeInheritance.rollChildValue(100.0, 100.0, MIN, MAX, false, false, seeded);
            double v2 = AttributeInheritance.rollChildValue(100.0, 100.0, MIN, MAX, true, false, seeded);
            double v3 = AttributeInheritance.rollChildValue(115.0, 118.0, MIN, MAX, true, false, seeded);
            assertTrue(v1 <= MAX, "unguided roll exceeded vanillaMax: " + v1);
            assertTrue(v2 <= MAX, "unguided favored roll exceeded vanillaMax: " + v2);
            assertTrue(v3 <= MAX, "unguided roll from A/S parents exceeded vanillaMax: " + v3);
        }
        // Extreme upward draws reflect at vanillaMax, landing at max - overshoot (never above).
        double extreme = AttributeInheritance.rollChildValue(MAX, MAX, MIN, MAX, true, false, fixed(0.999999));
        assertTrue(extreme <= MAX && extreme > MAX - 15.0, "extreme roll must reflect below max: " + extreme);
    }

    /**
     * (b) The average roll mirrors vanilla 26.3 {@code createOffspringAttribute}: margin is 15% of the range WIDTH, and
     * an overshoot is reflected, not clamped — so unguided foals of max parents essentially never sit exactly on max.
     */
    @Test
    void averageRollMirrorsVanillaFormulaWithReflection() {
        HorseAttributeRanges health = HorseAttributeRanges.HEALTH; // [15, 30]
        // Parents 20 and 28, draws 0.9 x3: quality = 0.4, margin = 0.15 * 15 = 2.25, spread = 8 + 4.5 = 12.5
        // raw = 24 + 5 = 29 (inside the range, returned as is).
        double inside = AttributeInheritance.rollChildValue(20.0, 28.0, health, false, false, fixed(0.9));
        assertEquals(24.0 + 12.5 * 0.4, inside, 1e-9);
        // Parents 30 and 30, draws 0.9: spread = 4.5, raw = 30 + 1.8 = 31.8 -> reflected to 28.2.
        double reflected = AttributeInheritance.rollChildValue(30.0, 30.0, health, false, false, fixed(0.9));
        assertEquals(30.0 - 1.8, reflected, 1e-9);
        // Parents 15 and 15, draws 0.1: raw = 15 - 1.8 -> reflected to 16.8.
        double reflectedLow = AttributeInheritance.rollChildValue(15.0, 15.0, health, false, false, fixed(0.1));
        assertEquals(15.0 + 1.8, reflectedLow, 1e-9);

        RandomGenerator seeded = new SplittableRandom(42L);
        int exactlyMax = 0;
        for (int i = 0; i < 10_000; i++) {
            double v = AttributeInheritance.rollChildValue(30.0, 30.0, health, false, false, seeded);
            assertTrue(v >= 15.0 && v <= 30.0, "out of vanilla range: " + v);
            if (v == 30.0) {
                exactlyMax++;
            }
        }
        assertTrue(exactlyMax < 10, "reflection must not pile foals on vanillaMax; got " + exactlyMax + "/10000");
    }

    /**
     * (c) dietGuided=true, favored=false can exceed vanillaMax via variance alone, but never takes the best-parent
     * anchor path.
     */
    @Test
    void guidedUnfavoredCanExceedVanillaMaxViaVarianceOnlyAndNeverUsesBestParentAnchor() {
        double aboveVanilla = AttributeInheritance.rollChildValue(100.0, 100.0, MIN, MAX, false, true, fixed(0.95));
        assertTrue(aboveVanilla > MAX && aboveVanilla <= 120.0,
            "guided unfavored roll should exceed vanillaMax via variance: " + aboveVanilla);

        // First draw 0.05 WOULD trigger the 20% best-parent branch if favored; unfavored must use the average roll.
        double unfavoredResult = AttributeInheritance.rollChildValue(
            20.0, 100.0, MIN, MAX, false, true, fixed(0.05, 0.50, 0.50));
        double expectedSpread = 80.0 + 2.0 * 15.0; // |20 - 100| + 2 * 0.15 * (100 - 0)
        double expectedQuality = (0.05 + 0.50 + 0.50) / 3.0 - 0.5;
        assertEquals(60.0 + expectedSpread * expectedQuality, unfavoredResult, 1e-9);
        assertTrue(unfavoredResult < 100.0, "unfavored must not anchor on best parent 100.0");
    }

    /**
     * (d) dietGuided=true, favored=true with a draw forced into the 20% branch returns max(parent1, parent2) plus the
     * bonus, reflected at 1.2 x vanillaMax.
     */
    @Test
    void guidedFavoredInTwentyPercentBranchUsesBestParentAnchorPlusBonus() {
        // First draw 0.10 (< 0.20 -> best-parent branch), second draw 0.50 -> bonus = 0.50 * 0.20 * 100 = 10.0.
        double result = AttributeInheritance.rollChildValue(40.0, 95.0, MIN, MAX, true, true, fixed(0.10, 0.50));
        assertEquals(95.0 + 10.0, result, 1e-9);

        // 115 + 0.99 * 20 = 134.8 -> reflected at the 120 ceiling to 105.2.
        double reflected = AttributeInheritance.rollChildValue(110.0, 115.0, MIN, MAX, true, true, fixed(0.0, 0.99));
        assertEquals(120.0 - 14.8, reflected, 1e-9);
        assertTrue(reflected <= 120.0);
    }

    /**
     * (e) Spec section 8: in the same guided breeding, the favored attribute reaches A/S (above vanillaMax) clearly
     * more often than the two non-favored attributes. Parents at 95% of vanilla max (at 100% the non-favored average
     * roll already reaches A often). Expected rates: favored ~17-21%, non-favored ~2-7%; the 8-point margin is >15
     * standard errors at 20k draws, and the seed makes the run deterministic anyway.
     */
    @Test
    void favoredAttributeReachesAOrSMoreOftenThanNonFavored() {
        int draws = 20_000;
        for (GeneticAttribute favoredAttr : GeneticAttribute.values()) {
            RandomGenerator seeded = new SplittableRandom(20261016L + favoredAttr.ordinal());
            Map<GeneticAttribute, Double> parents = new EnumMap<>(GeneticAttribute.class);
            for (GeneticAttribute a : GeneticAttribute.values()) {
                parents.put(a, 0.95 * HorseAttributeRanges.of(a).vanillaMax());
            }
            EnumMap<GeneticAttribute, Integer> aOrS = new EnumMap<>(GeneticAttribute.class);
            for (int i = 0; i < draws; i++) {
                Map<GeneticAttribute, Double> child =
                    AttributeInheritance.rollChildValues(parents, parents, EnumSet.of(favoredAttr), seeded);
                for (GeneticAttribute a : GeneticAttribute.values()) {
                    Grade g = HorseAttributeRanges.of(a).gradeOf(child.get(a));
                    if (g == Grade.A || g == Grade.S) {
                        aOrS.merge(a, 1, Integer::sum);
                    }
                }
            }
            double favoredRate = aOrS.getOrDefault(favoredAttr, 0) / (double) draws;
            for (GeneticAttribute other : GeneticAttribute.values()) {
                if (other == favoredAttr) {
                    continue;
                }
                double otherRate = aOrS.getOrDefault(other, 0) / (double) draws;
                assertTrue(favoredRate > otherRate + 0.08,
                    "favored " + favoredAttr + " A/S rate " + favoredRate + " should clearly exceed " + other + " " + otherRate);
            }
        }
    }

    /**
     * (f) Diet union (controller ruling): a carrot-fed x apple-fed breeding favors both Speed and Health — each gets
     * the best-parent branch independently — while Jump keeps the plain average roll.
     */
    @Test
    void unionOfDietsMakesEveryFavoredAttributeEligibleForBestParent() {
        Map<GeneticAttribute, Double> parents = new EnumMap<>(GeneticAttribute.class);
        for (GeneticAttribute a : GeneticAttribute.values()) {
            parents.put(a, HorseAttributeRanges.of(a).vanillaMax());
        }
        // Every draw 0.10: favored attributes take the best-parent branch (bonus 0.02 x max -> above max);
        // the non-favored one takes the average roll with quality -0.4 (below max).
        Set<GeneticAttribute> union = EnumSet.of(GeneticAttribute.SPEED, GeneticAttribute.HEALTH);
        Map<GeneticAttribute, Double> child = AttributeInheritance.rollChildValues(parents, parents, union, fixed(0.10));
        for (GeneticAttribute a : GeneticAttribute.values()) {
            double max = HorseAttributeRanges.of(a).vanillaMax();
            if (union.contains(a)) {
                assertEquals(max * 1.02, child.get(a), 1e-9, a + " should anchor on best parent");
            } else {
                assertTrue(child.get(a) < max, a + " is not favored and must not anchor: " + child.get(a));
            }
        }
        // A single diet favors only its own attribute.
        Map<GeneticAttribute, Double> carrotOnly = AttributeInheritance.rollChildValues(
            parents, parents, EnumSet.of(GeneticAttribute.SPEED), fixed(0.10));
        assertTrue(carrotOnly.get(GeneticAttribute.HEALTH) < HorseAttributeRanges.HEALTH.vanillaMax());
        // No diet at all is unguided: nothing above vanillaMax even with maximal draws.
        Map<GeneticAttribute, Double> unguided = AttributeInheritance.rollChildValues(
            parents, parents, EnumSet.noneOf(GeneticAttribute.class), fixed(0.999999));
        for (GeneticAttribute a : GeneticAttribute.values()) {
            assertTrue(unguided.get(a) <= HorseAttributeRanges.of(a).vanillaMax());
        }
    }

    /** Returns the given doubles in a cycle from {@code nextDouble()}. */
    static RandomGenerator fixed(double... values) {
        return new RandomGenerator() {
            private int index = 0;

            @Override
            public long nextLong() {
                throw new UnsupportedOperationException("only nextDouble is scripted");
            }

            @Override
            public double nextDouble() {
                return values[index++ % values.length];
            }
        };
    }
}
