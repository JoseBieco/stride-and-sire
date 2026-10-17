package com.josebieco.strideandsire.genetics;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * Pure inheritance math for horse numeric attributes (spec section 4). No Minecraft dependencies: randomness comes in
 * as a JDK {@link RandomGenerator} (handlers adapt Minecraft's {@code RandomSource}).
 *
 * <p>The average roll mirrors vanilla 26.3's {@code AbstractHorse.createOffspringAttribute}: parents are clamped into
 * the allowed range, {@code margin = 0.15 * (vanillaMax - vanillaMin)}, {@code spread = |p1 - p2| + 2 * margin},
 * {@code child = average + spread * ((r1 + r2 + r3) / 3 - 0.5)}, and a value outside the range is <b>reflected</b>
 * back inside it (not clamped). The only difference from vanilla is the upper bound:
 * <ul>
 *   <li><b>Unguided breeding ({@code dietGuided = false})</b>: {@code [vanillaMin, vanillaMax]} — exactly vanilla,
 *       never reaches Grade A or S.</li>
 *   <li><b>Diet-guided breeding ({@code dietGuided = true})</b>: {@code [vanillaMin, 1.2 * vanillaMax]}. For the
 *       favored attribute only, there is a {@link #BEST_PARENT_CHANCE} (20%) chance to instead anchor on
 *       {@code max(parent1, parent2)} plus an upward bonus in {@code [0, 0.20 * vanillaMax)}, reflected at the same
 *       ceiling.</li>
 * </ul>
 */
public final class AttributeInheritance {
    public static final double BEST_PARENT_CHANCE = 0.20;
    public static final double BEST_PARENT_MAX_BONUS_FRACTION = 0.20;
    /** Vanilla's offspring margin, as a fraction of the vanilla range width. */
    public static final double VARIANCE_MARGIN_FRACTION = 0.15;

    private AttributeInheritance() {}

    /** Rolls one attribute of a foal. {@code favored} is ignored unless {@code dietGuided}. */
    public static double rollChildValue(
            double parent1,
            double parent2,
            double vanillaMin,
            double vanillaMax,
            boolean favored,
            boolean dietGuided,
            RandomGenerator random) {
        if (!(vanillaMax > vanillaMin)) {
            throw new IllegalArgumentException("Invalid vanilla range: min=" + vanillaMin + ", max=" + vanillaMax);
        }
        Objects.requireNonNull(random, "random");
        double maxAllowed = dietGuided ? HorseAttributeRanges.CEILING_FACTOR * vanillaMax : vanillaMax;
        double p1 = Math.clamp(parent1, vanillaMin, maxAllowed);
        double p2 = Math.clamp(parent2, vanillaMin, maxAllowed);
        if (dietGuided && favored && random.nextDouble() < BEST_PARENT_CHANCE) {
            double bonus = random.nextDouble() * BEST_PARENT_MAX_BONUS_FRACTION * vanillaMax;
            return reflectInto(Math.max(p1, p2) + bonus, vanillaMin, maxAllowed);
        }
        double margin = VARIANCE_MARGIN_FRACTION * (vanillaMax - vanillaMin);
        double spread = Math.abs(p1 - p2) + 2.0 * margin;
        double average = (p1 + p2) / 2.0;
        double quality = (random.nextDouble() + random.nextDouble() + random.nextDouble()) / 3.0 - 0.5;
        return reflectInto(average + spread * quality, vanillaMin, maxAllowed);
    }

    public static double rollChildValue(
            double parent1,
            double parent2,
            HorseAttributeRanges range,
            boolean favored,
            boolean dietGuided,
            RandomGenerator random) {
        return rollChildValue(parent1, parent2, range.vanillaMin(), range.vanillaMax(), favored, dietGuided, random);
    }

    /**
     * Rolls all three attributes of a foal, in {@link GeneticAttribute} order. The breeding is diet-guided iff
     * {@code favored} is non-empty; each attribute in {@code favored} independently gets the best-parent chance
     * (the favored set is the union of both parents' diet items).
     */
    public static Map<GeneticAttribute, Double> rollChildValues(
            Map<GeneticAttribute, Double> parentA,
            Map<GeneticAttribute, Double> parentB,
            Set<GeneticAttribute> favored,
            RandomGenerator random) {
        boolean dietGuided = !favored.isEmpty();
        EnumMap<GeneticAttribute, Double> child = new EnumMap<>(GeneticAttribute.class);
        for (GeneticAttribute attr : GeneticAttribute.values()) {
            child.put(attr, rollChildValue(
                parentA.get(attr), parentB.get(attr), HorseAttributeRanges.of(attr),
                favored.contains(attr), dietGuided, random));
        }
        return child;
    }

    /**
     * Vanilla's out-of-range handling: a value above {@code max} becomes {@code max - overshoot}, one below
     * {@code min} becomes {@code min + undershoot}. A final clamp guards the (unreachable with these inputs) case of
     * an overshoot wider than the whole range.
     */
    static double reflectInto(double value, double min, double max) {
        double reflected = value > max ? max - (value - max) : value < min ? min + (min - value) : value;
        return Math.clamp(reflected, min, max);
    }
}
