package com.josebieco.strideandsire.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import net.minecraft.world.item.ItemStack;

/**
 * The saddlebag equipped on an eligible {@link net.minecraft.world.entity.animal.equine.AbstractHorse}; its container
 * contents travel inside the stack's {@code container} data component. Stored per entity through
 * {@link SaddlebagStorage} (Forge capability {@code strideandsire:saddlebag}); read and write it only through
 * {@link SaddlebagAccess}.
 */
public record SaddlebagAttachment(ItemStack stack) {
    public static final SaddlebagAttachment EMPTY = new SaddlebagAttachment(ItemStack.EMPTY);

    public static final Codec<SaddlebagAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ItemStack.OPTIONAL_CODEC.optionalFieldOf("stack", ItemStack.EMPTY).forGetter(SaddlebagAttachment::stack)
    ).apply(instance, SaddlebagAttachment::of));

    public SaddlebagAttachment {
        Objects.requireNonNull(stack, "stack");
    }

    /** Returns {@link #EMPTY} for an empty stack, otherwise a new attachment holding {@code stack}. */
    public static SaddlebagAttachment of(ItemStack stack) {
        return stack.isEmpty() ? EMPTY : new SaddlebagAttachment(stack);
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }
}
