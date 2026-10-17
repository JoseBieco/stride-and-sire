package com.josebieco.strideandsire.genetics;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * Pure Mendelian inheritance and phenotype tie-breaking for recessive {@link CoatGene}s (Fase D):
 * <ul>
 *   <li>{@code AA x AA}: 100% {@link Zygosity#HOMOZYGOUS_DOMINANT}</li>
 *   <li>{@code AA x Aa}: 50% {@link Zygosity#HOMOZYGOUS_DOMINANT} ({@code [0.00, 0.50)}),
 *       50% {@link Zygosity#HETEROZYGOUS} ({@code [0.50, 1.00)})</li>
 *   <li>{@code AA x aa}: 100% {@link Zygosity#HETEROZYGOUS}</li>
 *   <li>{@code Aa x Aa}: 25% {@link Zygosity#HOMOZYGOUS_DOMINANT} ({@code [0.00, 0.25)}),
 *       50% {@link Zygosity#HETEROZYGOUS} ({@code [0.25, 0.75)}),
 *       25% {@link Zygosity#HOMOZYGOUS_RECESSIVE} ({@code [0.75, 1.00)})</li>
 *   <li>{@code Aa x aa}: 50% {@link Zygosity#HETEROZYGOUS} ({@code [0.00, 0.50)}),
 *       50% {@link Zygosity#HOMOZYGOUS_RECESSIVE} ({@code [0.50, 1.00)})</li>
 *   <li>{@code aa x aa}: 100% {@link Zygosity#HOMOZYGOUS_RECESSIVE}</li>
 * </ul>
 */
public final class HorseCoatInheritance {
    private HorseCoatInheritance() {}

    /**
     * Rolls the foal's {@link Zygosity} for a single coat gene from its parents' zygosities using a single uniform draw
     * in {@code [0.0, 1.0)}.
     */
    public static Zygosity rollChildZygosity(Zygosity parent1, Zygosity parent2, RandomGenerator random) {
        Objects.requireNonNull(parent1, "parent1");
        Objects.requireNonNull(parent2, "parent2");
        Objects.requireNonNull(random, "random");

        double dominantChance;
        double heteroChance;

        if (parent1 == Zygosity.HOMOZYGOUS_DOMINANT && parent2 == Zygosity.HOMOZYGOUS_DOMINANT) {
            dominantChance = 1.0;
            heteroChance = 0.0;
        } else if ((parent1 == Zygosity.HOMOZYGOUS_DOMINANT && parent2 == Zygosity.HETEROZYGOUS)
            || (parent1 == Zygosity.HETEROZYGOUS && parent2 == Zygosity.HOMOZYGOUS_DOMINANT)) {
            dominantChance = 0.50;
            heteroChance = 0.50;
        } else if ((parent1 == Zygosity.HOMOZYGOUS_DOMINANT && parent2 == Zygosity.HOMOZYGOUS_RECESSIVE)
            || (parent1 == Zygosity.HOMOZYGOUS_RECESSIVE && parent2 == Zygosity.HOMOZYGOUS_DOMINANT)) {
            dominantChance = 0.0;
            heteroChance = 1.0;
        } else if (parent1 == Zygosity.HETEROZYGOUS && parent2 == Zygosity.HETEROZYGOUS) {
            dominantChance = 0.25;
            heteroChance = 0.50;
        } else if ((parent1 == Zygosity.HETEROZYGOUS && parent2 == Zygosity.HOMOZYGOUS_RECESSIVE)
            || (parent1 == Zygosity.HOMOZYGOUS_RECESSIVE && parent2 == Zygosity.HETEROZYGOUS)) {
            dominantChance = 0.0;
            heteroChance = 0.50;
        } else {
            dominantChance = 0.0;
            heteroChance = 0.0;
        }

        double roll = random.nextDouble();
        if (roll < dominantChance) {
            return Zygosity.HOMOZYGOUS_DOMINANT;
        }
        if (roll < dominantChance + heteroChance) {
            return Zygosity.HETEROZYGOUS;
        }
        return Zygosity.HOMOZYGOUS_RECESSIVE;
    }

    /**
     * Uniformly selects one {@link CoatGene} from the non-empty set of genes that rolled
     * {@link Zygosity#HOMOZYGOUS_RECESSIVE} on a horse, sorting by enum ordinal first so the draw is deterministic
     * regardless of {@link Set} implementation.
     */
    public static CoatGene pickManifestGene(Set<CoatGene> manifestGenes, RandomGenerator random) {
        Objects.requireNonNull(manifestGenes, "manifestGenes");
        Objects.requireNonNull(random, "random");
        if (manifestGenes.isEmpty()) {
            throw new IllegalArgumentException("manifestGenes must not be empty");
        }

        List<CoatGene> ordered = manifestGenes.stream().sorted().toList();
        int index = Math.min(ordered.size() - 1, (int) (random.nextDouble() * ordered.size()));
        return ordered.get(index);
    }
}
