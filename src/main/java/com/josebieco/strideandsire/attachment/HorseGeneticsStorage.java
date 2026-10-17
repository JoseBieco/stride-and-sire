package com.josebieco.strideandsire.attachment;

import com.josebieco.strideandsire.registry.ModAttachments;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.common.capabilities.Capability;

/**
 * Per-entity holder for a {@link HorseGeneticsAttachment}, attached to eligible horses as the Forge capability
 * {@link ModAttachments#GENETICS} under {@code strideandsire:genetics}.
 */
@AutoRegisterCapability
public final class HorseGeneticsStorage extends HorseEquipmentStorage<HorseGeneticsAttachment> {
    public HorseGeneticsStorage() {
        super(HorseGeneticsAttachment.CODEC, HorseGeneticsAttachment.EMPTY);
    }

    @Override
    protected Capability<?> capability() {
        return ModAttachments.GENETICS;
    }
}
