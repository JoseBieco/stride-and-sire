package com.josebieco.strideandsire.event;

import com.josebieco.strideandsire.StrideAndSire;
import com.josebieco.strideandsire.attachment.HorseGeneticsAccess;
import com.josebieco.strideandsire.attachment.HorseshoeAccess;
import com.josebieco.strideandsire.genetics.LineageTrait;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;

/**
 * Casco Firme (Sure-footed): raises a horse's auto-step height ({@link Attributes#STEP_HEIGHT}) by
 * {@value #SURE_FOOTED_STEP_BONUS} block — from vanilla's 1.0 to a total of 1.5 — while
 * {@link LineageTrait#SURE_FOOTED} is {@code MANIFEST} <b>and</b> a player is the horse's controlling passenger.
 * Unridden horses keep vanilla step height, so a Sure-footed horse cannot walk out of a 1.5-block fenced pen.
 *
 * <p>The transient modifier is idempotent and refreshed on both logical sides: from {@code HorseGeneticsAccess.set},
 * on level join, and every tick (which picks up riders mounting/dismounting within one tick).
 */
public final class SureFootedStepHandler {
    public static final Identifier SURE_FOOTED_STEP_MODIFIER_ID =
        Identifier.fromNamespaceAndPath(StrideAndSire.MODID, "sure_footed_step");
    public static final double SURE_FOOTED_STEP_BONUS = 0.5D;
    private static final AttributeModifier SURE_FOOTED_STEP_MODIFIER =
        new AttributeModifier(SURE_FOOTED_STEP_MODIFIER_ID, SURE_FOOTED_STEP_BONUS, AttributeModifier.Operation.ADD_VALUE);

    private SureFootedStepHandler() {}

    public static void register() {
        EntityJoinLevelEvent.BUS.addListener(SureFootedStepHandler::onJoinLevel);
        LivingEvent.LivingTickEvent.BUS.addListener(SureFootedStepHandler::onLivingTick);
    }

    /** True when the step bonus should apply: Sure-footed MANIFEST and ridden (controlled) by a player. */
    public static boolean shouldHaveBonus(AbstractHorse horse) {
        return HorseGeneticsAccess.hasManifest(horse, LineageTrait.SURE_FOOTED)
            && horse.getControllingPassenger() instanceof Player;
    }

    /**
     * Synchronizes the transient {@link Attributes#STEP_HEIGHT} modifier with {@link #shouldHaveBonus}. Idempotent and
     * safe to call on either logical side.
     */
    public static void refreshStepHeight(AbstractHorse horse) {
        AttributeInstance stepHeight = horse.getAttribute(Attributes.STEP_HEIGHT);
        if (stepHeight == null) {
            return;
        }
        boolean wanted = shouldHaveBonus(horse);
        boolean present = stepHeight.hasModifier(SURE_FOOTED_STEP_MODIFIER_ID);
        if (wanted && !present) {
            stepHeight.addTransientModifier(SURE_FOOTED_STEP_MODIFIER);
        } else if (!wanted && present) {
            stepHeight.removeModifier(SURE_FOOTED_STEP_MODIFIER_ID);
        }
    }

    private static void onJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof AbstractHorse horse && HorseshoeAccess.isEligible(horse)) {
            refreshStepHeight(horse);
        }
    }

    private static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof AbstractHorse horse && HorseshoeAccess.isEligible(horse)) {
            refreshStepHeight(horse);
        }
    }
}
