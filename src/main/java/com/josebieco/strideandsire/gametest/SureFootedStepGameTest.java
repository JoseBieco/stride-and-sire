package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.attachment.HorseGeneticsAccess;
import com.josebieco.strideandsire.event.SureFootedStepHandler;
import com.josebieco.strideandsire.genetics.LineageTrait;
import com.josebieco.strideandsire.genetics.TraitState;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
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
     * Task 8 + controller ruling (total step height 1.5, only while a player controls the horse):
     * (a) a ridden MANIFEST Sure-footed horse climbs a 1.5-block ledge without jumping;
     * (b) an unridden MANIFEST horse does not (so it cannot leave a 1.5-block pen);
     * (c) a ridden LATENT horse does not;
     * (d) the step bonus is removed once the rider dismounts.
     */
    public static void manifestClimbsOnePointFiveBlockLedgeWhileLatentDoesNot(GameTestHelper helper) {
        helper.makeFloor();
        // Three lanes, each with a 2-wide 1.5-block ledge at x=3 (stone at y=1, bottom slab at y=2).
        int[] lanes = {1, 4, 7};
        for (int z0 : lanes) {
            for (int z = z0; z <= z0 + 1; z++) {
                helper.setBlock(new BlockPos(3, 1, z), Blocks.STONE.defaultBlockState());
                helper.setBlock(new BlockPos(3, 2, z), Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
            }
        }

        Horse riddenManifest = spawnSaddled(helper, lanes[0]);
        Horse unriddenManifest = spawnSaddled(helper, lanes[1]);
        Horse riddenLatent = spawnSaddled(helper, lanes[2]);
        HorseGeneticsAccess.set(riddenManifest, LineageTrait.SURE_FOOTED, TraitState.MANIFEST);
        HorseGeneticsAccess.set(unriddenManifest, LineageTrait.SURE_FOOTED, TraitState.MANIFEST);
        HorseGeneticsAccess.set(riddenLatent, LineageTrait.SURE_FOOTED, TraitState.LATENT);

        Player riderA = helper.makeMockPlayer(GameType.SURVIVAL);
        Player riderC = helper.makeMockPlayer(GameType.SURVIVAL);
        riderA.startRiding(riddenManifest, true, false);
        riderC.startRiding(riddenLatent, true, false);

        // Mount/dismount is picked up by the per-tick refresh.
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(Math.abs(riddenManifest.maxUpStep() - 1.5F) < 1e-6,
                "ridden MANIFEST Sure-footed horse should have total step height 1.5, got " + riddenManifest.maxUpStep());
            helper.assertTrue(unriddenManifest.maxUpStep() < 1.5F,
                "unridden MANIFEST horse should keep vanilla step height, got " + unriddenManifest.maxUpStep());
            helper.assertTrue(riddenLatent.maxUpStep() < 1.5F,
                "ridden LATENT horse should keep vanilla step height, got " + riddenLatent.maxUpStep());

            helper.assertTrue(stepRise(helper, riddenManifest, lanes[0]) >= 1.45D,
                "ridden MANIFEST Sure-footed horse should step up the 1.5-block ledge without jumping");
            helper.assertTrue(stepRise(helper, unriddenManifest, lanes[1]) < 0.5D,
                "unridden MANIFEST horse must not step up the 1.5-block ledge");
            helper.assertTrue(stepRise(helper, riddenLatent, lanes[2]) < 0.5D,
                "ridden LATENT horse must not step up the 1.5-block ledge");

            riderA.stopRiding();
            riderC.stopRiding();
        });
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(!riddenManifest.getAttribute(Attributes.STEP_HEIGHT)
                    .hasModifier(SureFootedStepHandler.SURE_FOOTED_STEP_MODIFIER_ID),
                "step bonus must be removed after the rider dismounts");
            helper.assertTrue(riddenManifest.maxUpStep() < 1.5F,
                "dismounted MANIFEST horse should be back to vanilla step height, got " + riddenManifest.maxUpStep());
            helper.succeed();
        });
    }

    private static Horse spawnSaddled(GameTestHelper helper, int laneZ) {
        Horse horse = helper.spawn(EntityTypes.HORSE, new Vec3(1.7, 1.0, laneZ + 1.0));
        horse.setTamed(true);
        horse.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        return horse;
    }

    /** Puts the horse back at its lane start and moves it 1 block into the ledge; returns how far it rose. */
    private static double stepRise(GameTestHelper helper, Horse horse, int laneZ) {
        Vec3 start = helper.absoluteVec(new Vec3(1.7, 1.0, laneZ + 1.0));
        horse.setPos(start);
        horse.setDeltaMovement(Vec3.ZERO);
        horse.setOnGround(true);
        horse.move(MoverType.SELF, new Vec3(1.0, 0.0, 0.0));
        return horse.getY() - start.y;
    }
}
