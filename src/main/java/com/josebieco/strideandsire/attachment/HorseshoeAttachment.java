package com.josebieco.strideandsire.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * The horseshoe equipped on an eligible {@link net.minecraft.world.entity.animal.equine.AbstractHorse}.
 * Stored per entity through {@link HorseshoeStorage} (Forge capability {@code strideandsire:horseshoe});
 * read and write it only through {@link HorseshoeAccess}.
 */
public record HorseshoeAttachment(ItemStack stack) {
    public static final HorseshoeAttachment EMPTY = new HorseshoeAttachment(ItemStack.EMPTY);

    public static final Codec<HorseshoeAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ItemStack.OPTIONAL_CODEC.optionalFieldOf("stack", ItemStack.EMPTY).forGetter(HorseshoeAttachment::stack)
    ).apply(instance, HorseshoeAttachment::of));

    public static final StreamCodec<RegistryFriendlyByteBuf, HorseshoeAttachment> STREAM_CODEC =
        ItemStack.OPTIONAL_STREAM_CODEC.map(HorseshoeAttachment::of, HorseshoeAttachment::stack);

    public HorseshoeAttachment {
        Objects.requireNonNull(stack, "stack");
    }

    /** Returns {@link #EMPTY} for an empty stack, otherwise a new attachment holding {@code stack}. */
    public static HorseshoeAttachment of(ItemStack stack) {
        return stack.isEmpty() ? EMPTY : new HorseshoeAttachment(stack);
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }
}
