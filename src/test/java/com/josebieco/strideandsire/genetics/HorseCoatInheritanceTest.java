package com.josebieco.strideandsire.genetics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

class HorseCoatInheritanceTest {
    private static final Zygosity AA = Zygosity.HOMOZYGOUS_DOMINANT;
    private static final Zygosity Aa = Zygosity.HETEROZYGOUS;
    private static final Zygosity aa = Zygosity.HOMOZYGOUS_RECESSIVE;

    @Test
    void aaDominantCrossedWithAaDominantAlwaysProducesHomozygousDominant() {
        for (double roll : new double[] {0.0, 0.25, 0.50, 0.75, 0.999999}) {
            assertEquals(AA, HorseCoatInheritance.rollChildZygosity(AA, AA, fixed(roll)));
        }
    }

    @Test
    void aaDominantCrossedWithHeterozygousSplitsFiftyFiftyDominantAndHeterozygous() {
        assertEquals(AA, HorseCoatInheritance.rollChildZygosity(AA, Aa, fixed(0.0)));
        assertEquals(AA, HorseCoatInheritance.rollChildZygosity(Aa, AA, fixed(0.499999)));
        assertEquals(Aa, HorseCoatInheritance.rollChildZygosity(AA, Aa, fixed(0.50)));
        assertEquals(Aa, HorseCoatInheritance.rollChildZygosity(Aa, AA, fixed(0.999999)));
    }

    @Test
    void aaDominantCrossedWithHomozygousRecessiveAlwaysProducesHeterozygous() {
        for (double roll : new double[] {0.0, 0.25, 0.50, 0.75, 0.999999}) {
            assertEquals(Aa, HorseCoatInheritance.rollChildZygosity(AA, aa, fixed(roll)));
            assertEquals(Aa, HorseCoatInheritance.rollChildZygosity(aa, AA, fixed(roll)));
        }
    }

    @Test
    void heterozygousCrossedWithHeterozygousUsesOneTwoOneMendelianRatio() {
        assertEquals(AA, HorseCoatInheritance.rollChildZygosity(Aa, Aa, fixed(0.0)));
        assertEquals(AA, HorseCoatInheritance.rollChildZygosity(Aa, Aa, fixed(0.249999)));
        assertEquals(Aa, HorseCoatInheritance.rollChildZygosity(Aa, Aa, fixed(0.25)));
        assertEquals(Aa, HorseCoatInheritance.rollChildZygosity(Aa, Aa, fixed(0.50)));
        assertEquals(Aa, HorseCoatInheritance.rollChildZygosity(Aa, Aa, fixed(0.749999)));
        assertEquals(aa, HorseCoatInheritance.rollChildZygosity(Aa, Aa, fixed(0.75)));
        assertEquals(aa, HorseCoatInheritance.rollChildZygosity(Aa, Aa, fixed(0.999999)));
    }

    @Test
    void heterozygousCrossedWithHomozygousRecessiveSplitsFiftyFiftyHeterozygousAndRecessive() {
        assertEquals(Aa, HorseCoatInheritance.rollChildZygosity(Aa, aa, fixed(0.0)));
        assertEquals(Aa, HorseCoatInheritance.rollChildZygosity(aa, Aa, fixed(0.499999)));
        assertEquals(aa, HorseCoatInheritance.rollChildZygosity(Aa, aa, fixed(0.50)));
        assertEquals(aa, HorseCoatInheritance.rollChildZygosity(aa, Aa, fixed(0.999999)));
    }

    @Test
    void homozygousRecessiveCrossedWithHomozygousRecessiveAlwaysProducesHomozygousRecessive() {
        for (double roll : new double[] {0.0, 0.25, 0.50, 0.75, 0.999999}) {
            assertEquals(aa, HorseCoatInheritance.rollChildZygosity(aa, aa, fixed(roll)));
        }
    }

    @Test
    void pickManifestGeneAlwaysReturnsAMemberOfTheInputSet() {
        for (CoatGene single : CoatGene.values()) {
            Set<CoatGene> singleton = EnumSet.of(single);
            for (double roll : new double[] {0.0, 0.5, 0.999999}) {
                assertEquals(single, HorseCoatInheritance.pickManifestGene(singleton, fixed(roll)));
            }
        }

        Set<CoatGene> pair = Set.of(CoatGene.OBSIDIAN, CoatGene.SILVER_DAPPLE);
        for (double roll : new double[] {0.0, 0.25, 0.499999, 0.50, 0.75, 0.999999}) {
            CoatGene picked = HorseCoatInheritance.pickManifestGene(pair, fixed(roll));
            assertTrue(pair.contains(picked), "Picked gene must be in the candidate set: " + picked);
        }

        assertThrows(
            IllegalArgumentException.class,
            () -> HorseCoatInheritance.pickManifestGene(EnumSet.noneOf(CoatGene.class), fixed(0.5)));
    }

    @Test
    void pickManifestGeneDistributesAcrossAllCandidatesOverManySamples() {
        Set<CoatGene> allGenes = EnumSet.allOf(CoatGene.class);
        Set<CoatGene> observed = EnumSet.noneOf(CoatGene.class);
        RandomGenerator seeded = new SplittableRandom(20261017L);
        for (int i = 0; i < 200; i++) {
            CoatGene picked = HorseCoatInheritance.pickManifestGene(allGenes, seeded);
            assertTrue(allGenes.contains(picked));
            observed.add(picked);
        }
        assertEquals(allGenes, observed);
    }

    private static RandomGenerator fixed(double... values) {
        return AttributeInheritanceTest.fixed(values);
    }
}
