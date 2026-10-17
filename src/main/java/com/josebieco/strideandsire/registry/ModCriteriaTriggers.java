package com.josebieco.strideandsire.registry;

import com.josebieco.strideandsire.StrideAndSire;
import com.josebieco.strideandsire.advancement.BredPureCoatTrigger;
import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registers custom advancement criterion triggers under {@code strideandsire}.
 */
public final class ModCriteriaTriggers {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
        DeferredRegister.create(Registries.TRIGGER_TYPE, StrideAndSire.MODID);

    public static final RegistryObject<BredPureCoatTrigger> BRED_PURE_COAT =
        TRIGGERS.register("bred_pure_coat", BredPureCoatTrigger::new);

    private ModCriteriaTriggers() {}

    public static void register(BusGroup modBusGroup) {
        TRIGGERS.register(modBusGroup);
    }
}
