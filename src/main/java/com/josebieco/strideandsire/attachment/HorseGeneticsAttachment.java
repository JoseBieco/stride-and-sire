package com.josebieco.strideandsire.attachment;

import com.josebieco.strideandsire.genetics.LineageTrait;
import com.josebieco.strideandsire.genetics.TraitState;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.network.codec.StreamCodec;

/**
 * Immutable per-horse genetics attachment storing the {@link TraitState} of every {@link LineageTrait}.
 */
public record HorseGeneticsAttachment(Map<LineageTrait, TraitState> traits) {
    public static final HorseGeneticsAttachment EMPTY = new HorseGeneticsAttachment(Map.of());

    public static final Codec<HorseGeneticsAttachment> CODEC =
        Codec.unboundedMap(LineageTrait.CODEC, TraitState.CODEC)
            .xmap(HorseGeneticsAttachment::of, HorseGeneticsAttachment::traits);

    public static final StreamCodec<ByteBuf, HorseGeneticsAttachment> STREAM_CODEC = StreamCodec.of(
        (buf, attachment) -> {
            for (LineageTrait trait : LineageTrait.values()) {
                TraitState.STREAM_CODEC.encode(buf, attachment.state(trait));
            }
        },
        buf -> {
            EnumMap<LineageTrait, TraitState> map = new EnumMap<>(LineageTrait.class);
            for (LineageTrait trait : LineageTrait.values()) {
                map.put(trait, TraitState.STREAM_CODEC.decode(buf));
            }
            return of(map);
        }
    );

    public HorseGeneticsAttachment {
        Objects.requireNonNull(traits, "traits");
        EnumMap<LineageTrait, TraitState> normalized = new EnumMap<>(LineageTrait.class);
        for (LineageTrait trait : LineageTrait.values()) {
            TraitState state = traits.getOrDefault(trait, TraitState.ABSENT);
            normalized.put(trait, state == null ? TraitState.ABSENT : state);
        }
        traits = Collections.unmodifiableMap(normalized);
    }

    public static HorseGeneticsAttachment of(Map<LineageTrait, TraitState> traits) {
        HorseGeneticsAttachment built = new HorseGeneticsAttachment(traits);
        return built.isEmpty() ? EMPTY : built;
    }

    public TraitState state(LineageTrait trait) {
        return traits.getOrDefault(trait, TraitState.ABSENT);
    }

    public boolean isManifest(LineageTrait trait) {
        return state(trait).isManifest();
    }

    public boolean isCarrier(LineageTrait trait) {
        return state(trait).isCarrier();
    }

    public HorseGeneticsAttachment with(LineageTrait trait, TraitState state) {
        Objects.requireNonNull(trait, "trait");
        Objects.requireNonNull(state, "state");
        if (state(trait) == state) {
            return this;
        }
        EnumMap<LineageTrait, TraitState> copy = new EnumMap<>(traits);
        copy.put(trait, state);
        return of(copy);
    }

    public boolean isEmpty() {
        for (TraitState s : traits.values()) {
            if (s != TraitState.ABSENT) {
                return false;
            }
        }
        return true;
    }
}
