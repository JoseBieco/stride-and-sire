package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.attachment.HorseCoatGeneticsAccess;
import com.josebieco.strideandsire.attachment.HorseCoatGeneticsAttachment;
import com.josebieco.strideandsire.command.SetCoatCommand;
import com.josebieco.strideandsire.genetics.CoatGene;
import com.josebieco.strideandsire.genetics.Zygosity;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.Mule;

/**
 * Test instances: data/strideandsire/test_instance/setcoat_command_*.json
 */
public final class SetCoatCommandGameTest {
    private SetCoatCommandGameTest() {}

    /**
     * {@link SetCoatCommand#applyToHorse} sets all three genes on an eligible Horse and picks the single manifest gene
     * as the render gene (no tie to break with only one {@code aa}).
     */
    public static void appliesGenotypeToEligibleHorse(GameTestHelper helper) {
        helper.makeFloor();
        Horse horse = helper.spawn(EntityTypes.HORSE, 2, 1, 2);

        boolean applied = SetCoatCommand.applyToHorse(
            horse, Zygosity.HOMOZYGOUS_RECESSIVE, Zygosity.HETEROZYGOUS, Zygosity.HOMOZYGOUS_DOMINANT, RandomSource.create());
        helper.assertTrue(applied, "applyToHorse must return true for an eligible Horse");

        HorseCoatGeneticsAttachment attachment = HorseCoatGeneticsAccess.get(horse);
        helper.assertValueEqual(attachment.zygosity(CoatGene.OBSIDIAN), Zygosity.HOMOZYGOUS_RECESSIVE, "OBSIDIAN");
        helper.assertValueEqual(attachment.zygosity(CoatGene.ALBINO), Zygosity.HETEROZYGOUS, "ALBINO");
        helper.assertValueEqual(attachment.zygosity(CoatGene.SILVER_DAPPLE), Zygosity.HOMOZYGOUS_DOMINANT, "SILVER_DAPPLE");
        helper.assertValueEqual(attachment.manifestGeneToRender(), CoatGene.OBSIDIAN, "manifestGeneToRender");
        helper.succeed();
    }

    /** {@link SetCoatCommand#applyToHorse} is a no-op on a Mule (not eligible for coat genetics). */
    public static void ignoresIneligibleMule(GameTestHelper helper) {
        helper.makeFloor();
        Mule mule = helper.spawn(EntityTypes.MULE, 2, 1, 2);

        boolean applied = SetCoatCommand.applyToHorse(
            mule, Zygosity.HOMOZYGOUS_RECESSIVE, Zygosity.HOMOZYGOUS_RECESSIVE, Zygosity.HOMOZYGOUS_RECESSIVE, RandomSource.create());
        helper.assertFalse(applied, "applyToHorse must return false for an ineligible Mule");
        helper.assertTrue(HorseCoatGeneticsAccess.get(mule).isEmpty(), "Mule coat genetics must remain EMPTY");
        helper.succeed();
    }
}
