package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.item.saddlebag.SaddlebagItem;
import com.josebieco.strideandsire.item.saddlebag.SaddlebagMaterial;
import com.josebieco.strideandsire.registry.ModItems;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.RegistryObject;

/** Test instance: data/strideandsire/test_instance/saddlebag_items_are_registered_with_container.json */
public final class SaddlebagItemGameTest {
    private SaddlebagItemGameTest() {}

    public static void saddlebagItemsAreRegisteredWithContainer(GameTestHelper helper) {
        check(helper, ModItems.LEATHER_SADDLEBAG, SaddlebagMaterial.LEATHER);
        check(helper, ModItems.IRON_SADDLEBAG, SaddlebagMaterial.IRON);
        check(helper, ModItems.DIAMOND_SADDLEBAG, SaddlebagMaterial.DIAMOND);
        check(helper, ModItems.GOLD_SADDLEBAG, SaddlebagMaterial.GOLD);
        check(helper, ModItems.NETHERITE_SADDLEBAG, SaddlebagMaterial.NETHERITE);
        helper.succeed();
    }

    private static void check(GameTestHelper helper, RegistryObject<? extends Item> holder, SaddlebagMaterial expected) {
        String name = expected.name().toLowerCase(Locale.ROOT) + "_saddlebag";
        helper.assertTrue(holder != null && holder.isPresent(), name + " is not registered");
        helper.assertTrue(holder.getId().toString().equals("strideandsire:" + name), name + " has wrong id " + holder.getId());

        Item item = holder.get();
        helper.assertTrue(item instanceof SaddlebagItem i && i.getMaterial() == expected, name + " material mismatch");

        ItemStack stack = new ItemStack(item);
        helper.assertValueEqual(stack.getMaxStackSize(), 1, name + " max stack size");
        helper.assertTrue(!stack.isDamageableItem(), name + " must have no durability");
        helper.assertTrue(!stack.has(DataComponents.ENCHANTABLE), name + " must not be enchantable");
        helper.assertValueEqual(stack.has(DataComponents.DAMAGE_RESISTANT), expected == SaddlebagMaterial.NETHERITE,
            name + " fire resistance");

        helper.assertTrue(stack.has(DataComponents.CONTAINER), name + " fresh stack has no container component");
        helper.assertValueEqual(SaddlebagItem.containerSize(stack), 9, name + " container size");
        NonNullList<ItemStack> contents = SaddlebagItem.getContents(stack);
        helper.assertValueEqual(contents.size(), 9, name + " contents list size");
        for (ItemStack s : contents) {
            helper.assertTrue(s.isEmpty(), name + " fresh container must be empty");
        }

        SaddlebagItem.setContents(stack, List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
            new ItemStack(Items.DIAMOND, 3)));
        NonNullList<ItemStack> back = SaddlebagItem.getContents(stack);
        helper.assertValueEqual(back.size(), 9, name + " contents size after write");
        helper.assertTrue(back.get(4).is(Items.DIAMOND) && back.get(4).getCount() == 3, name + " slot 4 round-trip");
        for (int i = 0; i < back.size(); i++) {
            if (i != 4) {
                helper.assertTrue(back.get(i).isEmpty(), name + " slot " + i + " should be empty");
            }
        }
    }
}
