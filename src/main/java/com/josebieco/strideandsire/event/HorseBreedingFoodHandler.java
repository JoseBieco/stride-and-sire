package com.josebieco.strideandsire.event;

import com.josebieco.strideandsire.attachment.HorseshoeAccess;
import com.josebieco.strideandsire.item.genetics.BreedingDietItem;
import java.lang.reflect.Method;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * Makes {@link Items#GLISTERING_MELON_SLICE} a valid breeding-trigger food item for eligible horses, matching vanilla's
 * Golden Carrot handling in {@code AbstractHorse.handleEating} (heal, age up unless age-locked, love mode for a tamed
 * adult, temper for an untamed horse, eating sound + mouth animation + {@link GameEvent#EAT}) and recording
 * {@link BreedingDietItem#GLISTERING_MELON} for attribute inheritance.
 */
public final class HorseBreedingFoodHandler {
    private static final float GLISTERING_MELON_HEAL_AMOUNT = 4.0F;
    private static final int GLISTERING_MELON_AGE_UP_SECONDS = 60;
    private static final int GLISTERING_MELON_TEMPER = 3;
    private static final Method EATING_METHOD = eatingMethod();

    private HorseBreedingFoodHandler() {}

    public static void register() {
        PlayerInteractEvent.EntityInteractSpecific.BUS.addListener(HorseBreedingFoodHandler::onEntityInteractSpecific);
    }

    /**
     * Whether {@code horse} would accept a Glistering Melon Slice right now — the same conditions as
     * {@link #feedGlisteringMelon}. The client uses it to predict the server's answer (temper is server-only, so the
     * client assumes an untamed horse is below max temper).
     */
    public static boolean wouldEat(AbstractHorse horse) {
        if (horse.getHealth() < horse.getMaxHealth()) {
            return true;
        }
        if (horse.isBaby() && !horse.isAgeLocked()) {
            return true;
        }
        if (!horse.isBaby() && horse.isTamed() && horse.getAge() == 0 && !horse.isInLove()) {
            return true;
        }
        return !horse.isTamed() && (horse.level().isClientSide() || horse.getTemper() < horse.getMaxTemper());
    }

    /**
     * Feeds a {@link Items#GLISTERING_MELON_SLICE} to {@code horse} on the server, returning {@code true} if the horse
     * accepted the item (healed, aged up, entered love mode, or gained temper).
     */
    public static boolean feedGlisteringMelon(AbstractHorse horse, Player player, ItemStack stack) {
        if (!HorseshoeAccess.isEligible(horse) || !stack.is(Items.GLISTERING_MELON_SLICE) || !horse.isAlive()
                || horse.level().isClientSide()) {
            return false;
        }
        boolean ate = false;
        if (horse.getHealth() < horse.getMaxHealth()) {
            horse.heal(GLISTERING_MELON_HEAL_AMOUNT);
            ate = true;
        }
        if (horse.isBaby() && !horse.isAgeLocked()) {
            int speedUp = Math.max(GLISTERING_MELON_AGE_UP_SECONDS, AgeableMob.getSpeedUpSecondsWhenFeeding(-horse.getAge()));
            horse.ageUp(speedUp, true);
            ate = true;
        }
        if (!horse.isBaby() && horse.isTamed() && horse.getAge() == 0 && !horse.isInLove()) {
            ate = true;
            HorseBreedingGeneticsHandler.recordDietItem(horse, BreedingDietItem.GLISTERING_MELON);
            horse.setInLove(player);
        }
        if (!ate && !horse.isTamed() && horse.getTemper() < horse.getMaxTemper()) {
            horse.modifyTemper(GLISTERING_MELON_TEMPER);
            ate = true;
        }
        if (ate) {
            playEatingEffects(horse);
            horse.gameEvent(GameEvent.EAT, player);
            stack.consume(1, player);
        }
        return ate;
    }

    /**
     * Runs vanilla's private {@code AbstractHorse.eating()} — the per-type eating sound (horse / foal / donkey / mule;
     * none for undead horses) with vanilla's pitch jitter, plus the open-mouth animation — exactly as golden carrot
     * feeding does. It has no public or protected entry point, so it is invoked reflectively (26.x is unobfuscated, like
     * {@code StrideAndSireHorseMenu}'s access to {@code AbstractHorse.inventory}).
     */
    private static void playEatingEffects(AbstractHorse horse) {
        try {
            EATING_METHOD.invoke(horse);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot invoke AbstractHorse.eating()", e);
        }
    }

    private static Method eatingMethod() {
        try {
            Method method = AbstractHorse.class.getDeclaredMethod("eating");
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new IllegalStateException("Cannot access AbstractHorse.eating()", e);
        }
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
            if (!wouldEat(horse)) {
                return false;
            }
            if (horse.isBaby() && !horse.isAgeLocked()) {
                horse.level().addParticle(ParticleTypes.HAPPY_VILLAGER, horse.getRandomX(1.0), horse.getRandomY() + 0.5,
                    horse.getRandomZ(1.0), 0.0, 0.0, 0.0);
            }
            event.setCancellationResult(InteractionResult.SUCCESS);
            return true;
        }
        if (feedGlisteringMelon(horse, player, stack)) {
            event.setCancellationResult(InteractionResult.SUCCESS_SERVER);
            return true;
        }
        return false;
    }
}
