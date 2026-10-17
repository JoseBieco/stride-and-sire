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
import net.minecraftforge.event.entity.EntityJoinLevelEvent;

/**
 * Raises a horse's auto-step height ({@link Attributes#STEP_HEIGHT}) by +1.0 block (from vanilla 1.0 to 2.0 blocks,
 * allowing smooth 1.5–2 block climbs without jumping) whenever {@link LineageTrait#SURE_FOOTED} is
 * {@link com.josebieco.strideandsire.genetics.TraitState#MANIFEST}. Horses with the trait {@code LATENT} or
 * {@code ABSENT} keep vanilla step height.
 */
public final class SureFootedStepHandler {
    public static final Identifier SURE_FOOTED_STEP_MODIFIER_ID =
        Identifier.fromNamespaceAndPath(StrideAndSire.MODID, "sure_footed_step");
    public static final double SURE_FOOTED_STEP_BONUS = 1.0D;
    private static final AttributeModifier SURE_FOOTED_STEP_MODIFIER =
        new AttributeModifier(SURE_FOOTED_STEP_MODIFIER_ID, SURE_FOOTED_STEP_BONUS, AttributeModifier.Operation.ADD_VALUE);

    private SureFootedStepHandler() {}

    public static void register() {
        EntityJoinLevelEvent.BUS.addListener(SureFootedStepHandler::onJoinLevel);
    }

    /**
     * Synchronizes the transient {@link Attributes#STEP_HEIGHT} modifier with the horse's current
     * {@link LineageTrait#SURE_FOOTED} state. Idempotent and safe to call on either logical side.
     */
    public static void refreshStepHeight(AbstractHorse horse) {
        AttributeInstance stepHeight = horse.getAttribute(Attributes.STEP_HEIGHT);
        if (stepHeight == null) {
            return;
        }
        boolean wanted = HorseGeneticsAccess.hasManifest(horse, LineageTrait.SURE_FOOTED);
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
}
