package com.josebieco.strideandsire.registry;

import com.josebieco.strideandsire.StrideAndSire;
import com.josebieco.strideandsire.recipe.HorseshoeSmithingUpgradeRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, StrideAndSire.MODID);

    public static final RegistryObject<RecipeSerializer<HorseshoeSmithingUpgradeRecipe>> HORSESHOE_SMITHING_UPGRADE =
        SERIALIZERS.register("horseshoe_smithing_upgrade", () -> HorseshoeSmithingUpgradeRecipe.SERIALIZER);

    private ModRecipes() {}

    public static void register(BusGroup modBusGroup) {
        SERIALIZERS.register(modBusGroup);
    }
}
