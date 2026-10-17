package com.josebieco.strideandsire.event;

import com.josebieco.strideandsire.attachment.HorseCoatGeneticsAccess;
import com.josebieco.strideandsire.attachment.HorseCoatGeneticsAttachment;
import com.josebieco.strideandsire.genetics.CoatGene;
import com.josebieco.strideandsire.genetics.HorseCoatInheritance;
import com.josebieco.strideandsire.genetics.Zygosity;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.random.RandomGenerator;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;

/**
 * Hooks foal creation ({@link BabyEntitySpawnEvent}) to roll Mendelian inheritance for each {@link CoatGene} from
 * {@code parentA} and {@code parentB} and choose a fixed {@code manifestGeneToRender} tie-breaker when one or more
 * coat genes land on {@link Zygosity#HOMOZYGOUS_RECESSIVE} (Fase D).
 */
public final class HorseCoatBreedingHandler {
    private HorseCoatBreedingHandler() {}

    public static void register() {
        BabyEntitySpawnEvent.BUS.addListener(HorseCoatBreedingHandler::onBabyEntitySpawn);
    }

    /**
     * Applies Mendelian coat inheritance and tie-breaking to {@code child} from {@code parentA} and {@code parentB}.
     * No-op if {@code child} is not an eligible {@code Horse} (e.g. a Mule or Donkey foal).
     */
    public static void applyOffspringCoatGenetics(
            AbstractHorse parentA,
            AbstractHorse parentB,
            AbstractHorse child,
            RandomSource random) {
        if (!HorseCoatGeneticsAccess.isEligible(child)) {
            return;
        }
        HorseCoatGeneticsAttachment coatA = HorseCoatGeneticsAccess.get(parentA);
        HorseCoatGeneticsAttachment coatB = HorseCoatGeneticsAccess.get(parentB);
        RandomGenerator generator = HorseBreedingGeneticsHandler.adapt(random);

        EnumMap<CoatGene, Zygosity> childGenes = new EnumMap<>(CoatGene.class);
        EnumSet<CoatGene> manifestSet = EnumSet.noneOf(CoatGene.class);
        for (CoatGene gene : CoatGene.values()) {
            Zygosity childZygosity =
                HorseCoatInheritance.rollChildZygosity(coatA.zygosity(gene), coatB.zygosity(gene), generator);
            childGenes.put(gene, childZygosity);
            if (childZygosity.isManifest()) {
                manifestSet.add(gene);
            }
        }

        CoatGene manifestGeneToRender = manifestSet.isEmpty()
            ? null
            : HorseCoatInheritance.pickManifestGene(manifestSet, generator);
        HorseCoatGeneticsAccess.set(child, HorseCoatGeneticsAttachment.EMPTY.withGenes(childGenes, manifestGeneToRender));
    }

    private static boolean onBabyEntitySpawn(BabyEntitySpawnEvent event) {
        if (!(event.getParentA() instanceof AbstractHorse parentA)
                || !(event.getParentB() instanceof AbstractHorse parentB)
                || !(event.getChild() instanceof AbstractHorse child)
                || !HorseCoatGeneticsAccess.isEligible(child)) {
            return false;
        }
        applyOffspringCoatGenetics(parentA, parentB, child, parentA.getRandom());
        return false;
    }
}
