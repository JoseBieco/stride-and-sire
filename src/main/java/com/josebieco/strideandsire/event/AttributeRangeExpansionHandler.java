package com.josebieco.strideandsire.event;

import com.josebieco.strideandsire.attachment.HorseshoeAccess;
import com.josebieco.strideandsire.genetics.GeneticAttribute;
import com.josebieco.strideandsire.genetics.HorseAttributeRanges;
import com.josebieco.strideandsire.genetics.HorseAttributes;
import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;

/**
 * Ensures the three genetic attributes ({@link Attributes#MOVEMENT_SPEED}, {@link Attributes#JUMP_STRENGTH},
 * {@link Attributes#MAX_HEALTH}) on eligible {@link AbstractHorse} subtypes accept base values up to the +20%
 * expanded ceiling ({@link HorseAttributeRanges#ceiling()}), while clamping any out-of-range base value on join
 * to that ceiling.
 */
public final class AttributeRangeExpansionHandler {
    private static final List<EntityType<? extends AbstractHorse>> ELIGIBLE_TYPES = List.of(
        EntityTypes.HORSE, EntityTypes.DONKEY, EntityTypes.MULE, EntityTypes.SKELETON_HORSE, EntityTypes.ZOMBIE_HORSE);

    private AttributeRangeExpansionHandler() {}

    public static void register() {
        EntityAttributeModificationEvent.BUS.addListener(AttributeRangeExpansionHandler::onAttributeModification);
        EntityJoinLevelEvent.BUS.addListener(AttributeRangeExpansionHandler::onEntityJoinLevel);
    }

    /** Returns the range definition for one of the three horse genetic attributes. */
    public static HorseAttributeRanges rangeFor(Holder<Attribute> attribute) {
        if (attribute.equals(Attributes.MOVEMENT_SPEED)) {
            return HorseAttributeRanges.SPEED;
        }
        if (attribute.equals(Attributes.JUMP_STRENGTH)) {
            return HorseAttributeRanges.JUMP;
        }
        if (attribute.equals(Attributes.MAX_HEALTH)) {
            return HorseAttributeRanges.HEALTH;
        }
        throw new IllegalArgumentException("Not a horse genetic attribute: " + attribute);
    }

    public static double vanillaMin(Holder<Attribute> attribute) {
        return rangeFor(attribute).vanillaMin();
    }

    public static double vanillaMax(Holder<Attribute> attribute) {
        return rangeFor(attribute).vanillaMax();
    }

    public static double expandedMax(Holder<Attribute> attribute) {
        return rangeFor(attribute).ceiling();
    }

    /** Clamps a horse's base genetic attributes to the expanded ceiling [0, 1.2 * vanillaMax]. */
    public static void clampToExpandedCeiling(AbstractHorse horse) {
        if (!HorseshoeAccess.isEligible(horse)) {
            return;
        }
        for (GeneticAttribute geneticAttribute : GeneticAttribute.values()) {
            HorseAttributeRanges range = HorseAttributeRanges.of(geneticAttribute);
            double current = HorseAttributes.get(horse, geneticAttribute);
            if (current > range.ceiling()) {
                HorseAttributes.setBase(horse, geneticAttribute, range.ceiling());
            } else if (current < 0.0) {
                HorseAttributes.setBase(horse, geneticAttribute, 0.0);
            }
        }
    }

    private static void onAttributeModification(EntityAttributeModificationEvent event) {
        for (EntityType<? extends AbstractHorse> type : ELIGIBLE_TYPES) {
            for (Holder<Attribute> holder : List.of(Attributes.MOVEMENT_SPEED, Attributes.JUMP_STRENGTH, Attributes.MAX_HEALTH)) {
                if (!event.has(type, holder)) {
                    event.add(type, holder);
                }
                ensureAttributeCeiling(holder, expandedMax(holder));
            }
        }
    }

    private static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof AbstractHorse horse) {
            clampToExpandedCeiling(horse);
        }
    }

    private static void ensureAttributeCeiling(Holder<Attribute> holder, double requiredMax) {
        if (holder.value() instanceof RangedAttribute ranged && ranged.getMaxValue() < requiredMax) {
            try {
                Field maxField = RangedAttribute.class.getDeclaredField("maxValue");
                maxField.setAccessible(true);
                maxField.setDouble(ranged, requiredMax);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Cannot expand maxValue of " + holder, e);
            }
        }
    }
}
