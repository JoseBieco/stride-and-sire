package com.josebieco.strideandsire.client;

import com.josebieco.strideandsire.attachment.HorseshoeAccess;
import com.josebieco.strideandsire.network.ModNetwork;
import com.josebieco.strideandsire.network.OpenHorseshoeMenuPacket;
import com.josebieco.strideandsire.registry.ModMenus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.inventory.HorseInventoryMenu;
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
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.<HorseInventoryMenu, HorseshoeHorseScreen>register(
            ModMenus.HORSESHOE_HORSE.get(), HorseshoeHorseScreen::new));
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
