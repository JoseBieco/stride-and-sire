package com.josebieco.strideandsire.genetics;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

/** Maps {@link GeneticAttribute} to the real horse attribute instances (base values). */
public final class HorseAttributes {
    private HorseAttributes() {}

    private static AttributeInstance instance(AbstractHorse horse, GeneticAttribute a) {
        return horse.getAttribute(switch (a) {
            case SPEED -> Attributes.MOVEMENT_SPEED;
            case JUMP -> Attributes.JUMP_STRENGTH;
            case HEALTH -> Attributes.MAX_HEALTH;
        });
    }

    public static double get(AbstractHorse horse, GeneticAttribute a) {
        return instance(horse, a).getBaseValue();
    }

    public static void setBase(AbstractHorse horse, GeneticAttribute a, double value) {
        instance(horse, a).setBaseValue(value);
        if (a == GeneticAttribute.HEALTH && horse.getHealth() > horse.getMaxHealth()) {
            horse.setHealth(horse.getMaxHealth());
        }
    }
}
