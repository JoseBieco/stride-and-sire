package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.StrideAndSire;
import com.josebieco.strideandsire.attachment.HorseCoatGeneticsAccess;
import com.josebieco.strideandsire.attachment.HorseCoatGeneticsAttachment;
import com.josebieco.strideandsire.attachment.HorseGeneticsAccess;
import com.josebieco.strideandsire.attachment.HorseshoeAccess;
import com.josebieco.strideandsire.attachment.SaddlebagAccess;
import com.josebieco.strideandsire.genetics.CoatGene;
import com.josebieco.strideandsire.genetics.LineageTrait;
import com.josebieco.strideandsire.genetics.TraitState;
import com.josebieco.strideandsire.genetics.Zygosity;
import com.josebieco.strideandsire.menu.BreederSpyglassScreen;
import com.josebieco.strideandsire.menu.BreederSpyglassScreen.SpyglassReport;
import com.josebieco.strideandsire.registry.ModItems;
import com.josebieco.strideandsire.render.HorseCoatRendering;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;

/**
 * Test instance: data/strideandsire/test_instance/coat_genetics_review_focus_cross_phase_and_edge_cases.json
 */
public final class CoatGeneticsReviewFocusGameTest {
    private CoatGeneticsReviewFocusGameTest() {}

    /**
     * Cross-phase and edge-case regression suite for Fase D (Task 11):
     * <ol>
     *   <li><b>Wild spawn idempotency across re-join/reload (Review Focus 1):</b> wild horses rolled once on natural
     *       spawn never re-roll across subsequent {@link EntityJoinLevelEvent} posts or NBT save/reload cycles.</li>
     *   <li><b>Cross-phase independence with Fases A, B, C:</b> horseshoes, saddlebags, lineage traits, and coat
     *       genetics coexist and persist independently across NBT save/reload.</li>
     *   <li><b>Dual-manifest tie-break stability across reload &amp; Spyglass inspection (Review Focus 2):</b>
     *       {@code manifestGeneToRender} and {@link HorseCoatRendering#textureToRender} remain unchanged across reload
     *       and repeated Spyglass inspections, and inspecting a pure-coat horse does not grant pure-coat advancements.</li>
     *   <li><b>Horse-only eligibility vs Fase A/B/C equines (Review Focus 3):</b> Donkey, Mule, Skeleton Horse, and
     *       Zombie Horse remain eligible for horseshoes while ineligible for coat genetics.</li>
     *   <li><b>Idempotent advancement granting on repeated pure births (Review Focus 5):</b> breeding a second pure
     *       foal for an already-completed coat advancement succeeds cleanly without throwing.</li>
     * </ol>
     */
    public static void crossPhaseAndEdgeCases(GameTestHelper helper) {
        helper.makeFloor();
        ServerLevel level = helper.getLevel();
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);

        // 1. Wild spawn idempotency across re-join/reload (Review Focus 1).
        BlockPos spawnPos = helper.absolutePos(new BlockPos(1, 1, 1));
        Vec3 spawnVec = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
        Horse wild = EntityTypes.HORSE.create(level, EntitySpawnReason.NATURAL);
        helper.assertTrue(wild != null, "could not create wild Horse");
        wild.snapTo(spawnVec.x, spawnVec.y, spawnVec.z, 0.0F, 0.0F);
        wild.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), EntitySpawnReason.NATURAL, null);
        helper.assertTrue(level.addFreshEntity(wild), "wild Horse should be added to level");
        for (CoatGene gene : CoatGene.values()) {
            helper.assertFalse(
                HorseCoatGeneticsAccess.get(wild).isManifest(gene),
                "wild Horse must not manifest " + gene + " on initial natural spawn");
        }

        HorseCoatGeneticsAttachment knownCarrier = HorseCoatGeneticsAttachment.EMPTY.withGenes(
            Map.of(
                CoatGene.OBSIDIAN, Zygosity.HETEROZYGOUS,
                CoatGene.ALBINO, Zygosity.HOMOZYGOUS_DOMINANT,
                CoatGene.SILVER_DAPPLE, Zygosity.HOMOZYGOUS_DOMINANT),
            null);
        HorseCoatGeneticsAccess.set(wild, knownCarrier);

        EntityJoinLevelEvent.BUS.post(new EntityJoinLevelEvent(wild, level, false));
        helper.assertValueEqual(
            HorseCoatGeneticsAccess.get(wild).genes(),
            knownCarrier.genes(),
            "wild Horse coat genetics must not re-roll on EntityJoinLevelEvent(loadedFromDisk=false)");

        EntityJoinLevelEvent.BUS.post(new EntityJoinLevelEvent(wild, level, true));
        helper.assertValueEqual(
            HorseCoatGeneticsAccess.get(wild).genes(),
            knownCarrier.genes(),
            "wild Horse coat genetics must not re-roll on EntityJoinLevelEvent(loadedFromDisk=true)");

        TagValueOutput wildOut = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        wild.saveWithoutId(wildOut);
        CompoundTag wildTag = wildOut.buildResult();
        wild.discard();

        Horse reloadedWild = EntityTypes.HORSE.create(level, EntitySpawnReason.NATURAL);
        helper.assertTrue(reloadedWild != null, "could not create reloadedWild Horse");
        reloadedWild.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), wildTag));
        reloadedWild.setUUID(UUID.randomUUID());
        helper.assertTrue(level.addFreshEntity(reloadedWild), "reloadedWild should be added to level");
        helper.assertValueEqual(
            HorseCoatGeneticsAccess.get(reloadedWild).genes(),
            knownCarrier.genes(),
            "wild Horse coat genetics must survive NBT save/reload + addFreshEntity without re-rolling");
        reloadedWild.discard();

        // 2. Cross-phase independence with Fases A, B, C.
        Horse multiPhaseHorse = helper.spawn(EntityTypes.HORSE, 2, 1, 1);
        multiPhaseHorse.setTamed(true);
        multiPhaseHorse.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        HorseshoeAccess.set(multiPhaseHorse, new ItemStack(ModItems.IRON_HORSESHOE.get()));
        SaddlebagAccess.set(multiPhaseHorse, new ItemStack(ModItems.LEATHER_SADDLEBAG.get()));
        HorseGeneticsAccess.set(multiPhaseHorse, LineageTrait.SURE_FOOTED, TraitState.MANIFEST);
        HorseCoatGeneticsAttachment silverDappleManifest = HorseCoatGeneticsAttachment.EMPTY.withGenes(
            Map.of(CoatGene.SILVER_DAPPLE, Zygosity.HOMOZYGOUS_RECESSIVE),
            CoatGene.SILVER_DAPPLE);
        HorseCoatGeneticsAccess.set(multiPhaseHorse, silverDappleManifest);

        assertMultiPhaseState(helper, multiPhaseHorse, silverDappleManifest, "before reload");

        TagValueOutput multiOut = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        multiPhaseHorse.saveWithoutId(multiOut);
        CompoundTag multiTag = multiOut.buildResult();

        Horse reloadedMulti = EntityTypes.HORSE.create(level, EntitySpawnReason.LOAD);
        helper.assertTrue(reloadedMulti != null, "could not create reloadedMulti Horse");
        reloadedMulti.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), multiTag));
        assertMultiPhaseState(helper, reloadedMulti, silverDappleManifest, "after reload");

        // 3. Dual-manifest tie-break stability across reload & Spyglass inspection (Review Focus 2).
        Horse dualManifestHorse = helper.spawn(EntityTypes.HORSE, 3, 1, 1);
        dualManifestHorse.setTamed(true);
        HorseCoatGeneticsAttachment dualManifest = HorseCoatGeneticsAttachment.EMPTY.withGenes(
            Map.of(
                CoatGene.OBSIDIAN, Zygosity.HOMOZYGOUS_RECESSIVE,
                CoatGene.ALBINO, Zygosity.HOMOZYGOUS_RECESSIVE),
            CoatGene.ALBINO);
        HorseCoatGeneticsAccess.set(dualManifestHorse, dualManifest);

        Identifier expectedAlbinoTexture = HorseCoatRendering.coatTexture(CoatGene.ALBINO);
        helper.assertValueEqual(
            HorseCoatGeneticsAccess.get(dualManifestHorse).manifestGeneToRender(),
            CoatGene.ALBINO,
            "manifestGeneToRender before reload must be ALBINO");
        helper.assertValueEqual(
            HorseCoatRendering.textureToRender(dualManifestHorse),
            expectedAlbinoTexture,
            "textureToRender before reload must be ALBINO texture");

        TagValueOutput dualOut = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        dualManifestHorse.saveWithoutId(dualOut);
        CompoundTag dualTag = dualOut.buildResult();

        Horse reloadedDual = EntityTypes.HORSE.create(level, EntitySpawnReason.LOAD);
        helper.assertTrue(reloadedDual != null, "could not create reloadedDual Horse");
        reloadedDual.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), dualTag));
        reloadedDual.setUUID(UUID.randomUUID());
        reloadedDual.snapTo(spawnVec.x + 1.0, spawnVec.y, spawnVec.z, 0.0F, 0.0F);
        helper.assertTrue(level.addFreshEntity(reloadedDual), "reloadedDual should be added to level");

        helper.assertValueEqual(
            HorseCoatGeneticsAccess.get(reloadedDual).manifestGeneToRender(),
            CoatGene.ALBINO,
            "manifestGeneToRender after reload must still be ALBINO");
        helper.assertValueEqual(
            HorseCoatRendering.textureToRender(reloadedDual),
            expectedAlbinoTexture,
            "textureToRender after reload must still be ALBINO texture");

        SpyglassReport firstReport = BreederSpyglassScreen.buildReport(reloadedDual);
        SpyglassReport secondReport = BreederSpyglassScreen.buildReport(reloadedDual);
        helper.assertValueEqual(firstReport, secondReport, "repeated Spyglass reports must be identical");

        ItemStack spyglass = new ItemStack(Items.SPYGLASS);
        player.setItemInHand(InteractionHand.MAIN_HAND, spyglass);
        player.setShiftKeyDown(true);
        player.interactOn(reloadedDual, InteractionHand.MAIN_HAND, reloadedDual.position());
        player.interactOn(reloadedDual, InteractionHand.MAIN_HAND, reloadedDual.position());

        helper.assertValueEqual(
            HorseCoatGeneticsAccess.get(reloadedDual).manifestGeneToRender(),
            CoatGene.ALBINO,
            "manifestGeneToRender after Spyglass inspections must still be ALBINO");
        helper.assertValueEqual(
            HorseCoatRendering.textureToRender(reloadedDual),
            expectedAlbinoTexture,
            "textureToRender after Spyglass inspections must still be ALBINO texture");

        AdvancementHolder albinoAdv = level.getServer().getAdvancements().get(
            Identifier.fromNamespaceAndPath(StrideAndSire.MODID, "albino_coat"));
        AdvancementHolder obsidianAdv = level.getServer().getAdvancements().get(
            Identifier.fromNamespaceAndPath(StrideAndSire.MODID, "obsidian_coat"));
        helper.assertTrue(albinoAdv != null && obsidianAdv != null, "pure coat advancements must exist");
        helper.assertFalse(
            player.getAdvancements().getOrStartProgress(albinoAdv).isDone(),
            "inspecting a pure-coat horse with the Spyglass must NOT grant albino_coat");
        helper.assertFalse(
            player.getAdvancements().getOrStartProgress(obsidianAdv).isDone(),
            "inspecting a pure-coat horse with the Spyglass must NOT grant obsidian_coat");

        // 4. Horse-only eligibility vs Fase A/B/C equines (Review Focus 3).
        for (EntityType<? extends AbstractHorse> type : List.of(
                EntityTypes.DONKEY, EntityTypes.MULE, EntityTypes.SKELETON_HORSE, EntityTypes.ZOMBIE_HORSE)) {
            AbstractHorse nonHorseEquine = helper.spawn(type, 2, 1, 2);
            helper.assertTrue(
                HorseshoeAccess.isEligible(nonHorseEquine),
                type + " must be eligible for horseshoes (Fase A)");
            helper.assertFalse(
                HorseCoatGeneticsAccess.isEligible(nonHorseEquine),
                type + " must not be eligible for coat genetics (Fase D)");
            HorseCoatGeneticsAccess.set(nonHorseEquine, dualManifest);
            helper.assertValueEqual(
                HorseCoatGeneticsAccess.get(nonHorseEquine),
                HorseCoatGeneticsAttachment.EMPTY,
                type + " coat genetics must remain EMPTY after set");
            nonHorseEquine.discard();
        }

        // 5. Idempotent advancement granting on repeated pure births (Review Focus 5).
        Horse parentA = helper.spawn(EntityTypes.HORSE, 1, 1, 2);
        Horse parentB = helper.spawn(EntityTypes.HORSE, 2, 1, 2);
        parentA.tameWithName(player);
        parentB.tameWithName(player);
        HorseCoatGeneticsAttachment pureObsidian = HorseCoatGeneticsAttachment.EMPTY.withGenes(
            Map.of(CoatGene.OBSIDIAN, Zygosity.HOMOZYGOUS_RECESSIVE),
            CoatGene.OBSIDIAN);
        HorseCoatGeneticsAccess.set(parentA, pureObsidian);
        HorseCoatGeneticsAccess.set(parentB, pureObsidian);

        parentA.setAge(0);
        parentB.setAge(0);
        parentA.setInLove(player);
        parentB.setInLove(player);
        parentA.spawnChildFromBreeding(level, parentB);
        helper.assertTrue(
            player.getAdvancements().getOrStartProgress(obsidianAdv).isDone(),
            "obsidian_coat advancement must be granted after first pure OBSIDIAN birth");

        parentA.setAge(0);
        parentB.setAge(0);
        parentA.setInLove(player);
        parentB.setInLove(player);
        parentA.spawnChildFromBreeding(level, parentB);
        helper.assertTrue(
            player.getAdvancements().getOrStartProgress(obsidianAdv).isDone(),
            "obsidian_coat advancement must remain done after second pure OBSIDIAN birth without throwing");

        helper.succeed();
    }

    private static void assertMultiPhaseState(
            GameTestHelper helper,
            Horse horse,
            HorseCoatGeneticsAttachment expectedCoat,
            String phaseLabel) {
        helper.assertTrue(
            HorseshoeAccess.get(horse).is(ModItems.IRON_HORSESHOE.get()),
            "Horseshoe must be IRON_HORSESHOE " + phaseLabel);
        helper.assertTrue(
            SaddlebagAccess.get(horse).is(ModItems.LEATHER_SADDLEBAG.get()),
            "Saddlebag must be LEATHER_SADDLEBAG " + phaseLabel);
        helper.assertValueEqual(
            HorseGeneticsAccess.getState(horse, LineageTrait.SURE_FOOTED),
            TraitState.MANIFEST,
            "LineageTrait.SURE_FOOTED must be MANIFEST " + phaseLabel);
        helper.assertValueEqual(
            HorseCoatGeneticsAccess.get(horse),
            expectedCoat,
            "HorseCoatGeneticsAttachment must match expected " + phaseLabel);
        helper.assertValueEqual(
            HorseCoatRendering.textureToRender(horse),
            HorseCoatRendering.coatTexture(CoatGene.SILVER_DAPPLE),
            "HorseCoatRendering.textureToRender must return SILVER_DAPPLE texture " + phaseLabel);
    }
}
