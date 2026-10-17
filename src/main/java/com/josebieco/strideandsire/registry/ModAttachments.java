package com.josebieco.strideandsire.registry;

import com.josebieco.strideandsire.StrideAndSire;
import com.josebieco.strideandsire.attachment.HorseEquipmentEvents;
import com.josebieco.strideandsire.attachment.HorseGeneticsStorage;
import com.josebieco.strideandsire.attachment.HorseshoeStorage;
import com.josebieco.strideandsire.attachment.SaddlebagStorage;
import com.josebieco.strideandsire.network.ModNetwork;
import net.minecraft.resources.Identifier;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

/**
 * Per-entity data attachments. Forge 66 has no NeoForge-style AttachmentType, so each attachment is a Forge
 * capability (auto-registered via {@code @AutoRegisterCapability}) attached to eligible horses: the horseshoe
 * ({@link HorseshoeStorage}) under {@link #HORSESHOE_ID}, default
 * {@link com.josebieco.strideandsire.attachment.HorseshoeAttachment#EMPTY}; the saddlebag ({@link SaddlebagStorage})
 * under {@link #SADDLEBAG_ID}, default {@link com.josebieco.strideandsire.attachment.SaddlebagAttachment#EMPTY};
 * and the genetics attachment ({@link HorseGeneticsStorage}) under {@link #GENETICS_ID}, default
 * {@link com.josebieco.strideandsire.attachment.HorseGeneticsAttachment#EMPTY}.
 */
public final class ModAttachments {
    public static final Identifier HORSESHOE_ID = Identifier.fromNamespaceAndPath(StrideAndSire.MODID, "horseshoe");
    public static final Capability<HorseshoeStorage> HORSESHOE = CapabilityManager.get(new CapabilityToken<>() {});
    public static final Identifier SADDLEBAG_ID = Identifier.fromNamespaceAndPath(StrideAndSire.MODID, "saddlebag");
    public static final Capability<SaddlebagStorage> SADDLEBAG = CapabilityManager.get(new CapabilityToken<>() {});
    public static final Identifier GENETICS_ID = Identifier.fromNamespaceAndPath(StrideAndSire.MODID, "genetics");
    public static final Capability<HorseGeneticsStorage> GENETICS = CapabilityManager.get(new CapabilityToken<>() {});

    private ModAttachments() {}

    /** Registers the game-bus listeners (attach, tracking sync, death drop) and the sync channel. */
    public static void register() {
        ModNetwork.init();
        HorseEquipmentEvents.register();
    }
}
