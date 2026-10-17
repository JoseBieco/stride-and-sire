package com.josebieco.strideandsire.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.josebieco.strideandsire.StrideAndSire;
import com.josebieco.strideandsire.attachment.HorseCoatGeneticsAttachment;
import com.josebieco.strideandsire.genetics.CoatGene;
import com.josebieco.strideandsire.genetics.Zygosity;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import javax.imageio.ImageIO;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.junit.jupiter.api.Test;

class HorseCoatRenderingTest {
    @Test
    void textureToRenderIsNullWhenNoGeneManifest() {
        assertNull(HorseCoatRendering.textureToRender((AbstractHorse) null));
        assertNull(HorseCoatRendering.textureToRender((HorseCoatGeneticsAttachment) null));
        assertNull(HorseCoatRendering.textureToRender(HorseCoatGeneticsAttachment.EMPTY));

        HorseCoatGeneticsAttachment carrierOnly = HorseCoatGeneticsAttachment.EMPTY.withGenes(
            Map.of(
                CoatGene.OBSIDIAN, Zygosity.HETEROZYGOUS,
                CoatGene.ALBINO, Zygosity.HETEROZYGOUS,
                CoatGene.SILVER_DAPPLE, Zygosity.HETEROZYGOUS),
            null);
        assertNull(HorseCoatRendering.textureToRender(carrierOnly));
    }

    @Test
    void textureToRenderMatchesTheChosenManifestGene() throws IOException {
        for (CoatGene gene : CoatGene.values()) {
            HorseCoatGeneticsAttachment attachment = HorseCoatGeneticsAttachment.EMPTY.withGenes(
                Map.of(gene, Zygosity.HOMOZYGOUS_RECESSIVE),
                gene);
            Identifier expected = Identifier.fromNamespaceAndPath(
                StrideAndSire.MODID,
                "textures/entity/horse/" + gene.getSerializedName() + ".png");
            Identifier actual = HorseCoatRendering.textureToRender(attachment);
            assertEquals(expected, actual);
            assertEquals(expected, HorseCoatRendering.coatTexture(gene));

            String resourcePath = "/assets/" + expected.getNamespace() + "/" + expected.getPath();
            try (InputStream stream = HorseCoatRenderingTest.class.getResourceAsStream(resourcePath)) {
                assertNotNull(stream, "Missing coat texture resource on classpath: " + resourcePath);
                BufferedImage img = ImageIO.read(stream);
                assertNotNull(img, "Could not decode PNG: " + resourcePath);
                assertEquals(64, img.getWidth(), "Coat texture width must be 64: " + resourcePath);
                assertEquals(64, img.getHeight(), "Coat texture height must be 64: " + resourcePath);
            }
        }
    }
}
