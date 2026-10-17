package com.josebieco.strideandsire.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    private static final HorseCoatGeneticsAttachment MANIFEST_ALBINO = HorseCoatGeneticsAttachment.EMPTY.withGenes(
        Map.of(CoatGene.ALBINO, Zygosity.HOMOZYGOUS_RECESSIVE), CoatGene.ALBINO);

    @Test
    void overrideTextureIsDrawnOnlyForVisibleAdultsWithAManifestGene() {
        Identifier albino = HorseCoatRendering.coatTexture(CoatGene.ALBINO);
        assertEquals(albino, HorseCoatRendering.overrideTextureFor(false, false, MANIFEST_ALBINO));
        assertNull(HorseCoatRendering.overrideTextureFor(true, false, MANIFEST_ALBINO), "foals keep the vanilla coat");
        assertNull(HorseCoatRendering.overrideTextureFor(false, true, MANIFEST_ALBINO), "invisible horses draw no override");
        assertNull(HorseCoatRendering.overrideTextureFor(false, false, HorseCoatGeneticsAttachment.EMPTY));
        assertNull(HorseCoatRendering.overrideTextureFor(false, false, (HorseCoatGeneticsAttachment) null));
    }

    @Test
    void vanillaMarkingsAreSuppressedExactlyWhenTheOverrideRenders() {
        assertTrue(HorseCoatRendering.suppressesVanillaMarkings(HorseCoatRendering.overrideTextureFor(false, false, MANIFEST_ALBINO)));
        assertFalse(HorseCoatRendering.suppressesVanillaMarkings(HorseCoatRendering.overrideTextureFor(true, false, MANIFEST_ALBINO)));
        assertFalse(HorseCoatRendering.suppressesVanillaMarkings(
            HorseCoatRendering.overrideTextureFor(false, false, HorseCoatGeneticsAttachment.EMPTY)));
        assertFalse(HorseCoatRendering.suppressesVanillaMarkings(null));
    }

    @Test
    void horseshoesAreSubmittedStrictlyAfterTheCoatOverride() {
        // Both re-render the parent model with entityCutout (same solid phase); within one order the batch order between
        // RenderTypes is unspecified and the coplanar coat could overwrite the hooves.
        assertTrue(HorseshoeRendering.LAYER_ORDER > HorseCoatRendering.LAYER_ORDER,
            "horseshoe order " + HorseshoeRendering.LAYER_ORDER + " must exceed coat order " + HorseCoatRendering.LAYER_ORDER);
        assertTrue(SaddlebagRendering.LAYER_ORDER > HorseshoeRendering.LAYER_ORDER);
    }

    @Test
    void albinoEyesStayInsideTheHeadSideFaces() throws IOException {
        // Adult horse head: texOffs(0, 13), box 6x5x7 -> side row y 20..24: right side x 0..6, front x 7..12,
        // left side x 13..19, back x 20..25. Eyes must sit on the side faces, next to the front (mouth) edge.
        BufferedImage img;
        try (InputStream stream = HorseCoatRenderingTest.class.getResourceAsStream(
                "/assets/strideandsire/textures/entity/horse/albino.png")) {
            assertNotNull(stream);
            img = ImageIO.read(stream);
        }
        int base = img.getRGB(30, 40);
        int eyeCount = 0;
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                if (img.getRGB(x, y) == base || (y >= 36 && y < 54 && x >= 42)) {
                    continue;
                }
                eyeCount++;
                assertTrue(y >= 20 && y <= 24, "eye pixel outside head side row: " + x + "," + y);
                assertTrue((x >= 0 && x <= 6) || (x >= 13 && x <= 19), "eye pixel off the head side faces: " + x + "," + y);
            }
        }
        assertTrue(eyeCount > 0, "albino texture must have eye pixels");
    }
}
