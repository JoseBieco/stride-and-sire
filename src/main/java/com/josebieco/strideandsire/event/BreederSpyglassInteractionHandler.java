package com.josebieco.strideandsire.event;

import com.josebieco.strideandsire.attachment.HorseshoeAccess;
import com.josebieco.strideandsire.client.BreederSpyglassClientScreen;
import com.josebieco.strideandsire.menu.BreederSpyglassScreen;
import com.josebieco.strideandsire.menu.BreederSpyglassScreen.SpyglassReport;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.jspecify.annotations.Nullable;

/**
 * Shift + right-clicking an eligible horse with a vanilla {@link Items#SPYGLASS} opens the read-only
 * {@link BreederSpyglassScreen} without consuming the item or applying any durability/cooldown.
 */
public final class BreederSpyglassInteractionHandler {
    private static volatile @Nullable SpyglassReport lastInspectedReport;

    private BreederSpyglassInteractionHandler() {}

    public static void register() {
        PlayerInteractEvent.EntityInteractSpecific.BUS.addListener(BreederSpyglassInteractionHandler::onEntityInteractSpecific);
    }

    /** Test seam: returns the most recent report produced by a shift+right-click Spyglass inspection. */
    public static @Nullable SpyglassReport getLastInspectedReport() {
        return lastInspectedReport;
    }

    /** Clears the recorded inspection report (used by GameTests). */
    public static void clearLastInspectedReport() {
        lastInspectedReport = null;
    }

    private static boolean onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getTarget() instanceof AbstractHorse horse)
                || !HorseshoeAccess.isEligible(horse)
                || !horse.isAlive()) {
            return false;
        }
        Player player = event.getEntity();
        if (!player.isSecondaryUseActive() || player.isSpectator()) {
            return false;
        }
        ItemStack held = player.getItemInHand(event.getHand());
        if (!held.is(Items.SPYGLASS)) {
            return false;
        }
        lastInspectedReport = BreederSpyglassScreen.buildReport(horse);
        if (event.getLevel().isClientSide() && FMLEnvironment.dist == Dist.CLIENT) {
            BreederSpyglassClientScreen.open(horse);
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        return true;
    }
}
