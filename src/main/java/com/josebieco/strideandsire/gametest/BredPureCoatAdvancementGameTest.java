package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.StrideAndSire;
import com.josebieco.strideandsire.attachment.HorseCoatGeneticsAccess;
import com.josebieco.strideandsire.attachment.HorseCoatGeneticsAttachment;
import com.josebieco.strideandsire.genetics.CoatGene;
import com.josebieco.strideandsire.genetics.Zygosity;
import java.util.Map;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.eventbus.api.listener.EventListener;
import net.minecraftforge.eventbus.api.listener.Priority;

/**
 * Test instances:
 * <ul>
 *   <li>data/strideandsire/test_instance/coat_genetics_advancement_granted_on_pure_birth.json</li>
 *   <li>data/strideandsire/test_instance/coat_genetics_advancement_goes_to_breeder_not_owner.json</li>
 *   <li>data/strideandsire/test_instance/coat_genetics_advancement_skipped_when_birth_cancelled_or_replaced.json</li>
 * </ul>
 */
public final class BredPureCoatAdvancementGameTest {
    private BredPureCoatAdvancementGameTest() {}

    /**
     * Verifies that when a tamed, player-owned pair of horses breeds a foal that rolls
     * {@link Zygosity#HOMOZYGOUS_RECESSIVE} on {@link CoatGene#ALBINO}, the owning {@link ServerPlayer} is granted the
     * {@code strideandsire:albino_coat} advancement while {@code obsidian_coat} and {@code silver_dapple_coat} remain
     * incomplete.
     */
    public static void advancementGrantedOnPureBirth(GameTestHelper helper) {
        helper.makeFloor();
        ServerLevel level = helper.getLevel();
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);

        AdvancementHolder albinoAdv = level.getServer().getAdvancements().get(
            Identifier.fromNamespaceAndPath(StrideAndSire.MODID, "albino_coat"));
        AdvancementHolder obsidianAdv = level.getServer().getAdvancements().get(
            Identifier.fromNamespaceAndPath(StrideAndSire.MODID, "obsidian_coat"));
        AdvancementHolder silverDappleAdv = level.getServer().getAdvancements().get(
            Identifier.fromNamespaceAndPath(StrideAndSire.MODID, "silver_dapple_coat"));

        helper.assertTrue(albinoAdv != null, "strideandsire:albino_coat advancement must be loaded");
        helper.assertTrue(obsidianAdv != null, "strideandsire:obsidian_coat advancement must be loaded");
        helper.assertTrue(silverDappleAdv != null, "strideandsire:silver_dapple_coat advancement must be loaded");

        helper.assertFalse(
            player.getAdvancements().getOrStartProgress(albinoAdv).isDone(),
            "albino_coat must not be done before breeding");

        Horse parentA = helper.spawn(EntityTypes.HORSE, 1, 1, 1);
        Horse parentB = helper.spawn(EntityTypes.HORSE, 2, 1, 1);
        parentA.tameWithName(player);
        parentB.tameWithName(player);
        helper.assertValueEqual(parentA.getOwner(), player, "parentA owner must be player");

        HorseCoatGeneticsAttachment albinoRecessive = HorseCoatGeneticsAttachment.EMPTY.withGenes(
            Map.of(CoatGene.ALBINO, Zygosity.HOMOZYGOUS_RECESSIVE),
            CoatGene.ALBINO);
        HorseCoatGeneticsAccess.set(parentA, albinoRecessive);
        HorseCoatGeneticsAccess.set(parentB, albinoRecessive);

        parentA.setAge(0);
        parentB.setAge(0);
        parentA.setInLove(player);
        parentB.setInLove(player);
        parentA.spawnChildFromBreeding(level, parentB);

        helper.assertTrue(
            player.getAdvancements().getOrStartProgress(albinoAdv).isDone(),
            "albino_coat advancement must be granted when a pure ALBINO foal is born to player's horses");
        helper.assertFalse(
            player.getAdvancements().getOrStartProgress(obsidianAdv).isDone(),
            "obsidian_coat advancement must remain incomplete");
        helper.assertFalse(
            player.getAdvancements().getOrStartProgress(silverDappleAdv).isDone(),
            "silver_dapple_coat advancement must remain incomplete");

        helper.succeed();
    }

    private static AdvancementHolder advancement(ServerLevel level, String path) {
        return level.getServer().getAdvancements().get(Identifier.fromNamespaceAndPath(StrideAndSire.MODID, path));
    }

    private static Horse[] pureAlbinoPair(GameTestHelper helper, int z) {
        Horse parentA = helper.spawn(EntityTypes.HORSE, 1, 1, z);
        Horse parentB = helper.spawn(EntityTypes.HORSE, 2, 1, z);
        HorseCoatGeneticsAttachment albinoRecessive = HorseCoatGeneticsAttachment.EMPTY.withGenes(
            Map.of(CoatGene.ALBINO, Zygosity.HOMOZYGOUS_RECESSIVE),
            CoatGene.ALBINO);
        HorseCoatGeneticsAccess.set(parentA, albinoRecessive);
        HorseCoatGeneticsAccess.set(parentB, albinoRecessive);
        return new Horse[] {parentA, parentB};
    }

    private static void breed(ServerLevel level, Horse parentA, Horse parentB, ServerPlayer loveCause) {
        parentA.setAge(0);
        parentB.setAge(0);
        parentA.setInLove(loveCause);
        parentB.setInLove(loveCause);
        parentA.spawnChildFromBreeding(level, parentB);
    }

    /**
     * Vanilla {@code BRED_ANIMALS} parity: the advancement goes to the player who caused the breeding (love cause),
     * not the parents' owner; with no love cause it falls back to the owner.
     */
    public static void advancementGoesToBreederNotOwner(GameTestHelper helper) {
        helper.makeFloor();
        ServerLevel level = helper.getLevel();
        ServerPlayer owner = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        ServerPlayer breeder = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        AdvancementHolder albinoAdv = advancement(level, "albino_coat");
        helper.assertTrue(albinoAdv != null, "strideandsire:albino_coat advancement must be loaded");

        Horse[] pair = pureAlbinoPair(helper, 1);
        pair[0].tameWithName(owner);
        pair[1].tameWithName(owner);
        breed(level, pair[0], pair[1], breeder);
        helper.assertTrue(breeder.getAdvancements().getOrStartProgress(albinoAdv).isDone(),
            "the breeding player (love cause) must get albino_coat");
        helper.assertFalse(owner.getAdvancements().getOrStartProgress(albinoAdv).isDone(),
            "the owner must not get albino_coat when someone else bred the horses");

        // No love cause: fall back to the owner.
        ServerPlayer fallbackOwner = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        Horse[] ownedPair = pureAlbinoPair(helper, 2);
        ownedPair[0].tameWithName(fallbackOwner);
        ownedPair[1].tameWithName(fallbackOwner);
        breed(level, ownedPair[0], ownedPair[1], null);
        helper.assertTrue(fallbackOwner.getAdvancements().getOrStartProgress(albinoAdv).isDone(),
            "with no love cause the owner must get albino_coat");
        helper.succeed();
    }

    /**
     * The advancement is only granted for a foal that is actually born: not when another listener cancels the
     * {@link BabyEntitySpawnEvent} after ours ran, nor when it replaces the child.
     */
    public static void advancementSkippedWhenBirthCancelledOrReplaced(GameTestHelper helper) {
        helper.makeFloor();
        ServerLevel level = helper.getLevel();
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        AdvancementHolder albinoAdv = advancement(level, "albino_coat");
        helper.assertTrue(albinoAdv != null, "strideandsire:albino_coat advancement must be loaded");

        Horse[] pair = pureAlbinoPair(helper, 1);
        pair[0].tameWithName(player);
        pair[1].tameWithName(player);

        EventListener canceller = BabyEntitySpawnEvent.BUS.addListener(Priority.LOWEST, event -> true);
        try {
            breed(level, pair[0], pair[1], player);
        } finally {
            BabyEntitySpawnEvent.BUS.removeListener(canceller);
        }
        helper.assertTrue(HorseCoatBreedingGameTest.findNewestBaby(helper) == null, "a cancelled birth spawns no foal");
        helper.assertFalse(player.getAdvancements().getOrStartProgress(albinoAdv).isDone(),
            "albino_coat must not be granted when the birth is cancelled");

        EventListener replacer = BabyEntitySpawnEvent.BUS.addListener(Priority.LOWEST, event -> {
            event.setChild(EntityTypes.HORSE.create(level, EntitySpawnReason.BREEDING));
            return false;
        });
        try {
            breed(level, pair[0], pair[1], player);
        } finally {
            BabyEntitySpawnEvent.BUS.removeListener(replacer);
        }
        helper.assertFalse(player.getAdvancements().getOrStartProgress(albinoAdv).isDone(),
            "albino_coat must not be granted for a pure foal that was replaced before spawning");

        // Sanity: an undisturbed birth from the same pair does grant it.
        breed(level, pair[0], pair[1], player);
        helper.assertTrue(player.getAdvancements().getOrStartProgress(albinoAdv).isDone(),
            "an undisturbed pure ALBINO birth must grant albino_coat");
        helper.succeed();
    }
}
