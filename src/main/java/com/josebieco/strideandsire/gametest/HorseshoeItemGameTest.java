package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.item.horseshoe.HorseshoeItem;
import com.josebieco.strideandsire.item.horseshoe.HorseshoeMaterial;
import com.josebieco.strideandsire.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;
import java.util.Locale;

/** Test instance: data/strideandsire/test_instance/horseshoe_items_are_registered_with_correct_material.json */
public final class HorseshoeItemGameTest {
    private HorseshoeItemGameTest() {}

    public static void horseshoeItemsAreRegisteredWithCorrectMaterial(GameTestHelper helper) {
        check(helper, ModItems.COPPER_HORSESHOE, HorseshoeMaterial.COPPER);
        check(helper, ModItems.IRON_HORSESHOE, HorseshoeMaterial.IRON);
        check(helper, ModItems.DIAMOND_HORSESHOE, HorseshoeMaterial.DIAMOND);
        check(helper, ModItems.GOLD_HORSESHOE, HorseshoeMaterial.GOLD);
        check(helper, ModItems.NETHERITE_HORSESHOE, HorseshoeMaterial.NETHERITE);
        helper.succeed();
    }

    private static void check(GameTestHelper helper, RegistryObject<? extends Item> holder, HorseshoeMaterial expected) {
        String name = expected.name().toLowerCase(Locale.ROOT) + "_horseshoe";
        helper.assertTrue(holder != null && holder.isPresent(), name + " is not registered");
        helper.assertTrue(holder.getId().toString().equals("strideandsire:" + name), name + " has wrong id " + holder.getId());

        Item item = holder.get();
        helper.assertTrue(item instanceof HorseshoeItem i && i.getMaterial() == expected, name + " material mismatch");

        ItemStack stack = new ItemStack(item);
        helper.assertValueEqual(stack.getMaxDamage(), expected.baseDurability(), name + " max damage");
        helper.assertValueEqual(stack.getMaxStackSize(), 1, name + " max stack size");
        helper.assertTrue(stack.has(DataComponents.ENCHANTABLE), name + " is not enchantable");
        if (expected == HorseshoeMaterial.DIAMOND) {
            helper.assertValueEqual(stack.get(DataComponents.ENCHANTABLE).value(), 10, name + " enchantability");
        }
        boolean fireResistant = stack.has(DataComponents.DAMAGE_RESISTANT);
        helper.assertValueEqual(fireResistant, expected == HorseshoeMaterial.NETHERITE, name + " fire resistance");
    }
}
