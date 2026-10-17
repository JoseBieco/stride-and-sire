package com.josebieco.strideandsire.event;

import com.josebieco.strideandsire.advancement.BredPureCoatTrigger;
import com.josebieco.strideandsire.attachment.HorseCoatGeneticsAccess;
import com.josebieco.strideandsire.attachment.HorseCoatGeneticsAttachment;
import com.josebieco.strideandsire.genetics.CoatGene;
import com.josebieco.strideandsire.genetics.HorseCoatInheritance;
import com.josebieco.strideandsire.genetics.Zygosity;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.random.RandomGenerator;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import org.jspecify.annotations.Nullable;

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
     * Applies Mendelian coat inheritance and tie-breaking to {@code child} from {@code parentA} and {@code parentB},
     * and fires {@link BredPureCoatTrigger} for the owning {@link ServerPlayer} on any gene that rolls
     * {@link Zygosity#HOMOZYGOUS_RECESSIVE}. No-op if {@code child} is not an eligible {@code Horse}.
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

        if (!manifestSet.isEmpty()) {
            ServerPlayer owner = resolveOwnerPlayer(parentA, parentB);
            if (owner != null) {
                for (CoatGene gene : manifestSet) {
                    BredPureCoatTrigger.trigger(owner, gene);
                }
            }
        }
    }

    private static @Nullable ServerPlayer resolveOwnerPlayer(AbstractHorse parentA, AbstractHorse parentB) {
        if (parentA.getOwner() instanceof ServerPlayer sp) {
            return sp;
        }
        if (parentB.getOwner() instanceof ServerPlayer sp) {
            return sp;
        }
        return null;
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
