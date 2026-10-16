package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.attachment.HorseshoeAccess;
import com.josebieco.strideandsire.registry.ModItems;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * Test instances: data/strideandsire/test_instance/horseshoe_attachment_*.json
 */
public final class HorseshoeAttachmentGameTest {
    private HorseshoeAttachmentGameTest() {}

    /** Brief Step 1: set then get on a freshly spawned horse. */
    public static void setThenGetOnHorse(GameTestHelper helper) {
        helper.makeFloor();
        Horse horse = helper.spawn(EntityTypes.HORSE, 2, 1, 2);
        helper.assertTrue(HorseshoeAccess.get(horse).isEmpty(), "new horse should have no horseshoe");

        HorseshoeAccess.set(horse, new ItemStack(ModItems.IRON_HORSESHOE.get()));
        helper.assertTrue(HorseshoeAccess.get(horse).is(ModItems.IRON_HORSESHOE.get()), "horseshoe not stored on horse");

        HorseshoeAccess.set(horse, ItemStack.EMPTY);
        helper.assertTrue(HorseshoeAccess.get(horse).isEmpty(), "horseshoe not cleared");
        helper.succeed();
    }

    /** Every eligible type (Horse, Donkey, Mule, SkeletonHorse, ZombieHorse) stores a horseshoe. */
    public static void allEligibleTypesStoreHorseshoe(GameTestHelper helper) {
        helper.makeFloor();
        for (EntityType<? extends AbstractHorse> type : java.util.List.of(
                EntityTypes.HORSE, EntityTypes.DONKEY, EntityTypes.MULE, EntityTypes.SKELETON_HORSE, EntityTypes.ZOMBIE_HORSE)) {
            AbstractHorse horse = helper.spawn(type, 2, 1, 2);
            helper.assertTrue(HorseshoeAccess.isEligible(horse), type + " should be eligible");
            HorseshoeAccess.set(horse, new ItemStack(ModItems.GOLD_HORSESHOE.get()));
            helper.assertTrue(HorseshoeAccess.get(horse).is(ModItems.GOLD_HORSESHOE.get()), type + " did not store horseshoe");
        }
        helper.succeed();
    }

    /** Save the horse to NBT and load it into a fresh entity: item and damage survive. */
    public static void persistsAcrossSaveAndLoad(GameTestHelper helper) {
        helper.makeFloor();
        ServerLevel level = helper.getLevel();
        Horse horse = helper.spawn(EntityTypes.HORSE, 2, 1, 2);
        ItemStack shoe = new ItemStack(ModItems.IRON_HORSESHOE.get());
        shoe.setDamageValue(37);
        HorseshoeAccess.set(horse, shoe);

        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        horse.saveWithoutId(output);
        CompoundTag tag = output.buildResult();

        Horse fresh = EntityTypes.HORSE.create(level, EntitySpawnReason.LOAD);
        helper.assertTrue(fresh != null, "could not create fresh horse");
        helper.assertTrue(HorseshoeAccess.get(fresh).isEmpty(), "fresh horse should start without horseshoe");
        fresh.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));

        ItemStack loaded = HorseshoeAccess.get(fresh);
        helper.assertTrue(loaded.is(ModItems.IRON_HORSESHOE.get()), "horseshoe lost on reload, got " + loaded);
        helper.assertValueEqual(loaded.getDamageValue(), 37, "reloaded horseshoe damage");
        helper.succeed();
    }

    /** Camel and Llama extend AbstractHorse but are out of scope: get is EMPTY, set is a no-op. */
    public static void ineligibleEntitiesHaveNoHorseshoe(GameTestHelper helper) {
        helper.makeFloor();
        Llama llama = helper.spawn(EntityTypes.LLAMA, 1, 1, 1);
        Camel camel = helper.spawn(EntityTypes.CAMEL, 3, 1, 3);
        for (AbstractHorse animal : new AbstractHorse[] {llama, camel}) {
            helper.assertTrue(!HorseshoeAccess.isEligible(animal), animal.getType() + " must not be eligible");
            HorseshoeAccess.set(animal, new ItemStack(ModItems.IRON_HORSESHOE.get()));
            helper.assertTrue(HorseshoeAccess.get(animal).isEmpty(), animal.getType() + " must report no horseshoe");
        }
        helper.succeed();
    }

    /** A dying horse drops its equipped horseshoe as an item entity. */
    public static void dropsHorseshoeOnDeath(GameTestHelper helper) {
        helper.makeFloor();
        Horse horse = helper.spawn(EntityTypes.HORSE, 2, 1, 2);
        HorseshoeAccess.set(horse, new ItemStack(ModItems.NETHERITE_HORSESHOE.get()));
        horse.kill(helper.getLevel());
        helper.succeedWhen(() -> {
            helper.assertItemEntityPresent(ModItems.NETHERITE_HORSESHOE.get(), new BlockPos(2, 1, 2), 3.0);
            helper.assertTrue(HorseshoeAccess.get(horse).isEmpty(), "dead horse should no longer hold the horseshoe");
        });
    }
}
