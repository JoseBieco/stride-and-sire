package com.josebieco.strideandsire.attachment;

import com.josebieco.strideandsire.registry.ModAttachments;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.common.capabilities.Capability;

/**
 * Per-entity holder for a {@link HorseCoatGeneticsAttachment}, attached only to vanilla {@code Horse} entities as the
 * Forge capability {@link ModAttachments#COAT_GENETICS} under {@code strideandsire:coat_genetics}.
 */
@AutoRegisterCapability
public final class HorseCoatGeneticsStorage extends HorseEquipmentStorage<HorseCoatGeneticsAttachment> {
    public HorseCoatGeneticsStorage() {
        super(HorseCoatGeneticsAttachment.CODEC, HorseCoatGeneticsAttachment.EMPTY);
    }

    @Override
    protected Capability<?> capability() {
        return ModAttachments.COAT_GENETICS;
    }
}
