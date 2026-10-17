package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.attachment.HorseCoatGeneticsAccess;
import com.josebieco.strideandsire.attachment.HorseCoatGeneticsAttachment;
import com.josebieco.strideandsire.genetics.CoatGene;
import com.josebieco.strideandsire.genetics.Zygosity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.phys.Vec3;

/**
 * Test instances: data/strideandsire/test_instance/coat_genetics_wild_spawn_*.json
 */
public final class HorseCoatSpawnGameTest {
    private HorseCoatSpawnGameTest() {}

    /**
     * Spawns 150 wild Horses through the natural spawn path ({@link EntitySpawnReason#NATURAL}, {@code finalizeSpawn},
     * {@code addFreshEntity}) and verifies that:
     * <ul>
     *   <li>No gene ever rolls {@link Zygosity#HOMOZYGOUS_RECESSIVE} on a wild horse.</li>
     *   <li>{@link HorseCoatGeneticsAttachment#manifestGeneToRender()} is always {@code null}.</li>
     *   <li>At least one {@link Zygosity#HETEROZYGOUS} carrier occurs across the sample (450 gene rolls at 3% each).</li>
     * </ul>
     */
    public static void wildSpawnNeverManifests(GameTestHelper helper) {
        helper.makeFloor();
        ServerLevel level = helper.getLevel();
        BlockPos spawnPos = helper.absolutePos(new BlockPos(2, 1, 2));
        Vec3 spawnVec = helper.absoluteVec(new Vec3(2.5, 1.0, 2.5));

        int carrierCount = 0;
        for (int i = 0; i < 150; i++) {
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
            "Expected at least one HETEROZYGOUS carrier across 150 wild horses (450 gene rolls at 3%), got 0");
        helper.succeed();
    }
}
