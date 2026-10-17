package com.josebieco.strideandsire.event;

import com.josebieco.strideandsire.attachment.SaddlebagAccess;
import com.josebieco.strideandsire.item.saddlebag.SaddlebagItem;
import com.josebieco.strideandsire.item.saddlebag.SaddlebagMaterial;
import java.util.List;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.ItemStack;

/**
 * Saddlebag tier bonuses (spec section 3). Stateless: every decision is made from the arguments (one horse's own bag
 * and its own death source), so nothing can leak between horses dying in the same tick.
 *
 * <p><b>Contents on death</b> ({@link #deathDrop}, called from the equipment death-drop path): an explosion death
 * ({@code #minecraft:is_explosion}) clears the container unless the material
 * {@link SaddlebagMaterial#protectsContentsFromExplosion() protects from explosions} (Iron, Diamond, Gold, Netherite); a
 * fire/lava death ({@code #minecraft:is_fire}) clears it unless the material
 * {@link SaddlebagMaterial#protectsContentsFromFire() protects from fire} (Netherite). Any other cause keeps the
 * contents. The bag itself always drops.
 *
 * <p>The dropped bag item is not hit by the explosion that killed the horse: {@code ServerExplosion.hurtEntities}
 * snapshots the affected entity list before damaging anything, and the drop is spawned during that loop. The
 * Netherite bag's dropped item is fire/lava-proof through its {@code fireResistant()} item property.
 *
 * <p><b>Gold Piglin passive</b>: {@link #hasGoldPiglinPassive} feeds
 * {@link HorseshoePassiveEffectHandler#mountMakesPiglinsNeutral}.
 */
public final class SaddlebagBonusEffectHandler {
    private SaddlebagBonusEffectHandler() {}

    /** Whether a bag of {@code material} loses its contents when its horse is killed by {@code source}. */
    public static boolean shouldClearContents(SaddlebagMaterial material, DamageSource source) {
        if (source.is(DamageTypeTags.IS_EXPLOSION) && !material.protectsContentsFromExplosion()) {
            return true;
        }
        return source.is(DamageTypeTags.IS_FIRE) && !material.protectsContentsFromFire();
    }

    /**
     * The stack to drop for {@code bag} when its horse dies from {@code source}: {@code bag} itself, with its
     * container emptied if {@link #shouldClearContents}. Safe for {@link ItemStack#EMPTY}, non-bags and empty bags.
     */
    public static ItemStack deathDrop(ItemStack bag, DamageSource source) {
        if (bag.getItem() instanceof SaddlebagItem item && shouldClearContents(item.getMaterial(), source)) {
            SaddlebagItem.setContents(bag, List.of());
        }
        return bag;
    }

    /** Whether {@code horse}'s own equipped saddlebag has the Gold Piglin passive. */
    public static boolean hasGoldPiglinPassive(AbstractHorse horse) {
        return SaddlebagAccess.get(horse).getItem() instanceof SaddlebagItem item
            && item.getMaterial().hasGoldPiglinPassive();
    }
}
