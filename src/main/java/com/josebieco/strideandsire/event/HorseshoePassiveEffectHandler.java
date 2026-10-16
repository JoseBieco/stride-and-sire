package com.josebieco.strideandsire.event;

import com.josebieco.strideandsire.StrideAndSire;
import com.josebieco.strideandsire.attachment.HorseshoeAccess;
import com.josebieco.strideandsire.item.horseshoe.HorseshoeItem;
import com.josebieco.strideandsire.item.horseshoe.HorseshoeMaterial;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;

/**
 * Always-on horseshoe tier passives (spec section 4), all checked per entity against that horse's own horseshoe:
 * <ul>
 *   <li><b>Gold speed</b>: a transient {@link Attributes#MOVEMENT_SPEED} modifier {@link #GOLD_SPEED_MODIFIER_ID}.
 *       It is added/removed by {@link #refreshGoldSpeedModifier}, which {@link HorseshoeAccess#set} calls on every
 *       write (idempotent, so the menu's double write never stacks it), and re-applied when a horse joins a server
 *       level (transient modifiers are not saved, the horseshoe is).</li>
 *   <li><b>Gold Piglin neutrality</b>: a rider whose mount is gold-shod is treated by Piglins like a player wearing
 *       gold armor. Vanilla's gold check ({@code PiglinAi.isWearingSafeArmor}) only looks at the player's own armor
 *       slots and Forge's {@code IForgeItem.makesPiglinsNeutral} is per worn item, so neither reaches the mount. The
 *       hook is {@link LivingChangeTargetEvent} (fired by {@code StartAttacking} before a Piglin sets its attack
 *       target): we cancel exactly the "player not wearing gold" target choice, keeping vanilla's exceptions (Piglins
 *       already angry at that player, universal anger) intact. See {@link #shouldSuppressPiglinTarget}.</li>
 *   <li><b>Netherite fire immunity</b>: {@link LivingAttackEvent} (posted at the very start of
 *       {@code LivingEntity.hurtServer}, before any damage is applied) is cancelled for every
 *       {@code #minecraft:is_fire} source on a netherite-shod horse.</li>
 * </ul>
 */
public final class HorseshoePassiveEffectHandler {
    public static final Identifier GOLD_SPEED_MODIFIER_ID =
        Identifier.fromNamespaceAndPath(StrideAndSire.MODID, "gold_horseshoe_speed");
    /**
     * Balancing value (spec section 10): +10% of the horse's base movement speed
     * ({@link AttributeModifier.Operation#ADD_MULTIPLIED_BASE}). Kept modest on purpose: it stacks with the
     * horse's own random speed roll and with Speed potions.
     */
    public static final double GOLD_SPEED_BONUS = 0.1;
    private static final AttributeModifier GOLD_SPEED_MODIFIER =
        new AttributeModifier(GOLD_SPEED_MODIFIER_ID, GOLD_SPEED_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

    private HorseshoePassiveEffectHandler() {}

    public static void register() {
        EntityJoinLevelEvent.BUS.addListener(HorseshoePassiveEffectHandler::onJoinLevel);
        LivingAttackEvent.BUS.addListener(HorseshoePassiveEffectHandler::onLivingAttack);
        LivingChangeTargetEvent.BUS.addListener(HorseshoePassiveEffectHandler::onChangeTarget);
    }

    // ---- material lookup ---------------------------------------------------------------------------------------

    /** The material of the horse's own equipped horseshoe, if any. */
    private static Optional<HorseshoeMaterial> materialOf(AbstractHorse horse) {
        ItemStack shoe = HorseshoeAccess.get(horse);
        return shoe.getItem() instanceof HorseshoeItem item ? Optional.of(item.getMaterial()) : Optional.empty();
    }

    private static boolean hasGoldPassive(AbstractHorse horse) {
        return materialOf(horse).map(HorseshoeMaterial::hasGoldPassive).orElse(false);
    }

    private static boolean hasNetheritePassive(AbstractHorse horse) {
        return materialOf(horse).map(HorseshoeMaterial::hasNetheritePassive).orElse(false);
    }

    // ---- Gold: speed -------------------------------------------------------------------------------------------

    /**
     * Brings the Gold speed modifier in line with the horse's current horseshoe: adds it if the horseshoe has the
     * gold passive and it is absent, removes it otherwise. Idempotent; safe to call any number of times on either
     * side (the server's attribute sync and the client's horseshoe sync agree on the same modifier id).
     */
    public static void refreshGoldSpeedModifier(AbstractHorse horse) {
        AttributeInstance speed = horse.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        boolean wanted = hasGoldPassive(horse);
        boolean present = speed.hasModifier(GOLD_SPEED_MODIFIER_ID);
        if (wanted && !present) {
            speed.addTransientModifier(GOLD_SPEED_MODIFIER);
        } else if (!wanted && present) {
            speed.removeModifier(GOLD_SPEED_MODIFIER_ID);
        }
    }

    /** Transient modifiers are not persisted: restore it when a (re)loaded gold-shod horse enters a server level. */
    private static void onJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof AbstractHorse horse
                && HorseshoeAccess.isEligible(horse)) {
            refreshGoldSpeedModifier(horse);
        }
    }

    // ---- Gold: Piglin neutrality -------------------------------------------------------------------------------

    /** Whether {@code player} is riding a horse whose own horseshoe has the gold passive. */
    public static boolean mountMakesPiglinsNeutral(Player player) {
        return player.getVehicle() instanceof AbstractHorse horse && hasGoldPassive(horse);
    }

    /**
     * Whether a Piglin choosing {@code target} as its attack target should be stopped because the target is a
     * gold-mounted rider. Mirrors vanilla's gold-armor rule ({@code PiglinAi.findNearestValidAttackTarget}): gold
     * only prevents attack on sight, so a Piglin that is {@code ANGRY_AT} this player (attacked, opened a chest,
     * mined gold) or under universal anger still attacks. Piglin Brutes ignore gold in vanilla and are untouched.
     */
    public static boolean shouldSuppressPiglinTarget(LivingEntity attacker, LivingEntity target) {
        if (!(attacker instanceof Piglin piglin) || !(target instanceof Player player) || !mountMakesPiglinsNeutral(player)) {
            return false;
        }
        Brain<Piglin> brain = piglin.getBrain();
        Optional<Boolean> universalAnger = brain.getMemoryInternal(MemoryModuleType.UNIVERSAL_ANGER);
        if (universalAnger != null && universalAnger.isPresent()) {
            return false;
        }
        Optional<UUID> angryAt = brain.getMemoryInternal(MemoryModuleType.ANGRY_AT);
        return angryAt == null || angryAt.isEmpty() || !angryAt.get().equals(player.getUUID());
    }

    /** Cancelling keeps the Piglin's previous (empty) target; {@code StartAttacking} retries on later ticks. */
    private static boolean onChangeTarget(LivingChangeTargetEvent event) {
        return event.getTargetType() == LivingChangeTargetEvent.LivingTargetType.BEHAVIOR_TARGET
            && shouldSuppressPiglinTarget(event.getEntity(), event.getNewTarget());
    }

    // ---- Netherite: fire / lava immunity -----------------------------------------------------------------------

    /** Whether {@code source} is fire/lava damage ({@code #minecraft:is_fire}) to a netherite-shod horse. */
    public static boolean negatesFireDamage(LivingEntity entity, DamageSource source) {
        return entity instanceof AbstractHorse horse && source.is(DamageTypeTags.IS_FIRE) && hasNetheritePassive(horse);
    }

    private static boolean onLivingAttack(LivingAttackEvent event) {
        if (!negatesFireDamage(event.getEntity(), event.getSource())) {
            return false;
        }
        AbstractHorse horse = (AbstractHorse) event.getEntity();
        horse.clearFire(); // no endless burning visuals on an immune horse
        onFireDamageNegated(horse, event.getSource(), event.getAmount());
        return true;
    }

    /**
     * Called once per fire/lava damage instance negated by a Netherite horseshoe (server side). Note that
     * {@link LivingAttackEvent} fires before vanilla's hurt-invulnerability-frame check, i.e. every tick while
     * exposed; {@link HorseshoeDurabilityHandler} throttles the resulting wear.
     */
    private static void onFireDamageNegated(AbstractHorse horse, DamageSource source, float amount) {
        HorseshoeDurabilityHandler.consume(horse, HorseshoeDurabilityHandler.Trigger.FIRE_NEGATED);
    }
}
