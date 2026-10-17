package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.enchantment.HorseshoeEnchantments;
import com.josebieco.strideandsire.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameType;

/** Test instances: data/strideandsire/test_instance/horseshoe_recipe_*.json */
public final class HorseshoeRecipeGameTest {
    private HorseshoeRecipeGameTest() {}

    private static final boolean[] U_SHAPE = {true, false, true, true, false, true, false, true, false};
    private static final boolean[] FOUR = {true, false, true, true, false, false, false, true, false};

    private static CraftingInput grid(Item ingot, boolean[] filled) {
        List<ItemStack> items = new ArrayList<>();
        for (boolean f : filled) {
            items.add(f ? new ItemStack(ingot) : ItemStack.EMPTY);
        }
        return CraftingInput.of(3, 3, items);
    }

    private static Optional<RecipeHolder<CraftingRecipe>> craft(GameTestHelper helper, CraftingInput input) {
        ServerLevel level = helper.getLevel();
        return level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
    }

    private static void checkCrafting(GameTestHelper helper, Item ingot, Item expected, String name) {
        Optional<RecipeHolder<CraftingRecipe>> found = craft(helper, grid(ingot, U_SHAPE));
        helper.assertTrue(found.isPresent(), name + ": no crafting recipe for the 5-ingot U shape");
        ItemStack result = found.get().value().assemble(grid(ingot, U_SHAPE));
        helper.assertTrue(result.is(expected), name + ": wrong result " + result);
        helper.assertValueEqual(result.getCount(), 1, name + " result count");
        helper.assertTrue(craft(helper, grid(ingot, FOUR)).isEmpty(), name + ": 4 ingots must not craft anything");
    }

    public static void craftsCopperHorseshoe(GameTestHelper helper) {
        checkCrafting(helper, Items.COPPER_INGOT, ModItems.COPPER_HORSESHOE.get(), "copper");
        helper.succeed();
    }

    public static void craftsIronHorseshoe(GameTestHelper helper) {
        checkCrafting(helper, Items.IRON_INGOT, ModItems.IRON_HORSESHOE.get(), "iron");
        helper.succeed();
    }

    public static void craftsDiamondHorseshoe(GameTestHelper helper) {
        checkCrafting(helper, Items.DIAMOND, ModItems.DIAMOND_HORSESHOE.get(), "diamond");
        helper.succeed();
    }

    public static void craftsGoldHorseshoe(GameTestHelper helper) {
        checkCrafting(helper, Items.GOLD_INGOT, ModItems.GOLD_HORSESHOE.get(), "gold");
        helper.succeed();
    }

    public static void noCraftingRecipeForNetherite(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (Item ingot : List.of(Items.NETHERITE_INGOT, Items.IRON_INGOT, Items.GOLD_INGOT, Items.COPPER_INGOT, Items.DIAMOND)) {
            Optional<RecipeHolder<CraftingRecipe>> found = craft(helper, grid(ingot, U_SHAPE));
            if (found.isPresent()) {
                helper.assertTrue(!found.get().value().assemble(grid(ingot, U_SHAPE)).is(ModItems.NETHERITE_HORSESHOE.get()),
                    "U shape of " + ingot + " crafts the netherite horseshoe");
            }
        }
        // No crafting recipe may mention the netherite horseshoe in its displayed result.
        for (RecipeHolder<?> holder : level.recipeAccess().getRecipes()) {
            if (holder.value() instanceof CraftingRecipe recipe) {
                recipe.display().forEach(d -> helper.assertTrue(!d.result().toString().contains("netherite_horseshoe"),
                    "crafting recipe " + holder.id() + " produces the netherite horseshoe"));
            }
        }
        helper.succeed();
    }

    private static Holder<Enchantment> feather(ServerLevel level) {
        return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(HorseshoeEnchantments.FEATHER_STEP);
    }

    private static ItemStack diamond(ServerLevel level, int damage, boolean enchanted) {
        ItemStack stack = new ItemStack(ModItems.DIAMOND_HORSESHOE.get());
        if (enchanted) {
            stack.enchant(feather(level), 1);
        }
        stack.setDamageValue(damage);
        return stack;
    }

    private static SmithingRecipeInput smithingInput(ItemStack base) {
        return new SmithingRecipeInput(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), base, new ItemStack(Items.NETHERITE_INGOT));
    }

    public static void netheriteSmithingKeepsEnchantsAndDamageRatio(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack in = diamond(level, 150, true);
        SmithingRecipeInput input = smithingInput(in);
        Optional<RecipeHolder<SmithingRecipe>> found = level.recipeAccess().getRecipeFor(RecipeType.SMITHING, input, level);
        helper.assertTrue(found.isPresent(), "no smithing recipe for diamond horseshoe + netherite ingot + template");
        ItemStack out = found.get().value().assemble(input);
        helper.assertTrue(out.is(ModItems.NETHERITE_HORSESHOE.get()), "wrong smithing result " + out);
        helper.assertValueEqual(EnchantmentHelper.getItemEnchantmentLevel(feather(level), out), 1, "feather step level");
        helper.assertValueEqual(out.getMaxDamage(), 400, "netherite max damage");
        helper.assertValueEqual(out.getDamageValue(), 200, "damage rescaled by ratio (150/300 -> 200/400)");

        ItemStack freshOut = found.get().value().assemble(smithingInput(diamond(level, 0, false)));
        helper.assertTrue(freshOut.is(ModItems.NETHERITE_HORSESHOE.get()), "undamaged upgrade wrong item");
        helper.assertValueEqual(freshOut.getDamageValue(), 0, "undamaged output damage");

        ItemStack nearlyOut = found.get().value().assemble(smithingInput(diamond(level, 299, false)));
        helper.assertTrue(nearlyOut.getDamageValue() < nearlyOut.getMaxDamage(), "nearly-broken input produced a broken item");

        helper.assertTrue(level.recipeAccess().getRecipeFor(RecipeType.SMITHING,
            smithingInput(new ItemStack(ModItems.GOLD_HORSESHOE.get())), level).isEmpty(), "gold horseshoe must not upgrade");
        helper.assertTrue(level.recipeAccess().getRecipeFor(RecipeType.SMITHING,
            new SmithingRecipeInput(ItemStack.EMPTY, in, new ItemStack(Items.NETHERITE_INGOT)), level).isEmpty(),
            "upgrade without template must not match");
        helper.succeed();
    }

    public static void smithingMenuProducesNetheriteHorseshoe(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        SmithingMenu menu = new SmithingMenu(0, player.getInventory());
        menu.getSlot(0).set(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
        menu.getSlot(1).set(diamond(helper.getLevel(), 150, true));
        menu.getSlot(2).set(new ItemStack(Items.NETHERITE_INGOT));
        menu.createResult();
        ItemStack out = menu.getSlot(menu.getResultSlot()).getItem();
        helper.assertTrue(out.is(ModItems.NETHERITE_HORSESHOE.get()), "smithing menu result wrong: " + out);
        helper.assertValueEqual(out.getDamageValue(), 200, "menu result damage");
        helper.assertTrue(out.has(DataComponents.ENCHANTMENTS), "menu result lost enchantments");
        helper.succeed();
    }

    /**
     * A Diamond horseshoe carrying Frost Step (not allowed on Netherite) has no smithing result, neither through the
     * recipe lookup nor in the Smithing Table: the upgrade would otherwise carry an ineligible enchantment (which,
     * through the terrain exclusive set, would also block Lava Walker forever).
     */
    public static void frostStepBlocksNetheriteUpgrade(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Holder<Enchantment> frost = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(HorseshoeEnchantments.FROST_STEP);
        ItemStack frostOnly = diamond(level, 50, false);
        frostOnly.enchant(frost, 1);
        ItemStack frostAndFeather = diamond(level, 0, true);
        frostAndFeather.enchant(frost, 1);

        for (ItemStack base : List.of(frostOnly, frostAndFeather)) {
            helper.assertTrue(level.recipeAccess().getRecipeFor(RecipeType.SMITHING, smithingInput(base), level).isEmpty(),
                "Frost Step horseshoe must not match the netherite upgrade: " + base);

            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            SmithingMenu menu = new SmithingMenu(0, player.getInventory());
            menu.getSlot(0).set(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
            menu.getSlot(1).set(base.copy());
            menu.getSlot(2).set(new ItemStack(Items.NETHERITE_INGOT));
            menu.createResult();
            helper.assertTrue(menu.getSlot(menu.getResultSlot()).getItem().isEmpty(),
                "smithing menu must show no result for " + base + ", got " + menu.getSlot(menu.getResultSlot()).getItem());
        }
        helper.succeed();
    }

    /** Iron no longer upgrades to Netherite (the base is Diamond): no result via RecipeManager nor the Smithing Table. */
    public static void ironHorseshoeDoesNotUpgrade(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (int damage : new int[] {0, 100}) {
            ItemStack iron = new ItemStack(ModItems.IRON_HORSESHOE.get());
            iron.setDamageValue(damage);
            helper.assertTrue(level.recipeAccess().getRecipeFor(RecipeType.SMITHING, smithingInput(iron), level).isEmpty(),
                "iron horseshoe must not match the netherite upgrade: " + iron);

            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            SmithingMenu menu = new SmithingMenu(0, player.getInventory());
            menu.getSlot(0).set(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
            menu.getSlot(1).set(iron.copy());
            menu.getSlot(2).set(new ItemStack(Items.NETHERITE_INGOT));
            menu.createResult();
            helper.assertTrue(menu.getSlot(menu.getResultSlot()).getItem().isEmpty(),
                "smithing menu must show no result for iron, got " + menu.getSlot(menu.getResultSlot()).getItem());
        }
        helper.succeed();
    }
}
