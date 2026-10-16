package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.attachment.SaddlebagAccess;
import com.josebieco.strideandsire.item.saddlebag.SaddlebagItem;
import com.josebieco.strideandsire.registry.ModItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * Test instances: data/strideandsire/test_instance/saddlebag_attachment_*.json
 */
public final class SaddlebagAttachmentGameTest {
    private SaddlebagAttachmentGameTest() {}

    /** Brief Step 1: a Gold saddlebag holding one item survives set/get with its contents. */
    public static void setThenGetKeepsContents(GameTestHelper helper) {
        helper.makeFloor();
        Horse horse = helper.spawn(EntityTypes.HORSE, 2, 1, 2);
        helper.assertTrue(SaddlebagAccess.get(horse).isEmpty(), "new horse should have no saddlebag");

        SaddlebagAccess.set(horse, goldBagWithDiamonds());
        ItemStack got = SaddlebagAccess.get(horse);
        helper.assertTrue(got.is(ModItems.GOLD_SADDLEBAG.get()), "saddlebag not stored on horse, got " + got);
        assertHoldsDiamonds(helper, got, "after set/get");

        SaddlebagAccess.set(horse, ItemStack.EMPTY);
        helper.assertTrue(SaddlebagAccess.get(horse).isEmpty(), "saddlebag not cleared");
        helper.succeed();
    }

    /** Save the horse to NBT and load it into a fresh entity: saddlebag and its contents survive. */
    public static void persistsAcrossSaveAndLoad(GameTestHelper helper) {
        helper.makeFloor();
        ServerLevel level = helper.getLevel();
        Horse horse = helper.spawn(EntityTypes.HORSE, 2, 1, 2);
        SaddlebagAccess.set(horse, goldBagWithDiamonds());

        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        horse.saveWithoutId(output);
        CompoundTag tag = output.buildResult();

        Horse fresh = EntityTypes.HORSE.create(level, EntitySpawnReason.LOAD);
        helper.assertTrue(fresh != null, "could not create fresh horse");
        helper.assertTrue(SaddlebagAccess.get(fresh).isEmpty(), "fresh horse should start without saddlebag");
        fresh.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));

        ItemStack loaded = SaddlebagAccess.get(fresh);
        helper.assertTrue(loaded.is(ModItems.GOLD_SADDLEBAG.get()), "saddlebag lost on reload, got " + loaded);
        assertHoldsDiamonds(helper, loaded, "after reload");
        helper.succeed();
    }

    /** Llama is out of scope: get is EMPTY and set is a no-op. */
    public static void ineligibleLlamaHasNoSaddlebag(GameTestHelper helper) {
        helper.makeFloor();
        Llama llama = helper.spawn(EntityTypes.LLAMA, 2, 1, 2);
        helper.assertTrue(SaddlebagAccess.get(llama).isEmpty(), "llama must start with no saddlebag");
        SaddlebagAccess.set(llama, goldBagWithDiamonds());
        helper.assertTrue(SaddlebagAccess.get(llama).isEmpty(), "llama must still report no saddlebag after set");
        helper.succeed();
    }

    /** Killed by generic damage: exactly one saddlebag item entity with intact contents; attachment cleared. */
    public static void dropsSaddlebagOnDeathWithContents(GameTestHelper helper) {
        helper.makeFloor();
        Horse horse = helper.spawn(EntityTypes.HORSE, 2, 1, 2);
        SaddlebagAccess.set(horse, goldBagWithDiamonds());
        horse.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), Float.MAX_VALUE);
        helper.assertTrue(horse.isDeadOrDying(), "horse should be dead after generic damage");

        Item bag = ModItems.GOLD_SADDLEBAG.get();
        helper.succeedWhen(() -> {
            helper.assertItemEntityCountIs(bag, new BlockPos(2, 1, 2), 3.0, 1);
            helper.assertTrue(SaddlebagAccess.get(horse).isEmpty(), "dead horse should no longer hold the saddlebag");
            List<ItemEntity> drops = helper.getEntities(EntityTypes.ITEM).stream()
                .filter(e -> e.getItem().is(bag)).toList();
            helper.assertValueEqual(drops.size(), 1, "saddlebag item entity count");
            assertHoldsDiamonds(helper, drops.get(0).getItem(), "dropped saddlebag");
        });
    }

    private static ItemStack goldBagWithDiamonds() {
        ItemStack bag = new ItemStack(ModItems.GOLD_SADDLEBAG.get());
        SaddlebagItem.setContents(bag, List.of(ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.DIAMOND, 5)));
        return bag;
    }

    private static void assertHoldsDiamonds(GameTestHelper helper, ItemStack bag, String when) {
        NonNullList<ItemStack> contents = SaddlebagItem.getContents(bag);
        helper.assertValueEqual(contents.size(), 9, "contents size " + when);
        for (int i = 0; i < contents.size(); i++) {
            ItemStack s = contents.get(i);
            if (i == 2) {
                helper.assertTrue(s.is(Items.DIAMOND) && s.getCount() == 5, "slot 2 should hold 5 diamonds " + when + ", got " + s);
            } else {
                helper.assertTrue(s.isEmpty(), "slot " + i + " should be empty " + when + ", got " + s);
            }
        }
    }
}
