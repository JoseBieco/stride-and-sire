package com.josebieco.strideandsire.event;

import com.josebieco.strideandsire.attachment.HorseshoeAccess;
import com.josebieco.strideandsire.item.genetics.BreedingDietItem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * Makes {@link Items#GLISTERING_MELON_SLICE} a valid breeding-trigger food item for eligible horses, matching vanilla
 * Golden Carrot / Golden Apple behavior and recording {@link BreedingDietItem#GLISTERING_MELON} for attribute
 * inheritance.
 */
public final class HorseBreedingFoodHandler {
    private static final float GLISTERING_MELON_HEAL_AMOUNT = 4.0F;
    private static final int GLISTERING_MELON_AGE_UP_SECONDS = 60;
    private static final int GLISTERING_MELON_TEMPER = 3;

    private HorseBreedingFoodHandler() {}

    public static void register() {
        PlayerInteractEvent.EntityInteractSpecific.BUS.addListener(HorseBreedingFoodHandler::onEntityInteractSpecific);
    }

    /**
     * Feeds a {@link Items#GLISTERING_MELON_SLICE} to {@code horse} on the server, returning {@code true} if the horse
     * accepted the item (healed, aged up, entered love mode, or gained temper).
     */
    public static boolean feedGlisteringMelon(AbstractHorse horse, Player player, ItemStack stack) {
        if (!HorseshoeAccess.isEligible(horse) || !stack.is(Items.GLISTERING_MELON_SLICE) || !horse.isAlive()) {
            return false;
        }
        boolean ate = false;
        if (horse.getHealth() < horse.getMaxHealth()) {
            horse.heal(GLISTERING_MELON_HEAL_AMOUNT);
            ate = true;
        }
        if (horse.isBaby()) {
            int speedUp = Math.max(GLISTERING_MELON_AGE_UP_SECONDS, AgeableMob.getSpeedUpSecondsWhenFeeding(-horse.getAge()));
            horse.ageUp(speedUp, true);
            ate = true;
        }
        if (!horse.isBaby() && horse.isTamed() && horse.getAge() == 0 && !horse.isInLove()) {
            ate = true;
            HorseBreedingGeneticsHandler.recordDietItem(horse, BreedingDietItem.GLISTERING_MELON);
            horse.setInLove(player);
        }
        if (!ate && !horse.isTamed() && horse.getTemper() < horse.getMaxTemper() && !horse.level().isClientSide()) {
            horse.modifyTemper(GLISTERING_MELON_TEMPER);
            ate = true;
        }
        if (ate) {
            horse.gameEvent(GameEvent.EAT, player);
            stack.consume(1, player);
        }
        return ate;
    }

    private static boolean onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getTarget() instanceof AbstractHorse horse) || !HorseshoeAccess.isEligible(horse)) {
            return false;
        }
        Player player = event.getEntity();
        if (!horse.isAlive() || horse.isVehicle() || (!horse.isBaby() && horse.isTamed() && player.isSecondaryUseActive())) {
            return false;
        }
        ItemStack stack = player.getItemInHand(event.getHand());
        if (!stack.is(Items.GLISTERING_MELON_SLICE)) {
            return false;
        }
        if (event.getLevel().isClientSide()) {
            event.setCancellationResult(InteractionResult.CONSUME);
            return true;
        }
        if (feedGlisteringMelon(horse, player, stack)) {
            event.setCancellationResult(InteractionResult.SUCCESS_SERVER);
            return true;
        }
        return false;
    }
}
