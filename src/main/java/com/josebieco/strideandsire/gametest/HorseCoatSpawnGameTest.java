package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.attachment.HorseCoatGeneticsAccess;
import com.josebieco.strideandsire.attachment.HorseCoatGeneticsAttachment;
import com.josebieco.strideandsire.event.HorseCoatSpawnHandler;
import com.josebieco.strideandsire.genetics.CoatGene;
import com.josebieco.strideandsire.genetics.Zygosity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;

/**
 * Test instances: data/strideandsire/test_instance/coat_genetics_wild_spawn_*.json
 */
public final class HorseCoatSpawnGameTest {
    /** 200 horses x 3 genes = 600 rolls at 3%: P(no carrier) = 0.97^600 ~= 1.1e-8 (< 1e-6 budget). */
    private static final int WILD_SAMPLE = 200;

    private HorseCoatSpawnGameTest() {}

    /**
     * Spawns 200 wild Horses through the natural spawn path ({@link EntitySpawnReason#NATURAL}, {@code finalizeSpawn},
     * {@code addFreshEntity}) and verifies that:
     * <ul>
     *   <li>No gene ever rolls {@link Zygosity#HOMOZYGOUS_RECESSIVE} on a wild horse.</li>
     *   <li>{@link HorseCoatGeneticsAttachment#manifestGeneToRender()} is always {@code null}.</li>
     *   <li>At least one {@link Zygosity#HETEROZYGOUS} carrier occurs across the sample (600 gene rolls at 3% each; false failure
     *       0.97^600 ~= 1.1e-8).</li>
     * </ul>
     */
    public static void wildSpawnNeverManifests(GameTestHelper helper) {
        helper.makeFloor();
        ServerLevel level = helper.getLevel();
        BlockPos spawnPos = helper.absolutePos(new BlockPos(2, 1, 2));
        Vec3 spawnVec = helper.absoluteVec(new Vec3(2.5, 1.0, 2.5));

        int carrierCount = 0;
        for (int i = 0; i < WILD_SAMPLE; i++) {
            Horse wild = EntityTypes.HORSE.create(level, EntitySpawnReason.NATURAL);
            helper.assertTrue(wild != null, "could not create wild Horse");
            wild.snapTo(spawnVec.x, spawnVec.y, spawnVec.z, 0.0F, 0.0F);
            wild.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), EntitySpawnReason.NATURAL, null);
            helper.assertTrue(level.addFreshEntity(wild), "wild Horse should be added to the level");

            HorseCoatGeneticsAttachment coat = HorseCoatGeneticsAccess.get(wild);
            helper.assertTrue(
                coat.manifestGeneToRender() == null,
                "wild Horse must never have a manifestGeneToRender, got " + coat.manifestGeneToRender());
            for (CoatGene gene : CoatGene.values()) {
                Zygosity z = coat.zygosity(gene);
                helper.assertTrue(
                    z != Zygosity.HOMOZYGOUS_RECESSIVE,
                    "wild Horse must never spawn HOMOZYGOUS_RECESSIVE for " + gene);
                if (z == Zygosity.HETEROZYGOUS) {
                    carrierCount++;
                }
            }
            wild.discard();
        }

        helper.assertTrue(
            carrierCount > 0,
            "Expected at least one HETEROZYGOUS carrier across " + WILD_SAMPLE + " wild horses (3% per gene roll), got 0");
        helper.succeed();
    }

    /**
     * World-generation wild horses: {@code NaturalSpawner.spawnMobsForChunkGeneration} creates the horse with
     * {@link EntitySpawnReason#NATURAL}, posts {@code FinalizeSpawn} with {@link EntitySpawnReason#CHUNK_GENERATION}
     * before the entity is in any level (possibly off the main thread), and the horse later joins with
     * {@code loadedFromDisk=true}. Verifies that path rolls carriers (statistically, 200 horses: 0.97^600 ~= 1.1e-8),
     * sets the persistent roll guard, never syncs/throws for the not-yet-added entity, never manifests, and is never
     * re-rolled when the horse is then added to the level. Also verifies excluded reasons (structure, spawner,
     * breeding, spawn egg, command) do not roll.
     */
    public static void chunkGenerationSpawnRollsCarriers(GameTestHelper helper) {
        helper.makeFloor();
        ServerLevel level = helper.getLevel();
        BlockPos spawnPos = helper.absolutePos(new BlockPos(2, 1, 2));
        Vec3 spawnVec = helper.absoluteVec(new Vec3(2.5, 1.0, 2.5));

        int carrierCount = 0;
        for (int i = 0; i < WILD_SAMPLE; i++) {
            Horse wild = EntityTypes.HORSE.create(level, EntitySpawnReason.NATURAL);
            helper.assertTrue(wild != null, "could not create world-gen Horse");
            wild.snapTo(spawnVec.x, spawnVec.y, spawnVec.z, 0.0F, 0.0F);
            ForgeEventFactory.onFinalizeSpawn(wild, level, level.getCurrentDifficultyAt(spawnPos), EntitySpawnReason.CHUNK_GENERATION, null);
            helper.assertFalse(wild.isAddedToLevel(), "world-gen horse must not be in the level during FinalizeSpawn");
            helper.assertTrue(
                wild.getPersistentData().getBooleanOr(HorseCoatSpawnHandler.ROLLED_KEY, false),
                "CHUNK_GENERATION FinalizeSpawn must set the persistent roll guard");
            HorseCoatGeneticsAttachment rolled = HorseCoatGeneticsAccess.get(wild);
            helper.assertTrue(rolled.manifestGeneToRender() == null, "world-gen horse must never manifest");
            for (CoatGene gene : CoatGene.values()) {
                Zygosity z = rolled.zygosity(gene);
                helper.assertTrue(z != Zygosity.HOMOZYGOUS_RECESSIVE, "world-gen horse must never be aa for " + gene);
                if (z == Zygosity.HETEROZYGOUS) {
                    carrierCount++;
                }
            }
            if (i < 5) {
                helper.assertTrue(level.addFreshEntity(wild), "world-gen Horse should be added to the level");
                helper.assertValueEqual(HorseCoatGeneticsAccess.get(wild), rolled,
                    "joining the level after a CHUNK_GENERATION roll must not re-roll");
            }
            wild.discard();
        }
        helper.assertTrue(carrierCount > 0,
            "Expected at least one HETEROZYGOUS carrier across " + WILD_SAMPLE + " world-gen horses, got 0");

        for (EntitySpawnReason excluded : java.util.List.of(EntitySpawnReason.STRUCTURE, EntitySpawnReason.SPAWNER,
                EntitySpawnReason.BREEDING, EntitySpawnReason.SPAWN_ITEM_USE, EntitySpawnReason.COMMAND)) {
            Horse horse = EntityTypes.HORSE.create(level, excluded);
            helper.assertTrue(horse != null, "could not create Horse for " + excluded);
            horse.snapTo(spawnVec.x, spawnVec.y, spawnVec.z, 0.0F, 0.0F);
            ForgeEventFactory.onFinalizeSpawn(horse, level, level.getCurrentDifficultyAt(spawnPos), excluded, null);
            helper.assertFalse(
                horse.getPersistentData().getBooleanOr(HorseCoatSpawnHandler.ROLLED_KEY, false),
                excluded + " spawns must not roll wild carriers");
            horse.discard();
        }
        helper.succeed();
    }
}
