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
        ModAttachments.register();
        ModMenus.register(modBusGroup);
        HorseshoePassiveEffectHandler.register();
        HorseshoeEnchantmentEffectHandler.register();
        HorseshoeDurabilityHandler.register();
        ModGameTests.register(modBusGroup);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            StrideAndSireClient.init(modBusGroup);
        }
    }
}
