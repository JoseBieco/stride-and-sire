package com.josebieco.strideandsire.client;

import com.josebieco.strideandsire.attachment.HorseEquipmentSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.ItemStack;

/** Client-only: applies a {@code HorseEquipmentSyncPacket} to the client-side copy of the horse. */
public final class HorseEquipmentClientSync {
    private HorseEquipmentClientSync() {}

    public static void apply(int entityId, HorseEquipmentSlot slot, ItemStack stack) {
        var level = Minecraft.getInstance().level;
        if (level != null && level.getEntity(entityId) instanceof AbstractHorse horse) {
            slot.set(horse, stack); // client side: stores only, never re-sends
        }
    }
}
