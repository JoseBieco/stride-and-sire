package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.attachment.HorseCoatGeneticsAccess;
import com.josebieco.strideandsire.attachment.HorseCoatGeneticsAttachment;
import com.josebieco.strideandsire.command.SetCoatCommand;
import com.josebieco.strideandsire.genetics.CoatGene;
import com.josebieco.strideandsire.genetics.Zygosity;
import net.minecraft.gametest.framework.GameTestHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Random;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.PermissionSet;
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
            horse, Zygosity.HOMOZYGOUS_RECESSIVE, Zygosity.HETEROZYGOUS, Zygosity.HOMOZYGOUS_DOMINANT, new Random(42L));
        helper.assertTrue(applied, "applyToHorse must return true for an eligible Horse");

        HorseCoatGeneticsAttachment attachment = HorseCoatGeneticsAccess.get(horse);
        helper.assertValueEqual(attachment.zygosity(CoatGene.OBSIDIAN), Zygosity.HOMOZYGOUS_RECESSIVE, "OBSIDIAN");
        helper.assertValueEqual(attachment.zygosity(CoatGene.ALBINO), Zygosity.HETEROZYGOUS, "ALBINO");
        helper.assertValueEqual(attachment.zygosity(CoatGene.SILVER_DAPPLE), Zygosity.HOMOZYGOUS_DOMINANT, "SILVER_DAPPLE");
        helper.assertValueEqual(attachment.manifestGeneToRender(), CoatGene.OBSIDIAN, "manifestGeneToRender");
        helper.succeed();
    }

    /**
     * Runs the real {@code /strideandsire setcoat} through the server's command dispatcher: an operator source applies the
     * genotype; a malformed genotype token and a source without permissions are both rejected without touching the horse.
     */
    public static void commandDispatchAppliesAndGuards(GameTestHelper helper) {
        helper.makeFloor();
        Horse horse = helper.spawn(EntityTypes.HORSE, 2, 1, 2);
        CommandDispatcher<CommandSourceStack> dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        CommandSourceStack op = helper.getLevel().getServer().createCommandSourceStack().withSuppressedOutput();
        String uuid = horse.getStringUUID();

        helper.assertTrue(rejects(dispatcher, "strideandsire setcoat " + uuid + " aA Aa AA", op),
            "malformed token must be rejected");
        helper.assertTrue(HorseCoatGeneticsAccess.get(horse).isEmpty(), "rejected command must not change the horse");

        CommandSourceStack noPermissions = op.withPermission(PermissionSet.NO_PERMISSIONS);
        helper.assertTrue(rejects(dispatcher, "strideandsire setcoat " + uuid + " aa Aa AA", noPermissions),
            "a source without permissions must not be able to run setcoat");
        helper.assertTrue(HorseCoatGeneticsAccess.get(horse).isEmpty(), "unpermitted command must not change the horse");

        int applied;
        try {
            applied = dispatcher.execute("strideandsire setcoat " + uuid + " aa Aa AA", op);
        } catch (CommandSyntaxException e) {
            throw helper.assertionException("operator setcoat failed: " + e.getMessage());
        }
        helper.assertValueEqual(applied, 1, "applied count");
        HorseCoatGeneticsAttachment attachment = HorseCoatGeneticsAccess.get(horse);
        helper.assertValueEqual(attachment.zygosity(CoatGene.OBSIDIAN), Zygosity.HOMOZYGOUS_RECESSIVE, "OBSIDIAN");
        helper.assertValueEqual(attachment.zygosity(CoatGene.ALBINO), Zygosity.HETEROZYGOUS, "ALBINO");
        helper.assertValueEqual(attachment.zygosity(CoatGene.SILVER_DAPPLE), Zygosity.HOMOZYGOUS_DOMINANT, "SILVER_DAPPLE");
        helper.assertValueEqual(attachment.manifestGeneToRender(), CoatGene.OBSIDIAN, "manifestGeneToRender");
        helper.succeed();
    }

    private static boolean rejects(CommandDispatcher<CommandSourceStack> dispatcher, String command, CommandSourceStack source) {
        try {
            dispatcher.execute(command, source);
            return false;
        } catch (CommandSyntaxException e) {
            return true;
        }
    }

    /** {@link SetCoatCommand#applyToHorse} is a no-op on a Mule (not eligible for coat genetics). */
    public static void ignoresIneligibleMule(GameTestHelper helper) {
        helper.makeFloor();
        Mule mule = helper.spawn(EntityTypes.MULE, 2, 1, 2);

        boolean applied = SetCoatCommand.applyToHorse(
            mule, Zygosity.HOMOZYGOUS_RECESSIVE, Zygosity.HOMOZYGOUS_RECESSIVE, Zygosity.HOMOZYGOUS_RECESSIVE, new Random(42L));
        helper.assertFalse(applied, "applyToHorse must return false for an ineligible Mule");
        helper.assertTrue(HorseCoatGeneticsAccess.get(mule).isEmpty(), "Mule coat genetics must remain EMPTY");
        helper.succeed();
    }
}
