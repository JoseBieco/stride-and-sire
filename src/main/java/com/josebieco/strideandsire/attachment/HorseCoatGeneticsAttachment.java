package com.josebieco.strideandsire.attachment;

import com.josebieco.strideandsire.genetics.CoatGene;
import com.josebieco.strideandsire.genetics.Zygosity;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.Nullable;

/**
 * Immutable per-horse recessive coat genetics attachment storing the {@link Zygosity} of every {@link CoatGene} and the
 * chosen {@link #manifestGeneToRender} when at least one coat gene is {@link Zygosity#HOMOZYGOUS_RECESSIVE}.
 */
public record HorseCoatGeneticsAttachment(
    Map<CoatGene, Zygosity> genes,
    @Nullable CoatGene manifestGeneToRender
) {
    public static final HorseCoatGeneticsAttachment EMPTY = new HorseCoatGeneticsAttachment(Map.of(), null);

    public static final Codec<HorseCoatGeneticsAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.unboundedMap(CoatGene.CODEC, Zygosity.CODEC)
            .optionalFieldOf("genes", Map.of())
            .forGetter(HorseCoatGeneticsAttachment::genes),
        CoatGene.CODEC.optionalFieldOf("manifest_gene_to_render")
            .forGetter(a -> Optional.ofNullable(a.manifestGeneToRender))
    ).apply(instance, (genes, renderGene) -> EMPTY.withGenes(genes, renderGene.orElse(null))));

    public static final StreamCodec<ByteBuf, HorseCoatGeneticsAttachment> STREAM_CODEC = StreamCodec.of(
        (buf, attachment) -> {
            for (CoatGene gene : CoatGene.values()) {
                Zygosity.STREAM_CODEC.encode(buf, attachment.zygosity(gene));
            }
            boolean hasRenderGene = attachment.manifestGeneToRender != null;
            buf.writeBoolean(hasRenderGene);
            if (hasRenderGene) {
                CoatGene.STREAM_CODEC.encode(buf, attachment.manifestGeneToRender);
            }
        },
        buf -> {
            EnumMap<CoatGene, Zygosity> map = new EnumMap<>(CoatGene.class);
            for (CoatGene gene : CoatGene.values()) {
                map.put(gene, Zygosity.STREAM_CODEC.decode(buf));
            }
            CoatGene renderGene = buf.readBoolean() ? CoatGene.STREAM_CODEC.decode(buf) : null;
            return EMPTY.withGenes(map, renderGene);
        }
    );

    public HorseCoatGeneticsAttachment {
        Objects.requireNonNull(genes, "genes");
        EnumMap<CoatGene, Zygosity> normalized = new EnumMap<>(CoatGene.class);
        for (CoatGene gene : CoatGene.values()) {
            Zygosity z = genes.getOrDefault(gene, Zygosity.HOMOZYGOUS_DOMINANT);
            normalized.put(gene, z == null ? Zygosity.HOMOZYGOUS_DOMINANT : z);
        }
        genes = Collections.unmodifiableMap(normalized);
        if (manifestGeneToRender != null && !normalized.get(manifestGeneToRender).isManifest()) {
            manifestGeneToRender = null;
        }
    }

    public Zygosity zygosity(CoatGene gene) {
        return genes.getOrDefault(gene, Zygosity.HOMOZYGOUS_DOMINANT);
    }

    public boolean isManifest(CoatGene gene) {
        return zygosity(gene).isManifest();
    }

    public boolean isCarrier(CoatGene gene) {
        return zygosity(gene).isCarrier();
    }

    public Set<CoatGene> manifestGenes() {
        EnumSet<CoatGene> result = EnumSet.noneOf(CoatGene.class);
        for (Map.Entry<CoatGene, Zygosity> entry : genes.entrySet()) {
            if (entry.getValue().isManifest()) {
                result.add(entry.getKey());
            }
        }
        return Collections.unmodifiableSet(result);
    }

    public HorseCoatGeneticsAttachment withGenes(
        Map<CoatGene, Zygosity> genes,
        @Nullable CoatGene manifestGeneToRender
    ) {
        HorseCoatGeneticsAttachment built = new HorseCoatGeneticsAttachment(genes, manifestGeneToRender);
        return built.isEmpty() ? EMPTY : built;
    }

    public boolean isEmpty() {
        if (manifestGeneToRender != null) {
            return false;
        }
        for (Zygosity z : genes.values()) {
            if (z != Zygosity.HOMOZYGOUS_DOMINANT) {
                return false;
            }
        }
        return true;
    }
}
