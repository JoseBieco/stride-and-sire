package com.josebieco.strideandsire.menu;

import com.josebieco.strideandsire.attachment.HorseCoatGeneticsAccess;
import com.josebieco.strideandsire.attachment.HorseCoatGeneticsAttachment;
import com.josebieco.strideandsire.attachment.HorseGeneticsAccess;
import com.josebieco.strideandsire.attachment.HorseGeneticsAttachment;
import com.josebieco.strideandsire.genetics.CoatGene;
import com.josebieco.strideandsire.genetics.GeneticAttribute;
import com.josebieco.strideandsire.genetics.Grade;
import com.josebieco.strideandsire.genetics.HorseAttributeRanges;
import com.josebieco.strideandsire.genetics.HorseAttributes;
import com.josebieco.strideandsire.genetics.LineageTrait;
import com.josebieco.strideandsire.genetics.TraitState;
import com.josebieco.strideandsire.genetics.Zygosity;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

/**
 * Read-only Breeder's Spyglass inspection model and report builder.
 *
 * <p>Kept free of client-only GUI classes at the class-header level so {@link #buildReport(AbstractHorse)} can be
 * invoked directly inside dedicated-server GameTests. Client rendering lives in
 * {@link com.josebieco.strideandsire.client.BreederSpyglassClientScreen}.
 */
public final class BreederSpyglassScreen {
    private BreederSpyglassScreen() {}

    /**
     * Snapshot of a horse's qualitative attribute grades (E–S, never raw numbers), lineage trait states
     * ({@link TraitState#ABSENT}, {@link TraitState#LATENT}, {@link TraitState#MANIFEST}), and recessive coat gene
     * zygosity ({@link Zygosity#HOMOZYGOUS_DOMINANT}, {@link Zygosity#HETEROZYGOUS},
     * {@link Zygosity#HOMOZYGOUS_RECESSIVE}).
     */
    public record SpyglassReport(
            Grade speedGrade,
            Grade jumpGrade,
            Grade healthGrade,
            Map<LineageTrait, TraitState> traits,
            Map<CoatGene, Zygosity> coatGenes) {
        public SpyglassReport {
            Objects.requireNonNull(speedGrade, "speedGrade");
            Objects.requireNonNull(jumpGrade, "jumpGrade");
            Objects.requireNonNull(healthGrade, "healthGrade");
            Objects.requireNonNull(traits, "traits");
            Objects.requireNonNull(coatGenes, "coatGenes");
            EnumMap<LineageTrait, TraitState> traitCopy = new EnumMap<>(LineageTrait.class);
            for (LineageTrait trait : LineageTrait.values()) {
                TraitState state = traits.getOrDefault(trait, TraitState.ABSENT);
                traitCopy.put(trait, state == null ? TraitState.ABSENT : state);
            }
            traits = Collections.unmodifiableMap(traitCopy);

            EnumMap<CoatGene, Zygosity> coatCopy = new EnumMap<>(CoatGene.class);
            for (CoatGene gene : CoatGene.values()) {
                Zygosity zygosity = coatGenes.getOrDefault(gene, Zygosity.HOMOZYGOUS_DOMINANT);
                coatCopy.put(gene, zygosity == null ? Zygosity.HOMOZYGOUS_DOMINANT : zygosity);
            }
            coatGenes = Collections.unmodifiableMap(coatCopy);
        }

        public SpyglassReport(
                Grade speedGrade,
                Grade jumpGrade,
                Grade healthGrade,
                Map<LineageTrait, TraitState> traits) {
            this(speedGrade, jumpGrade, healthGrade, traits, Map.of());
        }

        public Grade gradeOf(GeneticAttribute attribute) {
            return switch (attribute) {
                case SPEED -> speedGrade;
                case JUMP -> jumpGrade;
                case HEALTH -> healthGrade;
            };
        }

        public TraitState traitState(LineageTrait trait) {
            return traits.getOrDefault(trait, TraitState.ABSENT);
        }

        public Zygosity coatZygosity(CoatGene gene) {
            return coatGenes.getOrDefault(gene, Zygosity.HOMOZYGOUS_DOMINANT);
        }
    }

    /**
     * Builds the read-only inspection report for {@code horse} using {@link HorseAttributeRanges#gradeOf},
     * {@link HorseGeneticsAccess#get}, and {@link HorseCoatGeneticsAccess#get}.
     */
    public static SpyglassReport buildReport(AbstractHorse horse) {
        Objects.requireNonNull(horse, "horse");
        Grade speedGrade = gradeAttribute(horse, GeneticAttribute.SPEED);
        Grade jumpGrade = gradeAttribute(horse, GeneticAttribute.JUMP);
        Grade healthGrade = gradeAttribute(horse, GeneticAttribute.HEALTH);

        HorseGeneticsAttachment genetics = HorseGeneticsAccess.get(horse);
        EnumMap<LineageTrait, TraitState> traits = new EnumMap<>(LineageTrait.class);
        for (LineageTrait trait : LineageTrait.values()) {
            traits.put(trait, genetics.state(trait));
        }

        HorseCoatGeneticsAttachment coatGenetics = HorseCoatGeneticsAccess.get(horse);
        EnumMap<CoatGene, Zygosity> coatGenes = new EnumMap<>(CoatGene.class);
        for (CoatGene gene : CoatGene.values()) {
            coatGenes.put(gene, coatGenetics.zygosity(gene));
        }
        return new SpyglassReport(speedGrade, jumpGrade, healthGrade, traits, coatGenes);
    }

    private static Grade gradeAttribute(AbstractHorse horse, GeneticAttribute attribute) {
        HorseAttributeRanges range = HorseAttributeRanges.of(attribute);
        double value = HorseAttributes.get(horse, attribute);
        return range.gradeOf(value);
    }
}
