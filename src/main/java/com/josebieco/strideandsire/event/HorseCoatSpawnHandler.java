package com.josebieco.strideandsire.event;

import com.josebieco.strideandsire.StrideAndSire;
import com.josebieco.strideandsire.attachment.HorseCoatGeneticsAccess;
import com.josebieco.strideandsire.attachment.HorseCoatGeneticsAttachment;
import com.josebieco.strideandsire.genetics.CoatGene;
import com.josebieco.strideandsire.genetics.Zygosity;
import java.util.EnumMap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;

/**
 * Rolls rare recessive {@link CoatGene} carriers on naturally spawned wild {@code Horse} entities (Fase D):
 * each {@link CoatGene} independently has a {@value #WILD_CARRIER_CHANCE} (3%) chance of rolling
 * {@link Zygosity#HETEROZYGOUS} and 97% {@link Zygosity#HOMOZYGOUS_DOMINANT}. Wild horses never spawn
 * {@link Zygosity#HOMOZYGOUS_RECESSIVE}, so recessive coats can only be manifested through selective breeding.
 *
 * <p>Hooks both {@link MobSpawnEvent.FinalizeSpawn} (fired by {@code ForgeEventFactory.onFinalizeSpawn}) and
 * {@link EntityJoinLevelEvent} (when {@code Mob.finalizeSpawn} was invoked directly with
 * {@link EntitySpawnReason#NATURAL}), guarded by a persistent {@code ForgeData} boolean flag so a horse is rolled at
 * most once across its entire lifetime (never re-rolled on chunk reload or dimension change).
 */
public final class HorseCoatSpawnHandler {
    public static final double WILD_CARRIER_CHANCE = 0.03D;
    static final String ROLLED_KEY = StrideAndSire.MODID + ":coat_genetics_rolled";

    private HorseCoatSpawnHandler() {}

    public static void register() {
        MobSpawnEvent.FinalizeSpawn.BUS.addListener(HorseCoatSpawnHandler::onFinalizeSpawn);
        EntityJoinLevelEvent.BUS.addListener(HorseCoatSpawnHandler::onEntityJoinLevel);
    }

    private static boolean onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (event.getSpawnReason() == EntitySpawnReason.NATURAL
                && event.getEntity() instanceof AbstractHorse horse) {
            rollWildCarriersOnce(horse);
        }
        return false;
    }

    private static boolean onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!event.loadedFromDisk()
                && event.getEntity() instanceof AbstractHorse horse
                && horse.getSpawnReason() == EntitySpawnReason.NATURAL) {
            rollWildCarriersOnce(horse);
        }
        return false;
    }

    private static void rollWildCarriersOnce(AbstractHorse horse) {
        if (horse.level().isClientSide() || !HorseCoatGeneticsAccess.isEligible(horse)) {
            return;
        }
        if (horse.getPersistentData().getBooleanOr(ROLLED_KEY, false)) {
            return;
        }
        horse.getPersistentData().putBoolean(ROLLED_KEY, true);

        RandomSource random = horse.getRandom();
        EnumMap<CoatGene, Zygosity> genes = new EnumMap<>(CoatGene.class);
        for (CoatGene gene : CoatGene.values()) {
            Zygosity z = random.nextDouble() < WILD_CARRIER_CHANCE
                ? Zygosity.HETEROZYGOUS
                : Zygosity.HOMOZYGOUS_DOMINANT;
            genes.put(gene, z);
        }
        HorseCoatGeneticsAccess.set(horse, HorseCoatGeneticsAttachment.EMPTY.withGenes(genes, null));
    }
}
