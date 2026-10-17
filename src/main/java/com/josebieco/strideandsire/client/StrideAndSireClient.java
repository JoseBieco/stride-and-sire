package com.josebieco.strideandsire.client;

import com.josebieco.strideandsire.attachment.HorseshoeAccess;
import com.josebieco.strideandsire.menu.StrideAndSireHorseMenu;
import com.josebieco.strideandsire.network.ModNetwork;
import com.josebieco.strideandsire.network.OpenHorseshoeMenuPacket;
import com.josebieco.strideandsire.registry.ModMenus;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.entity.AbstractHorseRenderer;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.network.PacketDistributor;

/** Client-only setup. Only called on {@code Dist.CLIENT}, so the dedicated (GameTest) server never loads it. */
public final class StrideAndSireClient {
    private StrideAndSireClient() {}

    public static void init(BusGroup modBusGroup) {
        FMLClientSetupEvent.getBus(modBusGroup).addListener(StrideAndSireClient::onClientSetup);
        TickEvent.ClientTickEvent.Pre.BUS.addListener(StrideAndSireClient::onClientTickPre);
        EntityRenderersEvent.RegisterLayerDefinitions.BUS.addListener(StrideAndSireClient::onRegisterLayerDefinitions);
        EntityRenderersEvent.AddLayers.BUS.addListener(StrideAndSireClient::onAddLayers);
    }

    /** Renderers that get the mod's equipment layers: the horseshoe/saddlebag-eligible types (not Llama/Camel). */
    private static final List<EntityType<? extends AbstractHorse>> EQUIPMENT_LAYER_TYPES = List.of(
        EntityTypes.HORSE, EntityTypes.DONKEY, EntityTypes.MULE, EntityTypes.SKELETON_HORSE, EntityTypes.ZOMBIE_HORSE);

    /** Renderers that get the recessive coat override layer: strictly vanilla Horse. */
    private static final List<EntityType<? extends AbstractHorse>> COAT_LAYER_TYPES = List.of(EntityTypes.HORSE);

    private static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SaddlebagModel.LAYER, SaddlebagModel::createBodyLayer);
    }

    /** Attaches the coat override, hoof, and saddlebag layers to every eligible renderer. Fires on each resource reload. */
    private static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (EntityType<? extends AbstractHorse> type : COAT_LAYER_TYPES) {
            Object renderer = event.getEntityRenderer(type);
            if (renderer instanceof AbstractHorseRenderer<?, ?, ?> horseRenderer) {
                addCoatLayer(horseRenderer);
            }
        }
        for (EntityType<? extends AbstractHorse> type : EQUIPMENT_LAYER_TYPES) {
            Object renderer = event.getEntityRenderer(type);
            if (renderer instanceof AbstractHorseRenderer<?, ?, ?> horseRenderer) {
                addEquipmentLayers(horseRenderer, event.getEntityModels());
            }
        }
    }

    private static <S extends EquineRenderState, M extends EntityModel<? super S>> void addCoatLayer(
            AbstractHorseRenderer<?, S, M> renderer) {
        renderer.addLayer(new HorseCoatOverrideLayer<>(renderer));
    }

    private static <S extends EquineRenderState, M extends EntityModel<? super S>> void addEquipmentLayers(
            AbstractHorseRenderer<?, S, M> renderer, EntityModelSet models) {
        renderer.addLayer(new HorseshoeLayer<>(renderer));
        renderer.addLayer(new SaddlebagLayer<>(renderer, models));
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.<StrideAndSireHorseMenu, StrideAndSireHorseScreen>register(
            ModMenus.HORSESHOE_HORSE.get(), StrideAndSireHorseScreen::new));
    }

    /**
     * Inventory key while riding an eligible horse: vanilla's {@code handleKeybinds} (which runs after this event in
     * the same tick, under the same no-screen/no-overlay condition) would send {@code OPEN_INVENTORY} and the server
     * would open the vanilla menu. Consume the press here and ask for the horseshoe menu instead.
     */
    private static void onClientTickPre(TickEvent.ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null || mc.gui.screen() != null || mc.gui.overlay() != null
                || !(mc.player.getVehicle() instanceof AbstractHorse horse) || !HorseshoeAccess.isEligible(horse)
                || !mc.gameMode.isServerControlledInventory()) {
            return;
        }
        boolean pressed = false;
        while (mc.options.keyInventory.consumeClick()) {
            pressed = true;
        }
        if (pressed) {
            ModNetwork.CHANNEL.send(OpenHorseshoeMenuPacket.INSTANCE, PacketDistributor.SERVER.noArg());
        }
    }
}
