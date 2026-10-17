package com.josebieco.strideandsire.menu;

import com.josebieco.strideandsire.attachment.HorseshoeAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * Opens {@link StrideAndSireHorseMenu} instead of vanilla's horse inventory for eligible horses.
 *
 * <p>Vanilla opens the horse inventory through {@code AbstractHorse.openCustomInventoryScreen}, which calls
 * {@code ServerPlayer.openHorseInventory}; neither fires a Forge event, so the two player-facing entry points are
 * intercepted before they reach it:
 * <ul>
 *   <li>sneak + right-click on a tamed adult horse, with any item vanilla would not use first
 *       ({@code AbstractHorse.mobInteract}): handled
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
     * True exactly when vanilla's interaction would reach {@code AbstractHorse.mobInteract}'s
     * {@code openCustomInventoryScreen}: a tamed adult horse without passengers, sneak-clicked (secondary use) by a
     * non-spectator, whatever the hand holds ({@code Horse}/{@code AbstractChestedHorse}/{@code ZombieHorse.mobInteract}
     * skip their item handling on that path), and nothing that {@code Mob.interact} runs before {@code mobInteract}
     * would consume the click. Those earlier interactions are left to vanilla:
     * <ul>
     *   <li>{@code Mob.checkAndHandleImportantInteractions}: a named name tag, a spawn egg of this horse's type;</li>
     *   <li>{@code Entity.interact}: tying the player's leashed mobs to this horse, shears cutting its leash
     *       connections, untying a horse leashed to the player, and leashing it with a lead.</li>
     * </ul>
     */
    private static boolean opensInventoryOnInteract(ServerPlayer player, AbstractHorse horse, PlayerInteractEvent.EntityInteractSpecific event) {
        if (!HorseshoeAccess.isEligible(horse) || player.isSpectator() || !horse.isAlive()
                || !player.isSecondaryUseActive() || !horse.isTamed() || horse.isVehicle() || horse.isBaby()) {
            return false;
        }
        ItemStack held = player.getItemInHand(event.getHand());
        if (held.is(Items.SPYGLASS)) {
            return false;
        }
        // Mob.checkAndHandleImportantInteractions
        if (held.is(Items.NAME_TAG) && held.get(DataComponents.CUSTOM_NAME) != null && horse.getType().canSerialize()) {
            return false;
        }
        if (held.getItem() instanceof SpawnEggItem && SpawnEggItem.spawnsEntity(held, horse.getType())) {
            return false;
        }
        // Entity.interact: sneak-click ties the player's leashed mobs to this horse.
        if (horse.canBeLeashed() && !Leashable.leashableInArea(horse,
                leashable -> leashable.getLeashHolder() == player && leashable.canHaveALeashAttachedTo(horse)).isEmpty()) {
            return false;
        }
        // Entity.interact: shears cut the horse's own leash and any leash tied to it.
        if (held.canPerformAction(ToolActions.SHEARS_HARVEST)
                && (horse.isLeashed() || !Leashable.leashableLeashedTo(horse).isEmpty())) {
            return false;
        }
        // Entity.interact: clicking a horse leashed to the player unties it; a lead leashes it to the player.
        if (horse.getLeashHolder() == player) {
            return false;
        }
        return !(held.is(Items.LEAD) && !(horse.getLeashHolder() instanceof Player) && horse.canHaveALeashAttachedTo(player));
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
            new SimpleMenuProvider((containerId, inventory, p) -> new StrideAndSireHorseMenu(containerId, inventory, horse), horse.getDisplayName()),
            data -> StrideAndSireHorseMenu.writeOpenData(data, horse));
    }
}
