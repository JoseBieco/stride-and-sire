package com.josebieco.strideandsire.registry;

import com.josebieco.strideandsire.StrideAndSire;
import com.josebieco.strideandsire.item.horseshoe.HorseshoeItem;
import com.josebieco.strideandsire.item.horseshoe.HorseshoeMaterial;
import com.josebieco.strideandsire.item.saddlebag.SaddlebagItem;
import com.josebieco.strideandsire.item.saddlebag.SaddlebagMaterial;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, StrideAndSire.MODID);

    public static final RegistryObject<HorseshoeItem> COPPER_HORSESHOE = horseshoe("copper_horseshoe", HorseshoeMaterial.COPPER);
    public static final RegistryObject<HorseshoeItem> IRON_HORSESHOE = horseshoe("iron_horseshoe", HorseshoeMaterial.IRON);
    public static final RegistryObject<HorseshoeItem> GOLD_HORSESHOE = horseshoe("gold_horseshoe", HorseshoeMaterial.GOLD);
    public static final RegistryObject<HorseshoeItem> NETHERITE_HORSESHOE = horseshoe("netherite_horseshoe", HorseshoeMaterial.NETHERITE);

    public static final RegistryObject<SaddlebagItem> LEATHER_SADDLEBAG = saddlebag("leather_saddlebag", SaddlebagMaterial.LEATHER);
    public static final RegistryObject<SaddlebagItem> IRON_SADDLEBAG = saddlebag("iron_saddlebag", SaddlebagMaterial.IRON);
    public static final RegistryObject<SaddlebagItem> GOLD_SADDLEBAG = saddlebag("gold_saddlebag", SaddlebagMaterial.GOLD);
    public static final RegistryObject<SaddlebagItem> NETHERITE_SADDLEBAG = saddlebag("netherite_saddlebag", SaddlebagMaterial.NETHERITE);

    private ModItems() {}

    private static RegistryObject<HorseshoeItem> horseshoe(String name, HorseshoeMaterial material) {
        return ITEMS.register(name, () -> new HorseshoeItem(material, new Item.Properties().setId(ITEMS.key(name))));
    }

    private static RegistryObject<SaddlebagItem> saddlebag(String name, SaddlebagMaterial material) {
        return ITEMS.register(name, () -> new SaddlebagItem(material, new Item.Properties().setId(ITEMS.key(name))));
    }

    public static void register(BusGroup modBusGroup) {
        ITEMS.register(modBusGroup);
        BuildCreativeModeTabContentsEvent.BUS.addListener(ModItems::addToCreativeTabs);
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(COPPER_HORSESHOE);
            event.accept(IRON_HORSESHOE);
            event.accept(GOLD_HORSESHOE);
            event.accept(NETHERITE_HORSESHOE);
            event.accept(LEATHER_SADDLEBAG);
            event.accept(IRON_SADDLEBAG);
            event.accept(GOLD_SADDLEBAG);
            event.accept(NETHERITE_SADDLEBAG);
        }
    }
}
