package com.josebieco.strideandsire.attachment;

import com.josebieco.strideandsire.registry.ModAttachments;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.common.capabilities.Capability;

/**
 * Per-entity holder for a {@link SaddlebagAttachment}, attached to eligible horses as the Forge capability
 * {@link ModAttachments#SADDLEBAG}. Forge persists it (container contents included, as part of the stack's
 * {@code container} component) inside the entity's {@code ForgeCaps} tag under the key {@code strideandsire:saddlebag}.
 * Not for direct use: go through {@link SaddlebagAccess}, which also syncs.
 */
@AutoRegisterCapability
public final class SaddlebagStorage extends HorseEquipmentStorage<SaddlebagAttachment> {
    public SaddlebagStorage() {
        super(SaddlebagAttachment.CODEC, SaddlebagAttachment.EMPTY);
    }

    @Override
    protected Capability<?> capability() {
        return ModAttachments.SADDLEBAG;
    }
}
