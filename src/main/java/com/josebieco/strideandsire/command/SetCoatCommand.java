package com.josebieco.strideandsire.command;

import com.josebieco.strideandsire.attachment.HorseCoatGeneticsAccess;
import com.josebieco.strideandsire.attachment.HorseCoatGeneticsAttachment;
import com.josebieco.strideandsire.genetics.CoatGene;
import com.josebieco.strideandsire.genetics.HorseCoatInheritance;
import com.josebieco.strideandsire.genetics.Zygosity;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraftforge.event.RegisterCommandsEvent;

/**
 * Debug/creative command: forces the Fase D coat genotype of one or more Horses, bypassing normal spawn/breeding
 * rolls. Restricted to operators (permission level 2), same as vanilla cheat commands like {@code /give}.
 *
 * <p>Syntax: {@code /strideandsire setcoat <targets> <obsidian> <albino> <silver_dapple>}, each genotype argument
 * being exactly {@code AA}, {@code Aa}, or {@code aa} (real genetics notation, case-sensitive).
 */
public final class SetCoatCommand {
    private static final DynamicCommandExceptionType INVALID_GENOTYPE = new DynamicCommandExceptionType(
        token -> Component.literal("Invalid genotype '" + token + "': expected AA, Aa, or aa"));

    private static final String[] GENOTYPE_TOKENS = {"AA", "Aa", "aa"};
    private static final SuggestionProvider<CommandSourceStack> GENOTYPE_SUGGESTIONS =
        (context, builder) -> SharedSuggestionProvider.suggest(GENOTYPE_TOKENS, builder);

    private SetCoatCommand() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("strideandsire")
                .then(Commands.literal("setcoat")
                    .requires(source -> source.hasPermission(2))
                    .then(targetsArgument())));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, ?> targetsArgument() {
        return Commands.argument("targets", EntityArgument.entities())
            .then(genotypeArgument("obsidian")
                .then(genotypeArgument("albino")
                    .then(genotypeArgument("silver_dapple").executes(SetCoatCommand::run))));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> genotypeArgument(String name) {
        return Commands.argument(name, StringArgumentType.word())
            .suggests(GENOTYPE_SUGGESTIONS);
    }

    /** Maps exactly {@code "AA"}, {@code "Aa"}, {@code "aa"} to their {@link Zygosity}; any other token is rejected. */
    public static Zygosity parseZygosity(String token) throws CommandSyntaxException {
        return switch (token) {
            case "AA" -> Zygosity.HOMOZYGOUS_DOMINANT;
            case "Aa" -> Zygosity.HETEROZYGOUS;
            case "aa" -> Zygosity.HOMOZYGOUS_RECESSIVE;
            default -> throw INVALID_GENOTYPE.create(token);
        };
    }

    /**
     * Applies the given genotype to {@code horse} if it is eligible for coat genetics ({@link CoatGene} is Horse-only),
     * picking a random {@link CoatGene#values()} tie-break among manifest genes exactly like natural birth does.
     * Returns {@code true} if applied, {@code false} if {@code horse} was ineligible (a no-op).
     */
    public static boolean applyToHorse(
            AbstractHorse horse, Zygosity obsidian, Zygosity albino, Zygosity silverDapple, RandomGenerator random) {
        if (!HorseCoatGeneticsAccess.isEligible(horse)) {
            return false;
        }
        Map<CoatGene, Zygosity> genes = new EnumMap<>(CoatGene.class);
        genes.put(CoatGene.OBSIDIAN, obsidian);
        genes.put(CoatGene.ALBINO, albino);
        genes.put(CoatGene.SILVER_DAPPLE, silverDapple);

        Set<CoatGene> manifest = HorseCoatGeneticsAttachment.EMPTY.withGenes(genes, null).manifestGenes();
        CoatGene renderGene = manifest.isEmpty() ? null : HorseCoatInheritance.pickManifestGene(manifest, random);
        HorseCoatGeneticsAccess.set(horse, HorseCoatGeneticsAttachment.EMPTY.withGenes(genes, renderGene));
        return true;
    }

    private static int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<? extends Entity> targets = EntityArgument.getEntities(context, "targets");
        Zygosity obsidian = parseZygosity(StringArgumentType.getString(context, "obsidian"));
        Zygosity albino = parseZygosity(StringArgumentType.getString(context, "albino"));
        Zygosity silverDapple =
            parseZygosity(StringArgumentType.getString(context, "silver_dapple"));

        RandomGenerator random = adapt(context.getSource().getLevel().getRandom());
        int applied = 0;
        int skipped = 0;
        for (Entity target : targets) {
            if (target instanceof AbstractHorse horse && applyToHorse(horse, obsidian, albino, silverDapple, random)) {
                applied++;
            } else {
                skipped++;
            }
        }

        CommandSourceStack source = context.getSource();
        if (applied > 0) {
            source.sendSuccess(() -> Component.literal(
                "Set coat genotype on " + applied + " horse(s)"
                    + (skipped > 0 ? " (" + skipped + " non-Horse target(s) skipped)" : "")), true);
        } else {
            source.sendFailure(Component.literal("No eligible Horse found among the given targets"));
        }
        return applied;
    }

    /** Adapts Minecraft's {@link RandomSource} to the JDK abstraction {@link HorseCoatInheritance} takes. */
    private static RandomGenerator adapt(RandomSource random) {
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                return random.nextLong();
            }

            @Override
            public double nextDouble() {
                return random.nextDouble();
            }
        };
    }
}
