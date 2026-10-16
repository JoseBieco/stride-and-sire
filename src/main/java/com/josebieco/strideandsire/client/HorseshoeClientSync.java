package com.josebieco.strideandsire.client;

import com.josebieco.strideandsire.attachment.HorseshoeAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.ItemStack;

/** Client-only: applies a {@code HorseshoeSyncPacket} to the client-side copy of the horse. */
public final class HorseshoeClientSync {
    private HorseshoeClientSync() {}

    public static void apply(int entityId, ItemStack stack) {
        var level = Minecraft.getInstance().level;
        if (level != null && level.getEntity(entityId) instanceof AbstractHorse horse) {
            HorseshoeAccess.set(horse, stack); // client side: stores only, never re-sends
        }
    }
}
