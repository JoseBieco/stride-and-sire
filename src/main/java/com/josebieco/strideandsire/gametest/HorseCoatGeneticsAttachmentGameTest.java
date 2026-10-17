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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.Mule;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * Test instances: data/strideandsire/test_instance/coat_genetics_attachment_*.json
 */
public final class HorseCoatGeneticsAttachmentGameTest {
    private HorseCoatGeneticsAttachmentGameTest() {}

    /**
     * Spawns a Horse, stores a recessive coat genetics attachment via {@link HorseCoatGeneticsAccess#set}, and asserts
     * both in-memory retrieval and NBT save/load persistence.
     */
    public static void setThenGetOnHorse(GameTestHelper helper) {
        helper.makeFloor();
        ServerLevel level = helper.getLevel();
        Horse horse = helper.spawn(EntityTypes.HORSE, 2, 1, 2);
        helper.assertTrue(HorseCoatGeneticsAccess.isEligible(horse), "Horse must be eligible for coat genetics");
        helper.assertTrue(HorseCoatGeneticsAccess.get(horse).isEmpty(), "New horse must start with EMPTY coat genetics");

        HorseCoatGeneticsAttachment target = HorseCoatGeneticsAttachment.EMPTY.withGenes(
            Map.of(
                CoatGene.OBSIDIAN, Zygosity.HOMOZYGOUS_RECESSIVE,
                CoatGene.ALBINO, Zygosity.HETEROZYGOUS),
            CoatGene.OBSIDIAN);
        HorseCoatGeneticsAccess.set(horse, target);

        HorseCoatGeneticsAttachment after = HorseCoatGeneticsAccess.get(horse);
        helper.assertTrue(after.isManifest(CoatGene.OBSIDIAN), "OBSIDIAN should be manifest");
        helper.assertValueEqual(after.zygosity(CoatGene.ALBINO), Zygosity.HETEROZYGOUS, "ALBINO should be heterozygous");
        helper.assertValueEqual(
            after.zygosity(CoatGene.SILVER_DAPPLE),
            Zygosity.HOMOZYGOUS_DOMINANT,
            "SILVER_DAPPLE should default to HOMOZYGOUS_DOMINANT");
        helper.assertValueEqual(after.manifestGeneToRender(), CoatGene.OBSIDIAN, "manifestGeneToRender should be OBSIDIAN");

        // Verify persistence across NBT save and load.
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        horse.saveWithoutId(output);
        CompoundTag tag = output.buildResult();

        Horse fresh = EntityTypes.HORSE.create(level, EntitySpawnReason.LOAD);
        helper.assertTrue(fresh != null, "could not create fresh Horse");
        fresh.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));

        HorseCoatGeneticsAttachment loaded = HorseCoatGeneticsAccess.get(fresh);
        helper.assertValueEqual(loaded, target, "Reloaded HorseCoatGeneticsAttachment must match saved value");
        helper.succeed();
    }

    /**
     * Asserts that non-Horse equines (Mule, Donkey, Skeleton Horse, Zombie Horse) are ineligible for coat genetics and
     * ignore {@link HorseCoatGeneticsAccess#set}.
     */
    public static void ineligibleMuleHasNoCoatGenetics(GameTestHelper helper) {
        helper.makeFloor();
        Mule mule = helper.spawn(EntityTypes.MULE, 2, 1, 2);
        HorseCoatGeneticsAttachment nonEmpty = HorseCoatGeneticsAttachment.EMPTY.withGenes(
            Map.of(CoatGene.OBSIDIAN, Zygosity.HOMOZYGOUS_RECESSIVE),
            CoatGene.OBSIDIAN);

        helper.assertFalse(HorseCoatGeneticsAccess.isEligible(mule), "Mule must not be eligible for coat genetics");
        HorseCoatGeneticsAccess.set(mule, nonEmpty);
        helper.assertTrue(HorseCoatGeneticsAccess.get(mule).isEmpty(), "Mule coat genetics must remain EMPTY");

        for (EntityType<? extends AbstractHorse> type : List.of(
                EntityTypes.DONKEY, EntityTypes.SKELETON_HORSE, EntityTypes.ZOMBIE_HORSE)) {
            AbstractHorse other = helper.spawn(type, 2, 1, 2);
            helper.assertFalse(HorseCoatGeneticsAccess.isEligible(other), type + " must not be eligible for coat genetics");
            HorseCoatGeneticsAccess.set(other, nonEmpty);
            helper.assertTrue(HorseCoatGeneticsAccess.get(other).isEmpty(), type + " coat genetics must remain EMPTY");
        }
        helper.succeed();
    }
}
