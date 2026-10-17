package com.josebieco.strideandsire.genetics;

import net.minecraft.util.RandomSource;

/**
 * Pure inheritance math for horse numeric attributes (spec section 4).
 *
 * <ul>
 *   <li><b>Unguided breeding ({@code dietGuided = false})</b>: average of parents plus random variance, clamped to
 *       {@code [0, vanillaMax]} — never reaches Grade A or S.</li>
 *   <li><b>Diet-guided breeding ({@code dietGuided = true})</b>: ceiling expands to {@code 1.2 * vanillaMax}. For
 *       non-favored attributes ({@code favored = false}), only the average + variance roll applies. For the favored
 *       attribute ({@code favored = true}), there is a {@link #BEST_PARENT_CHANCE} (20%) chance to anchor on
 *       {@code Math.max(parent1, parent2)} plus an upward bonus in {@code [0, 0.20 * vanillaMax]}, clamped to
 *       {@code 1.2 * vanillaMax}.</li>
 * </ul>
 */
public final class AttributeInheritance {
    public static final double BEST_PARENT_CHANCE = 0.20;
    public static final double BEST_PARENT_MAX_BONUS_FRACTION = 0.20;
    public static final double VARIANCE_MARGIN_FRACTION = 0.15;

    private AttributeInheritance() {}

    public static double rollChildValue(
            double parent1,
            double parent2,
            double vanillaMax,
            boolean favored,
            boolean dietGuided,
            RandomSource random) {
        if (!(vanillaMax > 0.0)) {
            throw new IllegalArgumentException("vanillaMax must be positive, got " + vanillaMax);
        }
        double maxAllowed = dietGuided ? HorseAttributeRanges.CEILING_FACTOR * vanillaMax : vanillaMax;
        if (dietGuided && favored && random.nextDouble() < BEST_PARENT_CHANCE) {
            double bestParent = Math.max(parent1, parent2);
            double bonus = random.nextDouble() * BEST_PARENT_MAX_BONUS_FRACTION * vanillaMax;
            return Math.clamp(bestParent + bonus, 0.0, maxAllowed);
        }
        double average = (parent1 + parent2) / 2.0;
        double margin = VARIANCE_MARGIN_FRACTION * vanillaMax;
        double spread = Math.abs(parent1 - parent2) + 2.0 * margin;
        double quality = (random.nextDouble() + random.nextDouble() + random.nextDouble()) / 3.0 - 0.5;
        double raw = average + spread * quality;
        return Math.clamp(raw, 0.0, maxAllowed);
    }

    public static double rollChildValue(
            double parent1,
            double parent2,
            HorseAttributeRanges range,
            boolean favored,
            boolean dietGuided,
            RandomSource random) {
        double rolled = rollChildValue(parent1, parent2, range.vanillaMax(), favored, dietGuided, random);
        return Math.max(range.vanillaMin(), rolled);
    }
}
