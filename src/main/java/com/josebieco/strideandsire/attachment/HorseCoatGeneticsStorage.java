package com.josebieco.strideandsire.attachment;

import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.common.capabilities.Capability;

/**
 * Per-entity holder for a {@link HorseCoatGeneticsAttachment}, attached only to vanilla {@code Horse} entities under
 * {@code strideandsire:coat_genetics}.
 */
@AutoRegisterCapability
public final class HorseCoatGeneticsStorage extends HorseEquipmentStorage<HorseCoatGeneticsAttachment> {
    public HorseCoatGeneticsStorage() {
        super(HorseCoatGeneticsAttachment.CODEC, HorseCoatGeneticsAttachment.EMPTY);
    }

    @Override
    protected Capability<?> capability() {
        // Wired to ModAttachments.COAT_GENETICS in Task 4
        return null;
    }
}
