package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.attachment.HorseGeneticsAccess;
import com.josebieco.strideandsire.genetics.LineageTrait;
import com.josebieco.strideandsire.genetics.TraitState;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;

/**
 * Test instance: data/strideandsire/test_instance/genetics_sure_footed_step_manifest_vs_latent.json
 */
public final class SureFootedStepGameTest {
    private SureFootedStepGameTest() {}

    /**
     * Task 8 Step 1: (a) a MANIFEST Sure-footed horse climbs a 1.5-block ledge without jumping;
     * (b) a LATENT Sure-footed horse does not.
     */
    public static void manifestClimbsOnePointFiveBlockLedgeWhileLatentDoesNot(GameTestHelper helper) {
        helper.makeFloor();

        // Build a 1.5-block ledge along X=2 (stone block at Y=1, bottom stone slab at Y=2) for Z=1 and Z=3.
        for (int z : new int[]{1, 3}) {
            helper.setBlock(new BlockPos(2, 1, z), Blocks.STONE.defaultBlockState());
            helper.setBlock(
                new BlockPos(2, 2, z),
                Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM)
            );
        }

        Horse manifestHorse = helper.spawn(EntityTypes.HORSE, new Vec3(1.2, 1.0, 1.5));
        Horse latentHorse = helper.spawn(EntityTypes.HORSE, new Vec3(1.2, 1.0, 3.5));
        manifestHorse.setTamed(true);
        latentHorse.setTamed(true);

        HorseGeneticsAccess.set(manifestHorse, LineageTrait.SURE_FOOTED, TraitState.MANIFEST);
        HorseGeneticsAccess.set(latentHorse, LineageTrait.SURE_FOOTED, TraitState.LATENT);

        helper.assertTrue(
            manifestHorse.maxUpStep() >= 1.5F,
            "MANIFEST Sure-footed horse should have maxUpStep >= 1.5, got " + manifestHorse.maxUpStep()
        );
        helper.assertTrue(
            latentHorse.maxUpStep() < 1.5F,
            "LATENT Sure-footed horse should keep vanilla maxUpStep < 1.5, got " + latentHorse.maxUpStep()
        );

        double startY = manifestHorse.getY();
        manifestHorse.setOnGround(true);
        manifestHorse.move(MoverType.SELF, new Vec3(1.0, 0.0, 0.0));

        latentHorse.setOnGround(true);
        latentHorse.move(MoverType.SELF, new Vec3(1.0, 0.0, 0.0));

        double manifestRise = manifestHorse.getY() - startY;
        double latentRise = latentHorse.getY() - startY;

        helper.assertTrue(
            manifestRise >= 1.45D,
            "MANIFEST Sure-footed horse should step up the 1.5-block ledge without jumping, rose by " + manifestRise
        );
        helper.assertTrue(
            latentRise < 0.5D,
            "LATENT Sure-footed horse should not step up the 1.5-block ledge, rose by " + latentRise
        );
        helper.succeed();
    }
}
