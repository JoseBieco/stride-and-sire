package com.josebieco.strideandsire.event;

import com.josebieco.strideandsire.advancement.BredPureCoatTrigger;
import com.josebieco.strideandsire.attachment.HorseCoatGeneticsAccess;
import com.josebieco.strideandsire.attachment.HorseCoatGeneticsAttachment;
import com.josebieco.strideandsire.genetics.CoatGene;
import com.josebieco.strideandsire.genetics.HorseCoatInheritance;
import com.josebieco.strideandsire.genetics.Zygosity;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;
import java.util.random.RandomGenerator;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import org.jspecify.annotations.Nullable;

/**
 * Hooks foal creation ({@link BabyEntitySpawnEvent}) to roll Mendelian inheritance for each {@link CoatGene} from
 * {@code parentA} and {@code parentB} and choose a fixed {@code manifestGeneToRender} tie-breaker when one or more
 * coat genes land on {@link Zygosity#HOMOZYGOUS_RECESSIVE} (Fase D).
 *
 * <p>Two listeners: the genetics roll at normal priority (like the Fase C attribute/trait roll), and the
 * {@link BredPureCoatTrigger} grant in a cancellation-aware monitor listener (the very last to run), so the
 * advancement is only granted for a foal that is actually born: not when a later listener cancelled the event, nor
 * when it replaced the child we rolled. The grant goes to the player who caused the breeding
 * ({@link BabyEntitySpawnEvent#getCausedByPlayer()}, the love cause, like vanilla {@code BRED_ANIMALS}), falling back
 * to the parents' online owner when there is none.
 */
public final class HorseCoatBreedingHandler {
    /** The roll of the event currently being posted, read back by the monitor listener of the same post. */
    private record PendingBirth(BabyEntitySpawnEvent event, AbstractHorse child, Set<CoatGene> manifestGenes) {}

    private static @Nullable PendingBirth pending;

    private HorseCoatBreedingHandler() {}

    public static void register() {
        BabyEntitySpawnEvent.BUS.addListener(HorseCoatBreedingHandler::onBabyEntitySpawn);
        BabyEntitySpawnEvent.BUS.addListener(HorseCoatBreedingHandler::onBabyEntitySpawnMonitor);
    }

    /**
     * Applies Mendelian coat inheritance and tie-breaking to {@code child} from {@code parentA} and {@code parentB}.
     * No-op (empty result) if {@code child} is not an eligible {@code Horse}.
     *
     * @return the genes the child rolled {@link Zygosity#HOMOZYGOUS_RECESSIVE} on (possibly empty)
     */
    public static Set<CoatGene> applyOffspringCoatGenetics(
            AbstractHorse parentA,
            AbstractHorse parentB,
            AbstractHorse child,
            RandomSource random) {
        if (!HorseCoatGeneticsAccess.isEligible(child)) {
            return Set.of();
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
        return manifestSet;
    }

    /**
     * Fires {@link BredPureCoatTrigger} for every gene in {@code manifestGenes}, for {@code breeder} (the love cause)
     * or, when there is none, the first online {@link ServerPlayer} owner of {@code parentA} / {@code parentB}.
     */
    public static void grantPureCoatAdvancements(
            @Nullable Player breeder,
            AbstractHorse parentA,
            AbstractHorse parentB,
            Set<CoatGene> manifestGenes) {
        if (manifestGenes.isEmpty()) {
            return;
        }
        ServerPlayer recipient = breeder instanceof ServerPlayer sp ? sp : resolveOwnerPlayer(parentA, parentB);
        if (recipient == null) {
            return;
        }
        for (CoatGene gene : manifestGenes) {
            BredPureCoatTrigger.trigger(recipient, gene);
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
        pending = null;
        if (!(event.getParentA() instanceof AbstractHorse parentA)
                || !(event.getParentB() instanceof AbstractHorse parentB)
                || !(event.getChild() instanceof AbstractHorse child)
                || !HorseCoatGeneticsAccess.isEligible(child)) {
            return false;
        }
        Set<CoatGene> manifest = applyOffspringCoatGenetics(parentA, parentB, child, parentA.getRandom());
        if (!manifest.isEmpty()) {
            pending = new PendingBirth(event, child, manifest);
        }
        return false;
    }

    /** Monitor (runs last, sees cancellation): grant only if the foal we rolled is the one that will be born. */
    private static void onBabyEntitySpawnMonitor(BabyEntitySpawnEvent event, boolean cancelled) {
        PendingBirth birth = pending;
        if (birth == null || birth.event() != event) {
            return;
        }
        pending = null;
        if (cancelled || event.getChild() != birth.child()) {
            return;
        }
        grantPureCoatAdvancements(event.getCausedByPlayer(), (AbstractHorse) event.getParentA(),
            (AbstractHorse) event.getParentB(), birth.manifestGenes());
    }
}
