package com.josebieco.strideandsire.menu;

import com.josebieco.strideandsire.attachment.HorseGeneticsAccess;
import com.josebieco.strideandsire.attachment.HorseGeneticsAttachment;
import com.josebieco.strideandsire.genetics.GeneticAttribute;
import com.josebieco.strideandsire.genetics.Grade;
import com.josebieco.strideandsire.genetics.HorseAttributeRanges;
import com.josebieco.strideandsire.genetics.HorseAttributes;
import com.josebieco.strideandsire.genetics.LineageTrait;
import com.josebieco.strideandsire.genetics.TraitState;
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
     * Snapshot of a horse's qualitative attribute grades (D–S, never raw numbers) and lineage trait states
     * ({@link TraitState#ABSENT}, {@link TraitState#LATENT}, {@link TraitState#MANIFEST}).
     */
    public record SpyglassReport(
            Grade speedGrade,
            Grade jumpGrade,
            Grade healthGrade,
            Map<LineageTrait, TraitState> traits) {
        public SpyglassReport {
            Objects.requireNonNull(speedGrade, "speedGrade");
            Objects.requireNonNull(jumpGrade, "jumpGrade");
            Objects.requireNonNull(healthGrade, "healthGrade");
            Objects.requireNonNull(traits, "traits");
            EnumMap<LineageTrait, TraitState> copy = new EnumMap<>(LineageTrait.class);
            for (LineageTrait trait : LineageTrait.values()) {
                copy.put(trait, traits.getOrDefault(trait, TraitState.ABSENT));
            }
            traits = Collections.unmodifiableMap(copy);
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
    }

    /**
     * Builds the read-only inspection report for {@code horse} using {@link HorseAttributeRanges#gradeOf} and
     * {@link HorseGeneticsAccess#get}.
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
        return new SpyglassReport(speedGrade, jumpGrade, healthGrade, traits);
    }

    private static Grade gradeAttribute(AbstractHorse horse, GeneticAttribute attribute) {
        HorseAttributeRanges range = HorseAttributeRanges.of(attribute);
        double value = HorseAttributes.get(horse, attribute);
        return range.gradeOf(value);
    }
}
