package com.josebieco.strideandsire.attachment;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.ItemStack;

/** The mod's per-horse equipment slots; used as the discriminator of the shared client sync packet. */
public enum HorseEquipmentSlot {
    HORSESHOE(HorseshoeAccess::get, HorseshoeAccess::set),
    SADDLEBAG(SaddlebagAccess::get, SaddlebagAccess::set);

    private static final HorseEquipmentSlot[] VALUES = values();

    public static final StreamCodec<ByteBuf, HorseEquipmentSlot> STREAM_CODEC =
        ByteBufCodecs.VAR_INT.map(HorseEquipmentSlot::byOrdinal, HorseEquipmentSlot::ordinal);

    private final Function<AbstractHorse, ItemStack> getter;
    private final BiConsumer<AbstractHorse, ItemStack> setter;

    HorseEquipmentSlot(Function<AbstractHorse, ItemStack> getter, BiConsumer<AbstractHorse, ItemStack> setter) {
        this.getter = getter;
        this.setter = setter;
    }

    /** Decodes a wire ordinal; rejects out-of-range values (corrupt packet / protocol mismatch) with a clear error. */
    public static HorseEquipmentSlot byOrdinal(int ordinal) {
        if (ordinal < 0 || ordinal >= VALUES.length) {
            throw new DecoderException(
                "Unknown HorseEquipmentSlot ordinal " + ordinal + " (expected 0.." + (VALUES.length - 1) + ")");
        }
        return VALUES[ordinal];
    }

    /** Delegates to the slot's {@code *Access.get} (live stack). */
    public ItemStack get(AbstractHorse horse) {
        return getter.apply(horse);
    }

    /** Delegates to the slot's {@code *Access.set} (write choke point). */
    public void set(AbstractHorse horse, ItemStack stack) {
        setter.accept(horse, stack);
    }
}
