package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.attachment.HorseshoeAccess;
import com.josebieco.strideandsire.enchantment.HorseshoeEnchantments;
import com.josebieco.strideandsire.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameType;

/** Review Focus item 4, end to end. Test instance: horseshoe_review_focus_iron_to_netherite_flow.json */
public final class HorseshoeReviewFocusGameTest {
    private HorseshoeReviewFocusGameTest() {}

    private static final boolean[] U_SHAPE = {true, false, true, true, false, true, false, true, false};

    public static void ironToNetheriteFlow(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Holder<Enchantment> feather = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
            .getOrThrow(HorseshoeEnchantments.FEATHER_STEP);

        // 1. craft
        List<ItemStack> items = new ArrayList<>();
        for (boolean f : U_SHAPE) {
            items.add(f ? new ItemStack(Items.IRON_INGOT) : ItemStack.EMPTY);
        }
        CraftingInput input = CraftingInput.of(3, 3, items);
        Optional<RecipeHolder<CraftingRecipe>> found = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
        helper.assertTrue(found.isPresent(), "no crafting recipe for iron U shape");
        ItemStack shoe = found.get().value().assemble(input);
        helper.assertTrue(shoe.is(ModItems.IRON_HORSESHOE.get()), "crafted wrong item " + shoe);

        // 2. enchant + damage to half
        shoe.enchant(feather, 1);
        shoe.setDamageValue(100);
        helper.assertValueEqual(shoe.getMaxDamage(), 200, "iron max damage");

        // 3. equip, then take off
        Horse horse = helper.spawn(EntityTypes.HORSE, 2, 1, 2);
        HorseshoeAccess.set(horse, shoe);
        ItemStack equipped = HorseshoeAccess.get(horse);
        helper.assertTrue(equipped.is(ModItems.IRON_HORSESHOE.get()), "iron shoe not equipped");
        ItemStack removed = equipped.copy();
        HorseshoeAccess.set(horse, ItemStack.EMPTY);
        helper.assertTrue(HorseshoeAccess.get(horse).isEmpty(), "slot not emptied");
        helper.assertValueEqual(removed.getDamageValue(), 100, "removed shoe damage");

        // 4. smithing upgrade through the real menu
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        SmithingMenu menu = new SmithingMenu(0, player.getInventory());
        menu.getSlot(0).set(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
        menu.getSlot(1).set(removed);
        menu.getSlot(2).set(new ItemStack(Items.NETHERITE_INGOT));
        menu.createResult();
        ItemStack out = menu.getSlot(menu.getResultSlot()).getItem().copy();
        helper.assertTrue(out.is(ModItems.NETHERITE_HORSESHOE.get()), "smithing result wrong: " + out);
        helper.assertValueEqual(EnchantmentHelper.getItemEnchantmentLevel(feather, out), 1, "feather step level after upgrade");
        helper.assertValueEqual(out.getMaxDamage(), 400, "netherite max damage");
        helper.assertValueEqual(out.getDamageValue(), 200, "damage after upgrade (ratio)");

        // 5. equip the result
        HorseshoeAccess.set(horse, out);
        ItemStack held = HorseshoeAccess.get(horse);
        helper.assertTrue(held.is(ModItems.NETHERITE_HORSESHOE.get()), "netherite shoe not equipped");
        helper.assertValueEqual(EnchantmentHelper.getItemEnchantmentLevel(feather, held), 1, "equipped feather step level");
        helper.assertValueEqual(held.getDamageValue(), 200, "equipped damage");
        helper.succeed();
    }
}
