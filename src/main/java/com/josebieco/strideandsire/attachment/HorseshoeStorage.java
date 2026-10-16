package com.josebieco.strideandsire.attachment;

import com.josebieco.strideandsire.registry.ModAttachments;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Per-entity holder for a {@link HorseshoeAttachment}, attached to eligible horses as the Forge capability
 * {@link ModAttachments#HORSESHOE}. Forge persists it inside the entity's {@code ForgeCaps} tag under the key
 * {@code strideandsire:horseshoe}. Not for direct use: go through {@link HorseshoeAccess}, which also syncs.
 */
@AutoRegisterCapability
public final class HorseshoeStorage implements ICapabilitySerializable<CompoundTag> {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final LazyOptional<HorseshoeStorage> self = LazyOptional.of(() -> this);
    private HorseshoeAttachment value = HorseshoeAttachment.EMPTY;

    HorseshoeAttachment value() {
        return value;
    }

    void setValue(HorseshoeAttachment value) {
        this.value = value;
    }

    void invalidate() {
        self.invalidate();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == ModAttachments.HORSESHOE ? self.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider registryAccess) {
        return HorseshoeAttachment.CODEC.encodeStart(registryAccess.createSerializationContext(NbtOps.INSTANCE), value)
            .resultOrPartial(error -> LOGGER.error("Failed to save horseshoe {}: {}", value.stack(), error))
            .filter(CompoundTag.class::isInstance)
            .map(CompoundTag.class::cast)
            .orElseGet(CompoundTag::new);
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider registryAccess, CompoundTag nbt) {
        value = HorseshoeAttachment.CODEC.parse(registryAccess.createSerializationContext(NbtOps.INSTANCE), nbt)
            .resultOrPartial(error -> LOGGER.error("Failed to load horseshoe from {}: {}", nbt, error))
            .orElse(HorseshoeAttachment.EMPTY);
    }
}
