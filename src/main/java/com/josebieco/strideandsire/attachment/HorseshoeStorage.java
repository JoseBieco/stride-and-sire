package com.josebieco.strideandsire.attachment;

import com.josebieco.strideandsire.registry.ModAttachments;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.common.capabilities.Capability;

/**
 * Per-entity holder for a {@link HorseshoeAttachment}, attached to eligible horses as the Forge capability
 * {@link ModAttachments#HORSESHOE}. Forge persists it inside the entity's {@code ForgeCaps} tag under the key
 * {@code strideandsire:horseshoe}. Not for direct use: go through {@link HorseshoeAccess}, which also syncs.
 */
@AutoRegisterCapability
public final class HorseshoeStorage extends HorseEquipmentStorage<HorseshoeAttachment> {
    public HorseshoeStorage() {
        super(HorseshoeAttachment.CODEC, HorseshoeAttachment.EMPTY);
    }

    @Override
    protected Capability<?> capability() {
        return ModAttachments.HORSESHOE;
    }
}
