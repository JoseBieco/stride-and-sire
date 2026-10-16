package com.josebieco.strideandsire.enchantment;

import com.josebieco.strideandsire.StrideAndSire;
import com.josebieco.strideandsire.item.horseshoe.HorseshoeEnchantmentKind;
import com.josebieco.strideandsire.item.horseshoe.HorseshoeItem;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * The three horseshoe-exclusive enchantments (spec section 4).
 *
 * <p>The enchantments themselves are data-driven ({@code data/strideandsire/enchantment/*.json}). Their
 * {@code supported_items}/{@code primary_items} point at the item tags {@code #strideandsire:enchantable/<name>},
 * so the anvil ({@code Enchantment.canEnchant}), the enchanting table ({@code Enchantment.isPrimaryItem}) and
 * {@code /enchant} reject ineligible tiers natively, exactly like vanilla rejects Soul Speed on a chestplate.
 *
 * <p>{@link #isEligible} is the Java side of the same rule, derived from
 * {@link com.josebieco.strideandsire.item.horseshoe.HorseshoeMaterial#allowsEnchantment} (the single source of truth
 * for the eligibility matrix). It gates the enchanting table via {@code HorseshoeItem.canApplyAtEnchantingTable} and
 * every enchantment effect (so a horseshoe given an ineligible enchantment by other means does nothing). A GameTest
 * asserts the tags and the matrix agree for every material x enchantment.
 */
public final class HorseshoeEnchantments {
    public static final ResourceKey<Enchantment> FEATHER_STEP = key("feather_step");
    public static final ResourceKey<Enchantment> FROST_STEP = key("frost_step");
    public static final ResourceKey<Enchantment> LAVA_WALKER = key("lava_walker");

    private HorseshoeEnchantments() {}

    private static ResourceKey<Enchantment> key(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(StrideAndSire.MODID, name));
    }

    /** The horseshoe enchantment kind for {@code key}, or {@code null} if it is not one of the three. */
    public static @Nullable HorseshoeEnchantmentKind kindOf(ResourceKey<Enchantment> key) {
        if (FEATHER_STEP.equals(key)) {
            return HorseshoeEnchantmentKind.FEATHER_STEP;
        }
        if (FROST_STEP.equals(key)) {
            return HorseshoeEnchantmentKind.FROST_STEP;
        }
        if (LAVA_WALKER.equals(key)) {
            return HorseshoeEnchantmentKind.LAVA_WALKER;
        }
        return null;
    }

    /**
     * Whether {@code enchantment} (one of the three horseshoe enchantments) may be on {@code horseshoeStack}:
     * the stack must be a horseshoe whose material allows it. Always {@code false} for non-horseshoe stacks and for
     * any other enchantment key (generic enchantments such as Unbreaking/Mending are governed by vanilla's tags).
     */
    public static boolean isEligible(ItemStack horseshoeStack, ResourceKey<Enchantment> enchantment) {
        HorseshoeEnchantmentKind kind = kindOf(enchantment);
        return kind != null
            && horseshoeStack.getItem() instanceof HorseshoeItem horseshoe
            && horseshoe.getMaterial().allowsEnchantment(kind);
    }

    /** {@link #isEligible} for a registry holder; {@code true} for holders that are not horseshoe enchantments. */
    public static boolean allowsOnHorseshoe(ItemStack horseshoeStack, Holder<Enchantment> enchantment) {
        Optional<ResourceKey<Enchantment>> key = enchantment.unwrapKey();
        return key.isEmpty() || kindOf(key.get()) == null || isEligible(horseshoeStack, key.get());
    }

    /**
     * The effective level of {@code enchantment} on {@code stack}: its level from the stack's enchantments, looked up
     * through {@code level}'s registry access, or 0 if absent, not registered, or not eligible on this stack.
     */
    public static int effectiveLevel(Level level, ItemStack stack, ResourceKey<Enchantment> enchantment) {
        if (stack.isEmpty() || !stack.isEnchanted() || !isEligible(stack, enchantment)) {
            return 0;
        }
        return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(enchantment)
            .map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, stack))
            .orElse(0);
    }
}
