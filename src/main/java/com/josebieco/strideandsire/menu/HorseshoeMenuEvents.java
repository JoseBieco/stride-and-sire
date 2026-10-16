package com.josebieco.strideandsire.menu;

import com.josebieco.strideandsire.attachment.HorseshoeAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * Opens {@link HorseshoeHorseMenu} instead of vanilla's horse inventory for eligible horses.
 *
 * <p>Vanilla opens the horse inventory through {@code AbstractHorse.openCustomInventoryScreen}, which calls
 * {@code ServerPlayer.openHorseInventory}; neither fires a Forge event, so the two player-facing entry points are
 * intercepted before they reach it:
 * <ul>
 *   <li>sneak + right-click with an empty hand on a tamed adult horse ({@code AbstractHorse.mobInteract}): handled
 *       here via {@link PlayerInteractEvent.EntityInteractSpecific} on the server;</li>
 *   <li>the inventory key while riding ({@code ServerboundPlayerCommandPacket.OPEN_INVENTORY}): the client consumes
 *       the key press and sends {@link com.josebieco.strideandsire.network.OpenHorseshoeMenuPacket} instead, which
 *       ends in {@link #openFromVehicle}.</li>
 * </ul>
 */
public final class HorseshoeMenuEvents {
    private HorseshoeMenuEvents() {}

    public static void register() {
        PlayerInteractEvent.EntityInteractSpecific.BUS.addListener(HorseshoeMenuEvents::onEntityInteract);
    }

    /** Returns true (cancel) when it opened the horseshoe menu in place of vanilla's {@code mobInteract} path. */
    private static boolean onEntityInteract(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getTarget() instanceof AbstractHorse horse)
                || !opensInventoryOnInteract(player, horse, event)) {
            return false;
        }
        open(player, horse);
        horse.gameEvent(GameEvent.ENTITY_INTERACT, player); // what Mob.interact emits for a consumed mobInteract
        event.setCancellationResult(InteractionResult.SUCCESS);
        return true;
    }

    /**
     * True exactly when vanilla's interaction would reach {@code openCustomInventoryScreen} and open the inventory, for
     * an empty hand: nothing earlier in {@code Mob.interact}/{@code Entity.interact} (leash handling) would act first.
     */
    private static boolean opensInventoryOnInteract(ServerPlayer player, AbstractHorse horse, PlayerInteractEvent.EntityInteractSpecific event) {
        if (!HorseshoeAccess.isEligible(horse) || player.isSpectator() || !horse.isAlive()
                || !player.getItemInHand(event.getHand()).isEmpty() || !player.isSecondaryUseActive()
                || !horse.isTamed() || horse.isVehicle() || horse.isBaby()) {
            return false;
        }
        // Entity.interact: sneak-click ties the player's leashed mobs to this horse, and clicking a horse leashed to
        // the player unties it. Leave both to vanilla.
        if (horse.getLeashHolder() == player) {
            return false;
        }
        return !horse.canBeLeashed() || Leashable.leashableInArea(horse,
            leashable -> leashable.getLeashHolder() == player && leashable.canHaveALeashAttachedTo(horse)).isEmpty();
    }

    /** Vanilla's riding path ({@code openCustomInventoryScreen}): only the rider of a tamed horse may open it. */
    public static void openFromVehicle(ServerPlayer player) {
        if (player.getVehicle() instanceof AbstractHorse horse && HorseshoeAccess.isEligible(horse)) {
            if (horse.hasPassenger(player) && horse.isTamed()) {
                open(player, horse);
            }
        } else if (player.getVehicle() instanceof HasCustomInventoryScreen vehicle) {
            vehicle.openCustomInventoryScreen(player); // not ours: exactly what vanilla's OPEN_INVENTORY does
        }
    }

    /** Opens the horseshoe menu for {@code horse}, like {@code ServerPlayer.openHorseInventory} does for vanilla's. */
    public static void open(ServerPlayer player, AbstractHorse horse) {
        player.openMenu(
            new SimpleMenuProvider((containerId, inventory, p) -> new HorseshoeHorseMenu(containerId, inventory, horse), horse.getDisplayName()),
            data -> HorseshoeHorseMenu.writeOpenData(data, horse));
    }
}
