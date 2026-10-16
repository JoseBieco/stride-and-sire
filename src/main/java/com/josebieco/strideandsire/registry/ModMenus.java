package com.josebieco.strideandsire.registry;

import com.josebieco.strideandsire.StrideAndSire;
import com.josebieco.strideandsire.menu.HorseshoeHorseMenu;
import com.josebieco.strideandsire.menu.HorseshoeMenuEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, StrideAndSire.MODID);

    /** Horse inventory with the horseshoe slot; extra open data is written by {@link HorseshoeHorseMenu#writeOpenData}. */
    public static final RegistryObject<MenuType<HorseshoeHorseMenu>> HORSESHOE_HORSE =
        MENUS.register("horseshoe_horse", () -> IForgeMenuType.create(HorseshoeHorseMenu::fromNetwork));

    private ModMenus() {}

    /** Registers the menu type and the game-bus listener that redirects the horse inventory to it. */
    public static void register(BusGroup modBusGroup) {
        MENUS.register(modBusGroup);
        HorseshoeMenuEvents.register();
    }
}
