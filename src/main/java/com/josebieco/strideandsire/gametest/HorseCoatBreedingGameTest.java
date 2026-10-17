package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.attachment.HorseCoatGeneticsAccess;
import com.josebieco.strideandsire.attachment.HorseCoatGeneticsAttachment;
import com.josebieco.strideandsire.genetics.CoatGene;
import com.josebieco.strideandsire.genetics.Zygosity;
import java.util.List;
import java.util.Map;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * Test instances:
 * <ul>
 *   <li>data/strideandsire/test_instance/coat_genetics_breeding_aa_x_aa_produces_manifest_statistically.json</li>
 *   <li>data/strideandsire/test_instance/coat_genetics_tie_break_persists_across_reload.json</li>
 * </ul>
 */
public final class HorseCoatBreedingGameTest {
    /** Aa x Aa trials: P(no aa foal) = 0.75^60 ~= 3.2e-8 (< 1e-6 budget). */
    private static final int CARRIER_TRIALS = 60;

    private HorseCoatBreedingGameTest() {}

    /**
     * Verifies both:
     * <ol>
     *   <li>{@code aa x aa} on {@link CoatGene#OBSIDIAN} deterministically produces a foal with
     *       {@link Zygosity#HOMOZYGOUS_RECESSIVE} on {@link CoatGene#OBSIDIAN} and
     *       {@code manifestGeneToRender() == CoatGene.OBSIDIAN}.</li>
     *   <li>{@code Aa x Aa} on {@link CoatGene#OBSIDIAN} across 60 trials produces at least one
     *       {@link Zygosity#HOMOZYGOUS_RECESSIVE} foal with {@code manifestGeneToRender() == CoatGene.OBSIDIAN}.</li>
     * </ol>
     */
    public static void aaByAaProducesManifestStatistically(GameTestHelper helper) {
        helper.makeFloor();
        ServerLevel level = helper.getLevel();

        // Part 1: aa x aa on OBSIDIAN -> 100% HOMOZYGOUS_RECESSIVE and manifestGeneToRender == OBSIDIAN.
        Horse recessiveA = helper.spawn(EntityTypes.HORSE, 1, 1, 1);
        Horse recessiveB = helper.spawn(EntityTypes.HORSE, 2, 1, 1);
        recessiveA.setTamed(true);
        recessiveB.setTamed(true);
        HorseCoatGeneticsAttachment obsidianManifest = HorseCoatGeneticsAttachment.EMPTY.withGenes(
            Map.of(CoatGene.OBSIDIAN, Zygosity.HOMOZYGOUS_RECESSIVE),
            CoatGene.OBSIDIAN);
        HorseCoatGeneticsAccess.set(recessiveA, obsidianManifest);
        HorseCoatGeneticsAccess.set(recessiveB, obsidianManifest);

        recessiveA.setAge(0);
        recessiveB.setAge(0);
        recessiveA.setInLove(null);
        recessiveB.setInLove(null);
        recessiveA.spawnChildFromBreeding(level, recessiveB);
        Horse pureFoal = findNewestBaby(helper);
        helper.assertTrue(pureFoal != null, "aa x aa breeding should spawn a foal");

        HorseCoatGeneticsAttachment pureFoalCoat = HorseCoatGeneticsAccess.get(pureFoal);
        helper.assertValueEqual(
            pureFoalCoat.zygosity(CoatGene.OBSIDIAN),
            Zygosity.HOMOZYGOUS_RECESSIVE,
            "aa x aa foal must be HOMOZYGOUS_RECESSIVE on OBSIDIAN");
        helper.assertValueEqual(
            pureFoalCoat.manifestGeneToRender(),
            CoatGene.OBSIDIAN,
            "aa x aa foal must render OBSIDIAN");
        pureFoal.discard();

        // Part 2: Aa x Aa on OBSIDIAN across 60 trials -> at least one HOMOZYGOUS_RECESSIVE foal (false failure 0.75^60 ~= 3.2e-8).
        Horse carrierA = helper.spawn(EntityTypes.HORSE, 3, 1, 1);
        Horse carrierB = helper.spawn(EntityTypes.HORSE, 3, 1, 2);
        carrierA.setTamed(true);
        carrierB.setTamed(true);
        HorseCoatGeneticsAttachment obsidianCarrier = HorseCoatGeneticsAttachment.EMPTY.withGenes(
            Map.of(CoatGene.OBSIDIAN, Zygosity.HETEROZYGOUS),
            null);
        HorseCoatGeneticsAccess.set(carrierA, obsidianCarrier);
        HorseCoatGeneticsAccess.set(carrierB, obsidianCarrier);

        int manifestCount = 0;
        for (int i = 0; i < CARRIER_TRIALS; i++) {
            carrierA.setAge(0);
            carrierB.setAge(0);
            carrierA.setInLove(null);
            carrierB.setInLove(null);
            carrierA.spawnChildFromBreeding(level, carrierB);
            Horse foal = findNewestBaby(helper);
            helper.assertTrue(foal != null, "Aa x Aa breeding trial " + i + " should spawn a foal");

            HorseCoatGeneticsAttachment foalCoat = HorseCoatGeneticsAccess.get(foal);
            if (foalCoat.isManifest(CoatGene.OBSIDIAN)) {
                helper.assertValueEqual(
                    foalCoat.manifestGeneToRender(),
                    CoatGene.OBSIDIAN,
                    "Manifest OBSIDIAN foal must have manifestGeneToRender == OBSIDIAN");
                manifestCount++;
            }
            foal.discard();
        }

        helper.assertTrue(
            manifestCount > 0,
            "Expected at least one HOMOZYGOUS_RECESSIVE foal across " + CARRIER_TRIALS + " Aa x Aa trials, got 0");
        helper.succeed();
    }

    /**
     * Verifies that when a foal is born {@link Zygosity#HOMOZYGOUS_RECESSIVE} on two genes at once
     * ({@link CoatGene#OBSIDIAN} and {@link CoatGene#ALBINO}), the tie-break choice in
     * {@link HorseCoatGeneticsAttachment#manifestGeneToRender()} is chosen at birth and persists identically across NBT
     * save/reload without re-rolling.
     */
    public static void tieBreakPersistsAcrossReload(GameTestHelper helper) {
        helper.makeFloor();
        ServerLevel level = helper.getLevel();

        // Breed two parents that are HOMOZYGOUS_RECESSIVE on both OBSIDIAN and ALBINO.
        Horse parentA = helper.spawn(EntityTypes.HORSE, 1, 1, 1);
        Horse parentB = helper.spawn(EntityTypes.HORSE, 2, 1, 1);
        parentA.setTamed(true);
        parentB.setTamed(true);
        HorseCoatGeneticsAttachment dualRecessive = HorseCoatGeneticsAttachment.EMPTY.withGenes(
            Map.of(
                CoatGene.OBSIDIAN, Zygosity.HOMOZYGOUS_RECESSIVE,
                CoatGene.ALBINO, Zygosity.HOMOZYGOUS_RECESSIVE),
            CoatGene.OBSIDIAN);
        HorseCoatGeneticsAccess.set(parentA, dualRecessive);
        HorseCoatGeneticsAccess.set(parentB, dualRecessive);

        parentA.setAge(0);
        parentB.setAge(0);
        parentA.setInLove(null);
        parentB.setInLove(null);
        parentA.spawnChildFromBreeding(level, parentB);
        Horse foal = findNewestBaby(helper);
        helper.assertTrue(foal != null, "dual-recessive breeding should spawn a foal");

        HorseCoatGeneticsAttachment foalCoat = HorseCoatGeneticsAccess.get(foal);
        helper.assertTrue(foalCoat.isManifest(CoatGene.OBSIDIAN), "foal must be manifest on OBSIDIAN");
        helper.assertTrue(foalCoat.isManifest(CoatGene.ALBINO), "foal must be manifest on ALBINO");
        CoatGene birthChoice = foalCoat.manifestGeneToRender();
        helper.assertTrue(
            birthChoice == CoatGene.OBSIDIAN || birthChoice == CoatGene.ALBINO,
            "birth tie-break must pick one of the manifest genes, got " + birthChoice);

        // Save and reload the foal, verifying the birth tie-break choice is preserved.
        TagValueOutput foalOut = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        foal.saveWithoutId(foalOut);
        CompoundTag foalTag = foalOut.buildResult();
        foal.discard();

        Horse reloadedFoal = EntityTypes.HORSE.create(level, EntitySpawnReason.LOAD);
        helper.assertTrue(reloadedFoal != null, "could not create reloaded foal");
        reloadedFoal.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), foalTag));
        helper.assertValueEqual(
            HorseCoatGeneticsAccess.get(reloadedFoal).manifestGeneToRender(),
            birthChoice,
            "Reloaded foal must preserve birth tie-break choice");

        // Also explicitly verify both possible tie-break outcomes (OBSIDIAN and ALBINO) survive save/reload without flipping.
        for (CoatGene chosen : List.of(CoatGene.OBSIDIAN, CoatGene.ALBINO)) {
            Horse horse = helper.spawn(EntityTypes.HORSE, 2, 1, 2);
            HorseCoatGeneticsAccess.set(
                horse,
                HorseCoatGeneticsAttachment.EMPTY.withGenes(
                    Map.of(
                        CoatGene.OBSIDIAN, Zygosity.HOMOZYGOUS_RECESSIVE,
                        CoatGene.ALBINO, Zygosity.HOMOZYGOUS_RECESSIVE),
                    chosen));

            TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
            horse.saveWithoutId(out);
            CompoundTag tag = out.buildResult();
            horse.discard();

            Horse fresh = EntityTypes.HORSE.create(level, EntitySpawnReason.LOAD);
            helper.assertTrue(fresh != null, "could not create fresh Horse");
            fresh.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
            helper.assertValueEqual(
                HorseCoatGeneticsAccess.get(fresh).manifestGeneToRender(),
                chosen,
                "Tie-break choice " + chosen + " must survive NBT reload");
        }

        helper.succeed();
    }

    /** The (single) live foal inside this test's bounds; never scans the whole level. */
    static Horse findNewestBaby(GameTestHelper helper) {
        Horse found = null;
        for (Horse horse : helper.getEntities(EntityTypes.HORSE)) {
            if (horse.isBaby() && horse.isAlive()) {
                found = horse;
            }
        }
        return found;
    }
}
