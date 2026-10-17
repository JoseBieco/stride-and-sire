package com.josebieco.strideandsire.genetics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ZygosityTest {
    @Test
    void isCarrierAndIsManifestMatchExpectedStates() {
        assertFalse(Zygosity.HOMOZYGOUS_DOMINANT.isCarrier());
        assertFalse(Zygosity.HOMOZYGOUS_DOMINANT.isManifest());
        assertEquals("homozygous_dominant", Zygosity.HOMOZYGOUS_DOMINANT.getSerializedName());

        assertTrue(Zygosity.HETEROZYGOUS.isCarrier());
        assertFalse(Zygosity.HETEROZYGOUS.isManifest());
        assertEquals("heterozygous", Zygosity.HETEROZYGOUS.getSerializedName());

        assertTrue(Zygosity.HOMOZYGOUS_RECESSIVE.isCarrier());
        assertTrue(Zygosity.HOMOZYGOUS_RECESSIVE.isManifest());
        assertEquals("homozygous_recessive", Zygosity.HOMOZYGOUS_RECESSIVE.getSerializedName());

        assertEquals("obsidian", CoatGene.OBSIDIAN.getSerializedName());
        assertEquals("coatgene.strideandsire.obsidian", CoatGene.OBSIDIAN.translationKey());

        assertEquals("albino", CoatGene.ALBINO.getSerializedName());
        assertEquals("coatgene.strideandsire.albino", CoatGene.ALBINO.translationKey());

        assertEquals("silver_dapple", CoatGene.SILVER_DAPPLE.getSerializedName());
        assertEquals("coatgene.strideandsire.silver_dapple", CoatGene.SILVER_DAPPLE.translationKey());
    }
}
