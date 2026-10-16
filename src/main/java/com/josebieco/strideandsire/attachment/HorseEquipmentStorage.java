package com.josebieco.strideandsire.attachment;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Shared plumbing for a per-horse equipment value of type {@code A} (an immutable attachment record) stored as a Forge
 * capability and persisted through {@code codec} inside the entity's {@code ForgeCaps} tag. Each concrete subclass is its
 * own capability type (annotate it with {@code @AutoRegisterCapability}; the annotation is not inherited).
 * Not for direct use: go through the matching {@code *Access} class, which also syncs.
 */
public abstract class HorseEquipmentStorage<A> implements ICapabilitySerializable<CompoundTag> {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final LazyOptional<HorseEquipmentStorage<A>> self = LazyOptional.of(() -> this);
    private final Codec<A> codec;
    private final A empty;
    private A value;

    protected HorseEquipmentStorage(Codec<A> codec, A empty) {
        this.codec = codec;
        this.empty = empty;
        this.value = empty;
    }

    /** The capability this storage answers to. */
    protected abstract Capability<?> capability();

    A value() {
        return value;
    }

    void setValue(A value) {
        this.value = value;
    }

    void invalidate() {
        self.invalidate();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == capability() ? self.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider registryAccess) {
        return codec.encodeStart(registryAccess.createSerializationContext(NbtOps.INSTANCE), value)
            .resultOrPartial(error -> LOGGER.error("Failed to save {} {}: {}", getClass().getSimpleName(), value, error))
            .filter(CompoundTag.class::isInstance)
            .map(CompoundTag.class::cast)
            .orElseGet(CompoundTag::new);
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider registryAccess, CompoundTag nbt) {
        value = codec.parse(registryAccess.createSerializationContext(NbtOps.INSTANCE), nbt)
            .resultOrPartial(error -> LOGGER.error("Failed to load {} from {}: {}", getClass().getSimpleName(), nbt, error))
            .orElse(empty);
    }
}
