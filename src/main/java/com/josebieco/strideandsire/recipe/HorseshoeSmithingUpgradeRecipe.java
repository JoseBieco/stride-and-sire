package com.josebieco.strideandsire.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleSmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.TransmuteRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;

/**
 * A {@code smithing_transform} that, unlike vanilla, preserves the input's damage <em>ratio</em> instead of its absolute
 * damage (an Iron horseshoe at 100/200 becomes a Netherite one at 200/400). Components (enchantments, ...) are copied
 * exactly like vanilla. Reuses vanilla's smithing recipe type, so the Smithing Table picks it up unchanged.
 */
public class HorseshoeSmithingUpgradeRecipe extends SimpleSmithingRecipe {
    public static final MapCodec<HorseshoeSmithingUpgradeRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(
                Recipe.CommonInfo.MAP_CODEC.forGetter(o -> o.commonInfo),
                Ingredient.CODEC.optionalFieldOf("template").forGetter(o -> o.template),
                Ingredient.CODEC.fieldOf("base").forGetter(o -> o.base),
                Ingredient.CODEC.optionalFieldOf("addition").forGetter(o -> o.addition),
                ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result)
            )
            .apply(i, HorseshoeSmithingUpgradeRecipe::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, HorseshoeSmithingUpgradeRecipe> STREAM_CODEC = StreamCodec.composite(
        Recipe.CommonInfo.STREAM_CODEC, o -> o.commonInfo,
        Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC, o -> o.template,
        Ingredient.CONTENTS_STREAM_CODEC, o -> o.base,
        Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC, o -> o.addition,
        ItemStackTemplate.STREAM_CODEC, o -> o.result,
        HorseshoeSmithingUpgradeRecipe::new
    );
    public static final RecipeSerializer<HorseshoeSmithingUpgradeRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final Optional<Ingredient> template;
    private final Ingredient base;
    private final Optional<Ingredient> addition;
    private final ItemStackTemplate result;

    public HorseshoeSmithingUpgradeRecipe(Recipe.CommonInfo commonInfo, Optional<Ingredient> template, Ingredient base,
                                          Optional<Ingredient> addition, ItemStackTemplate result) {
        super(commonInfo);
        this.template = template;
        this.base = base;
        this.addition = addition;
        this.result = result;
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input) {
        ItemStack in = input.base();
        ItemStack out = TransmuteRecipe.createWithOriginalComponents(this.result, in);
        int inMax = in.getMaxDamage();
        int outMax = out.getMaxDamage();
        if (inMax > 0 && outMax > 0) {
            int scaled = Math.round((float) in.getDamageValue() / inMax * outMax);
            out.set(DataComponents.DAMAGE, Math.max(0, Math.min(scaled, outMax - 1)));
        }
        return out;
    }

    @Override
    public Optional<Ingredient> templateIngredient() {
        return this.template;
    }

    @Override
    public Ingredient baseIngredient() {
        return this.base;
    }

    @Override
    public Optional<Ingredient> additionIngredient() {
        return this.addition;
    }

    @Override
    public RecipeSerializer<HorseshoeSmithingUpgradeRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    protected PlacementInfo createPlacementInfo() {
        return PlacementInfo.createFromOptionals(List.of(this.template, Optional.of(this.base), this.addition));
    }

    @Override
    public List<RecipeDisplay> display() {
        return List.of(new SmithingRecipeDisplay(
            Ingredient.optionalIngredientToDisplay(this.template),
            this.base.display(),
            Ingredient.optionalIngredientToDisplay(this.addition),
            new SlotDisplay.ItemStackSlotDisplay(this.result),
            new SlotDisplay.ItemSlotDisplay(Items.SMITHING_TABLE)
        ));
    }
}
