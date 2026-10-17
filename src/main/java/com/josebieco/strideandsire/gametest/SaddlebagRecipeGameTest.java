package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.item.saddlebag.SaddlebagItem;
import com.josebieco.strideandsire.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.NonNullList;
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
import net.minecraft.world.level.GameType;

/** Test instances: data/strideandsire/test_instance/saddlebag_recipe_*.json */
public final class SaddlebagRecipeGameTest {
    private SaddlebagRecipeGameTest() {}

    // Row-major 3x3: top "X X", middle "M M", bottom "LLL".
    private static List<ItemStack> gridItems(Item top, Item mid) {
        List<ItemStack> items = new ArrayList<>();
        for (Item i : new Item[] {top, null, top, mid, null, mid, Items.LEATHER, Items.LEATHER, Items.LEATHER}) {
            items.add(i == null ? ItemStack.EMPTY : new ItemStack(i));
        }
        return items;
    }

    private static CraftingInput grid(Item top, Item mid) {
        return CraftingInput.of(3, 3, gridItems(top, mid));
    }

    private static Optional<RecipeHolder<CraftingRecipe>> craft(GameTestHelper helper, CraftingInput input) {
        ServerLevel level = helper.getLevel();
        return level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
    }

    private static void check(GameTestHelper helper, Item top, Item mid, Item expected, String name) {
        CraftingInput in = grid(top, mid);
        Optional<RecipeHolder<CraftingRecipe>> found = craft(helper, in);
        helper.assertTrue(found.isPresent(), name + ": no crafting recipe");
        ItemStack result = found.get().value().assemble(in);
        helper.assertTrue(result.is(expected), name + ": wrong result " + result);
        helper.assertValueEqual(result.getCount(), 1, name + " count");
    }

    public static void craftsLeatherSaddlebag(GameTestHelper helper) {
        check(helper, Items.LEATHER, Items.LEATHER, ModItems.LEATHER_SADDLEBAG.get(), "leather");
        List<ItemStack> six = gridItems(Items.LEATHER, Items.LEATHER);
        six.set(8, ItemStack.EMPTY);
        helper.assertTrue(craft(helper, CraftingInput.of(3, 3, six)).isEmpty(), "6 leather must not craft");
        helper.succeed();
    }

    public static void craftsIronSaddlebag(GameTestHelper helper) {
        check(helper, Items.IRON_INGOT, Items.LEATHER, ModItems.IRON_SADDLEBAG.get(), "iron");
        Optional<RecipeHolder<CraftingRecipe>> wrong = craft(helper, grid(Items.IRON_INGOT, Items.IRON_INGOT));
        helper.assertTrue(wrong.isEmpty()
            || !wrong.get().value().assemble(grid(Items.IRON_INGOT, Items.IRON_INGOT)).is(ModItems.IRON_SADDLEBAG.get()),
            "iron in middle row must not craft an iron saddlebag");
        helper.succeed();
    }

    public static void craftsDiamondSaddlebag(GameTestHelper helper) {
        check(helper, Items.DIAMOND, Items.LEATHER, ModItems.DIAMOND_SADDLEBAG.get(), "diamond");
        helper.succeed();
    }

    public static void craftsGoldSaddlebag(GameTestHelper helper) {
        check(helper, Items.GOLD_INGOT, Items.LEATHER, ModItems.GOLD_SADDLEBAG.get(), "gold");
        helper.succeed();
    }

    public static void noCraftingRecipeForNetherite(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (Item top : List.of(Items.NETHERITE_INGOT, Items.IRON_INGOT, Items.GOLD_INGOT, Items.LEATHER, Items.DIAMOND)) {
            Optional<RecipeHolder<CraftingRecipe>> found = craft(helper, grid(top, Items.LEATHER));
            if (found.isPresent()) {
                helper.assertTrue(!found.get().value().assemble(grid(top, Items.LEATHER)).is(ModItems.NETHERITE_SADDLEBAG.get()),
                    top + " shape crafts the netherite saddlebag");
            }
        }
        for (RecipeHolder<?> holder : level.recipeAccess().getRecipes()) {
            if (holder.value() instanceof CraftingRecipe recipe) {
                recipe.display().forEach(d -> helper.assertTrue(!d.result().toString().contains("netherite_saddlebag"),
                    "crafting recipe " + holder.id() + " produces the netherite saddlebag"));
            }
        }
        helper.succeed();
    }

    private static ItemStack diamondBagWithContents() {
        ItemStack bag = new ItemStack(ModItems.DIAMOND_SADDLEBAG.get());
        NonNullList<ItemStack> contents = NonNullList.withSize(SaddlebagItem.containerSize(bag), ItemStack.EMPTY);
        contents.set(0, new ItemStack(Items.DIAMOND, 5));
        contents.set(7, new ItemStack(Items.APPLE, 33));
        SaddlebagItem.setContents(bag, contents);
        return bag;
    }

    private static void assertContents(GameTestHelper helper, ItemStack out, String name) {
        List<ItemStack> c = SaddlebagItem.getContents(out);
        helper.assertTrue(c.get(0).is(Items.DIAMOND) && c.get(0).getCount() == 5, name + ": slot 0 wrong " + c.get(0));
        helper.assertTrue(c.get(7).is(Items.APPLE) && c.get(7).getCount() == 33, name + ": slot 7 wrong " + c.get(7));
        for (int i = 0; i < c.size(); i++) {
            if (i != 0 && i != 7) {
                helper.assertTrue(c.get(i).isEmpty(), name + ": slot " + i + " should be empty, got " + c.get(i));
            }
        }
    }

    private static SmithingRecipeInput smithingInput(ItemStack base) {
        return new SmithingRecipeInput(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), base, new ItemStack(Items.NETHERITE_INGOT));
    }

    public static void netheriteSmithingPreservesContents(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SmithingRecipeInput input = smithingInput(diamondBagWithContents());
        Optional<RecipeHolder<SmithingRecipe>> found = level.recipeAccess().getRecipeFor(RecipeType.SMITHING, input, level);
        helper.assertTrue(found.isPresent(), "no smithing recipe for diamond saddlebag");
        ItemStack out = found.get().value().assemble(input);
        helper.assertTrue(out.is(ModItems.NETHERITE_SADDLEBAG.get()), "wrong result " + out);
        assertContents(helper, out, "recipe");

        ItemStack empty = found.get().value().assemble(smithingInput(new ItemStack(ModItems.DIAMOND_SADDLEBAG.get())));
        helper.assertTrue(empty.is(ModItems.NETHERITE_SADDLEBAG.get()), "empty upgrade wrong item");
        helper.assertTrue(SaddlebagItem.getContents(empty).stream().allMatch(ItemStack::isEmpty), "empty bag gained items");

        for (Item other : List.of(ModItems.LEATHER_SADDLEBAG.get(), ModItems.IRON_SADDLEBAG.get(), ModItems.GOLD_SADDLEBAG.get())) {
            helper.assertTrue(level.recipeAccess().getRecipeFor(RecipeType.SMITHING,
                smithingInput(new ItemStack(other)), level).isEmpty(), other + " must not upgrade");
        }
        helper.succeed();
    }

    public static void ironSaddlebagHasNoSmithingResult(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(level.recipeAccess().getRecipeFor(RecipeType.SMITHING,
            smithingInput(new ItemStack(ModItems.IRON_SADDLEBAG.get())), level).isEmpty(), "iron bag must not upgrade");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        SmithingMenu menu = new SmithingMenu(0, player.getInventory());
        menu.getSlot(0).set(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
        menu.getSlot(1).set(new ItemStack(ModItems.IRON_SADDLEBAG.get()));
        menu.getSlot(2).set(new ItemStack(Items.NETHERITE_INGOT));
        menu.createResult();
        helper.assertTrue(menu.getSlot(menu.getResultSlot()).getItem().isEmpty(),
            "smithing menu produced a result for an iron bag: " + menu.getSlot(menu.getResultSlot()).getItem());
        helper.succeed();
    }

    public static void smithingMenuPreservesContents(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        SmithingMenu menu = new SmithingMenu(0, player.getInventory());
        menu.getSlot(0).set(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
        menu.getSlot(1).set(diamondBagWithContents());
        menu.getSlot(2).set(new ItemStack(Items.NETHERITE_INGOT));
        menu.createResult();
        ItemStack out = menu.getSlot(menu.getResultSlot()).getItem();
        helper.assertTrue(out.is(ModItems.NETHERITE_SADDLEBAG.get()), "menu result wrong: " + out);
        assertContents(helper, out, "menu");
        helper.succeed();
    }
}
