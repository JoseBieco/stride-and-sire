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
 * each {@link CoatGene} independently has a {@value #WILD_CARRIER_CHANCE} (15%) chance of rolling
 * {@link Zygosity#HETEROZYGOUS} and 97% {@link Zygosity#HOMOZYGOUS_DOMINANT}. Wild horses never spawn
 * {@link Zygosity#HOMOZYGOUS_RECESSIVE}, so recessive coats can only be manifested through selective breeding.
 *
 * <p>"Wild" means the two natural paths: {@link EntitySpawnReason#NATURAL} (the periodic mob spawner) and
 * {@link EntitySpawnReason#CHUNK_GENERATION} (horses placed by {@code NaturalSpawner.spawnMobsForChunkGeneration}
 * when a chunk is generated, which is where most wild herds come from). Structure, spawner, breeding, spawn egg,
 * command and every other reason are excluded.
 *
 * <p>Hooks both {@link MobSpawnEvent.FinalizeSpawn} (fired by {@code ForgeEventFactory.onFinalizeSpawn}) and
 * {@link EntityJoinLevelEvent} (when {@code Mob.finalizeSpawn} was invoked directly with
 * {@link EntitySpawnReason#NATURAL}), guarded by a persistent {@code ForgeData} boolean flag so a horse is rolled at
 * most once across its entire lifetime (never re-rolled on chunk reload or dimension change). The chunk-generation
 * path only reaches {@code FinalizeSpawn}: the entity is created with reason {@code NATURAL} but is not in any level
 * yet (and may be on a worldgen thread), then joins later with {@code loadedFromDisk=true}. The roll only touches the
 * entity itself; {@code HorseCoatGeneticsAccess.set} sends no sync packet for an entity not yet added to a level, and
 * {@code StartTracking} syncs it once it is.
 */
public final class HorseCoatSpawnHandler {
    public static final double WILD_CARRIER_CHANCE = 0.15D;
    public static final String ROLLED_KEY = StrideAndSire.MODID + ":coat_genetics_rolled";

    private HorseCoatSpawnHandler() {}

    public static void register() {
        MobSpawnEvent.FinalizeSpawn.BUS.addListener(HorseCoatSpawnHandler::onFinalizeSpawn);
        EntityJoinLevelEvent.BUS.addListener(HorseCoatSpawnHandler::onEntityJoinLevel);
    }

    private static boolean onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (isWildSpawnReason(event.getSpawnReason())
                && event.getEntity() instanceof AbstractHorse horse) {
            rollWildCarriersOnce(horse);
        }
        return false;
    }

    private static boolean onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!event.loadedFromDisk()
                && event.getEntity() instanceof AbstractHorse horse
                && isWildSpawnReason(horse.getSpawnReason())) {
            rollWildCarriersOnce(horse);
        }
        return false;
    }

    /** The spawn reasons that count as a wild horse: natural spawning and chunk-generation spawning. */
    public static boolean isWildSpawnReason(EntitySpawnReason reason) {
        return reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION;
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
