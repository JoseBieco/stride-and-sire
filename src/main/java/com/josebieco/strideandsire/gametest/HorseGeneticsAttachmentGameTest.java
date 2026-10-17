package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.attachment.HorseGeneticsAccess;
import com.josebieco.strideandsire.attachment.HorseGeneticsAttachment;
import com.josebieco.strideandsire.genetics.LineageTrait;
import com.josebieco.strideandsire.genetics.TraitState;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * Test instances: data/strideandsire/test_instance/genetics_attachment_*.json
 */
public final class HorseGeneticsAttachmentGameTest {
    private HorseGeneticsAttachmentGameTest() {}

    /**
     * Task 3 Step 1: sets LineageTrait.FEARLESS to TraitState.MANIFEST via HorseGeneticsAccess.set and asserts
     * HorseGeneticsAccess.get(horse).traits().get(LineageTrait.FEARLESS) == TraitState.MANIFEST while the other two
     * traits remain ABSENT.
     */
    public static void setThenGetOnHorse(GameTestHelper helper) {
        helper.makeFloor();
        Horse horse = helper.spawn(EntityTypes.HORSE, 2, 1, 2);
        HorseGeneticsAttachment initial = HorseGeneticsAccess.get(horse);
        for (LineageTrait trait : LineageTrait.values()) {
            helper.assertValueEqual(initial.traits().get(trait), TraitState.ABSENT, "new horse " + trait);
        }

        HorseGeneticsAccess.set(horse, LineageTrait.FEARLESS, TraitState.MANIFEST);
        HorseGeneticsAttachment after = HorseGeneticsAccess.get(horse);
        helper.assertValueEqual(after.traits().get(LineageTrait.FEARLESS), TraitState.MANIFEST, "FEARLESS state");
        helper.assertValueEqual(after.traits().get(LineageTrait.DRAFT), TraitState.ABSENT, "DRAFT should remain ABSENT");
        helper.assertValueEqual(after.traits().get(LineageTrait.SURE_FOOTED), TraitState.ABSENT, "SURE_FOOTED should remain ABSENT");

        // Accumulating a second trait (LATENT) preserves the first.
        HorseGeneticsAccess.set(horse, LineageTrait.DRAFT, TraitState.LATENT);
        HorseGeneticsAttachment both = HorseGeneticsAccess.get(horse);
        helper.assertValueEqual(both.traits().get(LineageTrait.FEARLESS), TraitState.MANIFEST, "FEARLESS preserved");
        helper.assertValueEqual(both.traits().get(LineageTrait.DRAFT), TraitState.LATENT, "DRAFT set to LATENT");
        helper.assertValueEqual(both.traits().get(LineageTrait.SURE_FOOTED), TraitState.ABSENT, "SURE_FOOTED still ABSENT");
        helper.succeed();
    }

    /** Persists across NBT save and load, and works on all eligible types while ignoring Camel/Llama. */
    public static void persistsAcrossSaveAndLoadAndChecksEligibility(GameTestHelper helper) {
        helper.makeFloor();
        ServerLevel level = helper.getLevel();
        for (EntityType<? extends AbstractHorse> type : List.of(
                EntityTypes.HORSE, EntityTypes.DONKEY, EntityTypes.MULE, EntityTypes.SKELETON_HORSE, EntityTypes.ZOMBIE_HORSE)) {
            AbstractHorse horse = helper.spawn(type, 2, 1, 2);
            HorseGeneticsAccess.set(horse, LineageTrait.SURE_FOOTED, TraitState.LATENT);
            HorseGeneticsAccess.set(horse, LineageTrait.FEARLESS, TraitState.MANIFEST);

            TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
            horse.saveWithoutId(output);
            CompoundTag tag = output.buildResult();

            AbstractHorse fresh = type.create(level, EntitySpawnReason.LOAD);
            helper.assertTrue(fresh != null, "could not create fresh " + type);
            fresh.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));

            HorseGeneticsAttachment loaded = HorseGeneticsAccess.get(fresh);
            helper.assertValueEqual(loaded.traits().get(LineageTrait.DRAFT), TraitState.ABSENT, type + " reloaded DRAFT");
            helper.assertValueEqual(loaded.traits().get(LineageTrait.SURE_FOOTED), TraitState.LATENT, type + " reloaded SURE_FOOTED");
            helper.assertValueEqual(loaded.traits().get(LineageTrait.FEARLESS), TraitState.MANIFEST, type + " reloaded FEARLESS");
        }

        Llama llama = helper.spawn(EntityTypes.LLAMA, 1, 1, 1);
        Camel camel = helper.spawn(EntityTypes.CAMEL, 3, 1, 3);
        for (AbstractHorse ineligible : new AbstractHorse[] {llama, camel}) {
            HorseGeneticsAccess.set(ineligible, LineageTrait.FEARLESS, TraitState.MANIFEST);
            helper.assertValueEqual(HorseGeneticsAccess.get(ineligible), HorseGeneticsAttachment.EMPTY,
                ineligible.getType() + " must remain EMPTY");
        }
        helper.succeed();
    }
}
