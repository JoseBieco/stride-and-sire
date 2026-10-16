package com.josebieco.strideandsire;

import com.josebieco.strideandsire.gametest.ModGameTests;
import com.josebieco.strideandsire.registry.ModAttachments;
import com.josebieco.strideandsire.registry.ModItems;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(StrideAndSire.MODID)
public final class StrideAndSire {
    public static final String MODID = "strideandsire";

    public StrideAndSire(FMLJavaModLoadingContext context) {
        BusGroup modBusGroup = context.getModBusGroup();
        ModItems.register(modBusGroup);
        ModAttachments.register();
        ModGameTests.register(modBusGroup);
    }
}
