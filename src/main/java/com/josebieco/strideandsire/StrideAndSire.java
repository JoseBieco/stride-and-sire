package com.josebieco.strideandsire;

import com.josebieco.strideandsire.client.StrideAndSireClient;
import com.josebieco.strideandsire.event.HorseshoeDurabilityHandler;
import com.josebieco.strideandsire.event.HorseshoeEnchantmentEffectHandler;
import com.josebieco.strideandsire.event.HorseshoePassiveEffectHandler;
import com.josebieco.strideandsire.gametest.ModGameTests;
import com.josebieco.strideandsire.registry.ModAttachments;
import com.josebieco.strideandsire.registry.ModItems;
import com.josebieco.strideandsire.registry.ModMenus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(StrideAndSire.MODID)
public final class StrideAndSire {
    public static final String MODID = "strideandsire";

    public StrideAndSire(FMLJavaModLoadingContext context) {
        BusGroup modBusGroup = context.getModBusGroup();
        ModItems.register(modBusGroup);
        com.josebieco.strideandsire.registry.ModRecipes.register(modBusGroup);
        ModAttachments.register();
        ModMenus.register(modBusGroup);
        HorseshoePassiveEffectHandler.register();
        com.josebieco.strideandsire.event.SaddlebagSaddleDependencyHandler.register();
        HorseshoeEnchantmentEffectHandler.register();
        HorseshoeDurabilityHandler.register();
        com.josebieco.strideandsire.event.AttributeRangeExpansionHandler.register();
        com.josebieco.strideandsire.event.HorseBreedingFoodHandler.register();
        com.josebieco.strideandsire.event.HorseBreedingGeneticsHandler.register();
        ModGameTests.register(modBusGroup);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            StrideAndSireClient.init(modBusGroup);
        }
    }
}
