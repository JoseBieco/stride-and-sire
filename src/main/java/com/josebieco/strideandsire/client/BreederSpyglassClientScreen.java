package com.josebieco.strideandsire.client;

import com.josebieco.strideandsire.genetics.CoatGene;
import com.josebieco.strideandsire.genetics.GeneticAttribute;
import com.josebieco.strideandsire.genetics.Grade;
import com.josebieco.strideandsire.genetics.LineageTrait;
import com.josebieco.strideandsire.genetics.TraitState;
import com.josebieco.strideandsire.genetics.Zygosity;
import com.josebieco.strideandsire.menu.BreederSpyglassScreen;
import com.josebieco.strideandsire.menu.BreederSpyglassScreen.SpyglassReport;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

/**
 * Client-only read-only screen opener and implementation for the Breeder's Spyglass.
 * Displays qualitative attribute grades (E–S, never raw numeric values), lineage traits (Manifest vs. Latent), and
 * recessive coat gene zygosity (AA / Aa / aa).
 */
public final class BreederSpyglassClientScreen {
    /**
     * Wide enough that the longest state label in the value column (x0 + 118), pt_br "Latente (Portador)" / "AA (nenhum
     * traço)" (~98 px), keeps well over 4 px of margin to the right border.
     */
    private static final int PANEL_WIDTH = 232;
    private static final int PANEL_HEIGHT = 192;

    private BreederSpyglassClientScreen() {}

    public static void open(AbstractHorse horse) {
        Minecraft.getInstance().gui.setScreen(new Impl(horse));
    }

    private static final class Impl extends Screen {
        private final AbstractHorse horse;

        private Impl(AbstractHorse horse) {
            super(Component.translatable("gui.strideandsire.spyglass.title", horse.getDisplayName()));
            this.horse = horse;
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
            SpyglassReport report = BreederSpyglassScreen.buildReport(this.horse);

            int x0 = (this.width - PANEL_WIDTH) / 2;
            int y0 = (this.height - PANEL_HEIGHT) / 2;
            graphics.fill(x0, y0, x0 + PANEL_WIDTH, y0 + PANEL_HEIGHT, 0xCC101016);
            graphics.outline(x0, y0, PANEL_WIDTH, PANEL_HEIGHT, 0xFFD4AF37);

            int centerX = this.width / 2;
            int y = y0 + 10;
            graphics.centeredText(this.font, this.title, centerX, y, 0xFFFFE082);

            y += 18;
            graphics.text(this.font, Component.translatable("gui.strideandsire.spyglass.section.attributes")
                .withStyle(ChatFormatting.GOLD), x0 + 12, y, 0xFFFFAA00);

            y += 12;
            y = renderAttributeRow(graphics, x0 + 16, y, "gui.strideandsire.spyglass.attr.speed", report.gradeOf(GeneticAttribute.SPEED));
            y = renderAttributeRow(graphics, x0 + 16, y, "gui.strideandsire.spyglass.attr.jump", report.gradeOf(GeneticAttribute.JUMP));
            y = renderAttributeRow(graphics, x0 + 16, y, "gui.strideandsire.spyglass.attr.health", report.gradeOf(GeneticAttribute.HEALTH));

            y += 8;
            graphics.text(this.font, Component.translatable("gui.strideandsire.spyglass.section.traits")
                .withStyle(ChatFormatting.GOLD), x0 + 12, y, 0xFFFFAA00);

            y += 12;
            for (LineageTrait trait : LineageTrait.values()) {
                TraitState state = report.traitState(trait);
                Component traitLabel = Component.translatable(trait.translationKey());
                Component stateLabel = Component.translatable("gui.strideandsire.spyglass.state." + state.getSerializedName())
                    .withStyle(styleForState(state));
                graphics.text(this.font, traitLabel, x0 + 16, y, 0xFFE0E0E0);
                graphics.text(this.font, stateLabel, x0 + 118, y, 0xFFFFFFFF);
                y += 12;
            }

            y += 8;
            graphics.text(this.font, Component.translatable("gui.strideandsire.spyglass.section.coat_genetics")
                .withStyle(ChatFormatting.GOLD), x0 + 12, y, 0xFFFFAA00);

            y += 12;
            for (CoatGene gene : CoatGene.values()) {
                Zygosity zygosity = report.coatZygosity(gene);
                Component geneLabel = Component.translatable(gene.translationKey());
                Component zygosityLabel = Component.translatable("gui.strideandsire.spyglass.state." + zygosity.getSerializedName())
                    .withStyle(styleForZygosity(zygosity));
                graphics.text(this.font, geneLabel, x0 + 16, y, 0xFFE0E0E0);
                graphics.text(this.font, zygosityLabel, x0 + 118, y, 0xFFFFFFFF);
                y += 12;
            }
        }

        private int renderAttributeRow(GuiGraphicsExtractor graphics, int x, int y, String labelKey, Grade grade) {
            Component label = Component.translatable(labelKey);
            Component gradeBadge = Component.literal(grade.name()).withStyle(styleForGrade(grade));
            graphics.text(this.font, label, x, y, 0xFFE0E0E0);
            graphics.text(this.font, gradeBadge, x + 110, y, 0xFFFFFFFF);
            return y + 12;
        }
    }

    private static ChatFormatting styleForGrade(Grade grade) {
        return switch (grade) {
            case S -> ChatFormatting.LIGHT_PURPLE;
            case A -> ChatFormatting.AQUA;
            case B -> ChatFormatting.GREEN;
            case C -> ChatFormatting.YELLOW;
            case D -> ChatFormatting.GRAY;
            case E -> ChatFormatting.DARK_GRAY;
        };
    }

    private static ChatFormatting styleForState(TraitState state) {
        return switch (state) {
            case MANIFEST -> ChatFormatting.GREEN;
            case LATENT -> ChatFormatting.YELLOW;
            case ABSENT -> ChatFormatting.DARK_GRAY;
        };
    }

    private static ChatFormatting styleForZygosity(Zygosity zygosity) {
        return switch (zygosity) {
            case HOMOZYGOUS_RECESSIVE -> ChatFormatting.GREEN;
            case HETEROZYGOUS -> ChatFormatting.YELLOW;
            case HOMOZYGOUS_DOMINANT -> ChatFormatting.DARK_GRAY;
        };
    }
}
