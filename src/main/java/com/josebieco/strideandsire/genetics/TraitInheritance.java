package com.josebieco.strideandsire.genetics;

import java.util.Objects;
import net.minecraft.util.RandomSource;

/**
 * Pure inheritance math for horse {@link LineageTrait}s (spec section 5):
 * <ul>
 *   <li>Neither parent carries the trait ({@code ABSENT x ABSENT}): 2% {@link TraitState#LATENT}
 *       ({@code [0.00, 0.02)}), 1% {@link TraitState#MANIFEST} ({@code [0.02, 0.03)}), 97% {@link TraitState#ABSENT}.</li>
 *   <li>At least one parent carries the trait ({@code LATENT} or {@code MANIFEST}, including when both parents carry):
 *       35% {@link TraitState#LATENT} ({@code [0.00, 0.35)}), 15% {@link TraitState#MANIFEST} ({@code [0.35, 0.50)}),
 *       50% {@link TraitState#ABSENT}.</li>
 * </ul>
 */
public final class TraitInheritance {
    public static final double MUTATION_LATENT_CHANCE = 0.02;
    public static final double MUTATION_MANIFEST_CHANCE = 0.01;
    public static final double CARRIER_LATENT_CHANCE = 0.35;
    public static final double CARRIER_MANIFEST_CHANCE = 0.15;

    private TraitInheritance() {}

    public static TraitState rollChildState(TraitState parent1, TraitState parent2, RandomSource random) {
        Objects.requireNonNull(parent1, "parent1");
        Objects.requireNonNull(parent2, "parent2");
        Objects.requireNonNull(random, "random");

        boolean anyParentCarries = parent1.isCarrier() || parent2.isCarrier();
        double latentChance = anyParentCarries ? CARRIER_LATENT_CHANCE : MUTATION_LATENT_CHANCE;
        double manifestChance = anyParentCarries ? CARRIER_MANIFEST_CHANCE : MUTATION_MANIFEST_CHANCE;

        double roll = random.nextDouble();
        if (roll < latentChance) {
            return TraitState.LATENT;
        }
        if (roll < latentChance + manifestChance) {
            return TraitState.MANIFEST;
        }
        return TraitState.ABSENT;
    }
}
