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
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.GameType;

/**
 * Test instance: data/strideandsire/test_instance/coat_genetics_advancement_granted_on_pure_birth.json
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
}
