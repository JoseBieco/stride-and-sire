package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.attachment.HorseCoatGeneticsAccess;
import com.josebieco.strideandsire.attachment.HorseCoatGeneticsAttachment;
import com.josebieco.strideandsire.attachment.HorseGeneticsAccess;
import com.josebieco.strideandsire.attachment.HorseGeneticsAttachment;
import com.josebieco.strideandsire.event.BreederSpyglassInteractionHandler;
import com.josebieco.strideandsire.genetics.CoatGene;
import com.josebieco.strideandsire.genetics.GeneticAttribute;
import com.josebieco.strideandsire.genetics.Grade;
import com.josebieco.strideandsire.genetics.HorseAttributeRanges;
import com.josebieco.strideandsire.genetics.HorseAttributes;
import com.josebieco.strideandsire.genetics.LineageTrait;
import com.josebieco.strideandsire.genetics.TraitState;
import com.josebieco.strideandsire.genetics.Zygosity;
import com.josebieco.strideandsire.menu.BreederSpyglassScreen;
import com.josebieco.strideandsire.menu.BreederSpyglassScreen.SpyglassReport;
import java.util.Map;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/**
 * Test instance: data/strideandsire/test_instance/genetics_spyglass_report_and_interaction.json
 */
public final class BreederSpyglassGameTest {
    private BreederSpyglassGameTest() {}

    /**
     * Task 7 Step 1: asserts {@link BreederSpyglassScreen#buildReport} returns the expected {@link Grade} for each of
     * the 3 attributes on a horse with known attribute values, reports {@link TraitState#LATENT} Sure-footed as
     * distinct from {@link TraitState#MANIFEST}, reports {@link CoatGene} {@link Zygosity} (including the unset
     * default state), and verifies shift+right-click with a Spyglass triggers the inspection without consuming the
     * Spyglass, damaging it, applying cooldown, or opening the horse inventory.
     */
    public static void buildReportAndInteraction(GameTestHelper helper) {
        helper.makeFloor();
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);

        Horse horse = helper.spawn(EntityTypes.HORSE, 1, 1, 1);
        horse.setTamed(true);
        horse.setAge(0);

        // Before setting any coat genetics, buildReport must default every CoatGene to HOMOZYGOUS_DOMINANT cleanly.
        SpyglassReport defaultReport = BreederSpyglassScreen.buildReport(horse);
        for (CoatGene gene : CoatGene.values()) {
            helper.assertValueEqual(
                defaultReport.coatZygosity(gene),
                Zygosity.HOMOZYGOUS_DOMINANT,
                "unset horse coatZygosity(" + gene + ") must default to HOMOZYGOUS_DOMINANT");
        }

        // Speed in Grade S (> 110% of vanillaMax), Jump in Grade B (upper quarter of vanilla range), Health in Grade E (vanillaMin).
        HorseAttributes.setBase(horse, GeneticAttribute.SPEED, HorseAttributeRanges.SPEED.vanillaMax() * 1.15D);
        HorseAttributes.setBase(horse, GeneticAttribute.JUMP, 0.90D);
        HorseAttributes.setBase(horse, GeneticAttribute.HEALTH, HorseAttributeRanges.HEALTH.vanillaMin());

        HorseGeneticsAccess.set(horse, HorseGeneticsAttachment.of(Map.of(
            LineageTrait.SURE_FOOTED, TraitState.LATENT,
            LineageTrait.FEARLESS, TraitState.MANIFEST,
            LineageTrait.DRAFT, TraitState.ABSENT
        )));
        HorseCoatGeneticsAccess.set(horse, HorseCoatGeneticsAttachment.EMPTY.withGenes(
            Map.of(
                CoatGene.OBSIDIAN, Zygosity.HOMOZYGOUS_RECESSIVE,
                CoatGene.SILVER_DAPPLE, Zygosity.HETEROZYGOUS,
                CoatGene.ALBINO, Zygosity.HOMOZYGOUS_DOMINANT),
            CoatGene.OBSIDIAN));

        SpyglassReport report = BreederSpyglassScreen.buildReport(horse);
        helper.assertTrue(report.speedGrade() == Grade.S, "expected Speed Grade S, got " + report.speedGrade());
        helper.assertTrue(report.jumpGrade() == Grade.B, "expected Jump Grade B, got " + report.jumpGrade());
        helper.assertTrue(report.healthGrade() == Grade.E, "expected Health Grade E, got " + report.healthGrade());
        helper.assertTrue(
            report.traitState(LineageTrait.SURE_FOOTED) == TraitState.LATENT
                && report.traitState(LineageTrait.SURE_FOOTED) != TraitState.MANIFEST,
            "expected SURE_FOOTED to be reported as LATENT (distinct from MANIFEST)"
        );
        helper.assertTrue(
            report.traitState(LineageTrait.FEARLESS) == TraitState.MANIFEST,
            "expected FEARLESS to be reported as MANIFEST"
        );
        helper.assertTrue(
            report.traitState(LineageTrait.DRAFT) == TraitState.ABSENT,
            "expected DRAFT to be reported as ABSENT"
        );
        helper.assertValueEqual(
            report.coatZygosity(CoatGene.OBSIDIAN),
            Zygosity.HOMOZYGOUS_RECESSIVE,
            "expected OBSIDIAN to be reported as HOMOZYGOUS_RECESSIVE");
        helper.assertValueEqual(
            report.coatZygosity(CoatGene.SILVER_DAPPLE),
            Zygosity.HETEROZYGOUS,
            "expected SILVER_DAPPLE to be reported as HETEROZYGOUS");
        helper.assertValueEqual(
            report.coatZygosity(CoatGene.ALBINO),
            Zygosity.HOMOZYGOUS_DOMINANT,
            "expected ALBINO to be reported as HOMOZYGOUS_DOMINANT");

        // Shift + right-click with Spyglass should inspect without consuming/cooldown/opening container menu.
        BreederSpyglassInteractionHandler.clearLastInspectedReport();
        ItemStack spyglass = new ItemStack(Items.SPYGLASS);
        player.setItemInHand(InteractionHand.MAIN_HAND, spyglass);
        player.setShiftKeyDown(true);
        player.interactOn(horse, InteractionHand.MAIN_HAND, horse.position());

        helper.assertTrue(
            BreederSpyglassInteractionHandler.getLastInspectedReport() != null
                && BreederSpyglassInteractionHandler.getLastInspectedReport().equals(report),
            "shift+right-click with Spyglass should produce matching SpyglassReport"
        );
        helper.assertTrue(
            player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1
                && player.getItemInHand(InteractionHand.MAIN_HAND).getDamageValue() == 0,
            "Spyglass should not be consumed or damaged"
        );
        helper.assertTrue(
            !player.getCooldowns().isOnCooldown(spyglass),
            "Spyglass should have no cooldown after inspecting a horse"
        );
        helper.assertTrue(
            player.containerMenu == player.inventoryMenu,
            "shift+right-click with Spyglass should not open the horse container menu"
        );
        helper.succeed();
    }
}
