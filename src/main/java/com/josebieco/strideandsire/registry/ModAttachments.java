package com.josebieco.strideandsire.registry;

import com.josebieco.strideandsire.StrideAndSire;
import com.josebieco.strideandsire.attachment.HorseshoeEvents;
import com.josebieco.strideandsire.attachment.HorseshoeStorage;
import com.josebieco.strideandsire.network.ModNetwork;
import net.minecraft.resources.Identifier;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

/**
 * Per-entity data attachments. Forge 66 has no NeoForge-style AttachmentType, so the horseshoe is a Forge
 * capability ({@link HorseshoeStorage}, auto-registered via {@code @AutoRegisterCapability}) attached to
 * eligible horses under {@link #HORSESHOE_ID}; its default value is
 * {@link com.josebieco.strideandsire.attachment.HorseshoeAttachment#EMPTY}.
 */
public final class ModAttachments {
    public static final Identifier HORSESHOE_ID = Identifier.fromNamespaceAndPath(StrideAndSire.MODID, "horseshoe");
    public static final Capability<HorseshoeStorage> HORSESHOE = CapabilityManager.get(new CapabilityToken<>() {});

    private ModAttachments() {}

    /** Registers the game-bus listeners (attach, tracking sync, death drop) and the sync channel. */
    public static void register() {
        ModNetwork.init();
        HorseshoeEvents.register();
    }
}
