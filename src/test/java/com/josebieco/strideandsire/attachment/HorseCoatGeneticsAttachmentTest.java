package com.josebieco.strideandsire.attachment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.josebieco.strideandsire.genetics.CoatGene;
import com.josebieco.strideandsire.genetics.Zygosity;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HorseCoatGeneticsAttachmentTest {
    @Test
    void withGenesNormalizesMissingEntriesToHomozygousDominantAndIsEmptyWhenAllDominant() {
        HorseCoatGeneticsAttachment empty = HorseCoatGeneticsAttachment.EMPTY;
        assertTrue(empty.isEmpty());
        for (CoatGene gene : CoatGene.values()) {
            assertEquals(Zygosity.HOMOZYGOUS_DOMINANT, empty.zygosity(gene));
            assertFalse(empty.isManifest(gene));
        }
        assertTrue(empty.manifestGenes().isEmpty());
        assertNull(empty.manifestGeneToRender());

        HorseCoatGeneticsAttachment partial = HorseCoatGeneticsAttachment.EMPTY.withGenes(
            Map.of(CoatGene.ALBINO, Zygosity.HETEROZYGOUS),
            null);
        assertFalse(partial.isEmpty());
        assertEquals(Zygosity.HOMOZYGOUS_DOMINANT, partial.zygosity(CoatGene.OBSIDIAN));
        assertEquals(Zygosity.HETEROZYGOUS, partial.zygosity(CoatGene.ALBINO));
        assertEquals(Zygosity.HOMOZYGOUS_DOMINANT, partial.zygosity(CoatGene.SILVER_DAPPLE));
        assertTrue(partial.manifestGenes().isEmpty());
        assertNull(partial.manifestGeneToRender());

        HorseCoatGeneticsAttachment backToDominant = partial.withGenes(Map.of(), null);
        assertSame(HorseCoatGeneticsAttachment.EMPTY, backToDominant);
    }

    @Test
    void withGenesTracksManifestGenesAndChosenRenderGene() {
        EnumMap<CoatGene, Zygosity> map = new EnumMap<>(CoatGene.class);
        map.put(CoatGene.OBSIDIAN, Zygosity.HOMOZYGOUS_RECESSIVE);
        map.put(CoatGene.ALBINO, Zygosity.HETEROZYGOUS);
        map.put(CoatGene.SILVER_DAPPLE, Zygosity.HOMOZYGOUS_RECESSIVE);

        HorseCoatGeneticsAttachment attachment =
            HorseCoatGeneticsAttachment.EMPTY.withGenes(map, CoatGene.SILVER_DAPPLE);

        assertFalse(attachment.isEmpty());
        assertTrue(attachment.isManifest(CoatGene.OBSIDIAN));
        assertFalse(attachment.isManifest(CoatGene.ALBINO));
        assertTrue(attachment.isManifest(CoatGene.SILVER_DAPPLE));
        assertEquals(Set.of(CoatGene.OBSIDIAN, CoatGene.SILVER_DAPPLE), attachment.manifestGenes());
        assertEquals(CoatGene.SILVER_DAPPLE, attachment.manifestGeneToRender());
    }

    @Test
    void codecAndStreamCodecRoundTripPreservesGenesAndRenderGene() {
        HorseCoatGeneticsAttachment original = HorseCoatGeneticsAttachment.EMPTY.withGenes(
            Map.of(
                CoatGene.OBSIDIAN, Zygosity.HOMOZYGOUS_RECESSIVE,
                CoatGene.ALBINO, Zygosity.HETEROZYGOUS,
                CoatGene.SILVER_DAPPLE, Zygosity.HOMOZYGOUS_DOMINANT),
            CoatGene.OBSIDIAN);

        var json = HorseCoatGeneticsAttachment.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        HorseCoatGeneticsAttachment decodedCodec =
            HorseCoatGeneticsAttachment.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(original, decodedCodec);

        ByteBuf buf = Unpooled.buffer();
        try {
            HorseCoatGeneticsAttachment.STREAM_CODEC.encode(buf, original);
            HorseCoatGeneticsAttachment decodedStream = HorseCoatGeneticsAttachment.STREAM_CODEC.decode(buf);
            assertEquals(original, decodedStream);

            HorseCoatGeneticsAttachment.STREAM_CODEC.encode(buf, HorseCoatGeneticsAttachment.EMPTY);
            HorseCoatGeneticsAttachment decodedEmpty = HorseCoatGeneticsAttachment.STREAM_CODEC.decode(buf);
            assertSame(HorseCoatGeneticsAttachment.EMPTY, decodedEmpty);
        } finally {
            buf.release();
        }
    }
}
