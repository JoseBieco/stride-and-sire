package com.josebieco.strideandsire.item.genetics;

import com.josebieco.strideandsire.genetics.GeneticAttribute;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * Diet items that can trigger guided horse breeding and the attribute each favors (spec section 4):
 * <ul>
 *   <li>{@link #GOLDEN_CARROT} &rarr; {@link Attributes#MOVEMENT_SPEED}</li>
 *   <li>{@link #GOLDEN_APPLE} &rarr; {@link Attributes#MAX_HEALTH}</li>
 *   <li>{@link #GLISTERING_MELON} &rarr; {@link Attributes#JUMP_STRENGTH}</li>
 *   <li>{@link #NONE} &rarr; unguided breeding</li>
 * </ul>
 */
public enum BreedingDietItem {
    NONE(null),
    GOLDEN_CARROT(GeneticAttribute.SPEED),
    GOLDEN_APPLE(GeneticAttribute.HEALTH),
    GLISTERING_MELON(GeneticAttribute.JUMP);

    private final @Nullable GeneticAttribute favoredGeneticAttribute;

    BreedingDietItem(@Nullable GeneticAttribute favoredGeneticAttribute) {
        this.favoredGeneticAttribute = favoredGeneticAttribute;
    }

    public @Nullable GeneticAttribute favoredGeneticAttribute() {
        return favoredGeneticAttribute;
    }

    public @Nullable Holder<Attribute> favoredAttribute() {
        if (favoredGeneticAttribute == null) {
            return null;
        }
        return switch (favoredGeneticAttribute) {
            case SPEED -> Attributes.MOVEMENT_SPEED;
            case JUMP -> Attributes.JUMP_STRENGTH;
            case HEALTH -> Attributes.MAX_HEALTH;
        };
    }

    public boolean isDietGuided() {
        return this != NONE;
    }

    public boolean favors(GeneticAttribute attribute) {
        return favoredGeneticAttribute == attribute;
    }

    public boolean favors(Holder<Attribute> attribute) {
        Holder<Attribute> favored = favoredAttribute();
        return favored != null && favored.equals(attribute);
    }

    public static BreedingDietItem fromStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return NONE;
        }
        if (stack.is(Items.GOLDEN_CARROT)) {
            return GOLDEN_CARROT;
        }
        if (stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
            return GOLDEN_APPLE;
        }
        if (stack.is(Items.GLISTERING_MELON_SLICE)) {
            return GLISTERING_MELON;
        }
        return NONE;
    }
}
