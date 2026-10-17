package com.josebieco.strideandsire.assets;

import com.josebieco.strideandsire.item.horseshoe.HorseshoeMaterial;
import com.josebieco.strideandsire.item.saddlebag.SaddlebagMaterial;
import com.josebieco.strideandsire.render.SaddlebagRendering;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Every material tier must ship its textures; a missing one renders as the purple/black
 * missing texture in game. Iterates the enums so a new tier without assets fails here.
 */
class AssetCompletenessTest {

    private static final String TEXTURES = "/assets/strideandsire/textures/";

    @Test
    void everyHorseshoeTierHasItemAndHoofTexture() {
        for (HorseshoeMaterial m : HorseshoeMaterial.values()) {
            String name = m.name().toLowerCase(Locale.ROOT);
            assertExists("item/" + name + "_horseshoe.png");
            assertExists("entity/horseshoe/" + name + ".png");
        }
    }

    @Test
    void everySaddlebagTierHasItemTexture() {
        for (SaddlebagMaterial m : SaddlebagMaterial.values()) {
            assertExists("item/" + m.name().toLowerCase(Locale.ROOT) + "_saddlebag.png");
        }
    }

    /** The 3D side-bag model ({@code client.SaddlebagModel}) samples these with its own UV layout and texture size. */
    @Test
    void everySaddlebagTierHasModelTexture() throws IOException {
        for (SaddlebagMaterial m : SaddlebagMaterial.values()) {
            String path = "entity/saddlebag/" + m.name().toLowerCase(Locale.ROOT) + ".png";
            URL url = assertExists(path);
            try (InputStream in = url.openStream()) {
                BufferedImage image = ImageIO.read(in);
                assertNotNull(image, path + " is not a decodable PNG");
                assertEquals(SaddlebagRendering.MODEL_TEXTURE_WIDTH, image.getWidth(), path + " width");
                assertEquals(SaddlebagRendering.MODEL_TEXTURE_HEIGHT, image.getHeight(), path + " height");
            }
        }
    }

    @Test
    void emptySlotSpritesExist() throws IOException {
        for (String sprite : new String[] {"horseshoe", "saddlebag"}) {
            URL url = assertExists("gui/sprites/container/slot/" + sprite + ".png");
            try (InputStream in = url.openStream()) {
                BufferedImage image = ImageIO.read(in);
                assertNotNull(image, sprite + " slot sprite is not a decodable PNG");
                assertEquals(16, image.getWidth(), sprite + " slot sprite width");
                assertEquals(16, image.getHeight(), sprite + " slot sprite height");
            }
        }
    }

    private URL assertExists(String relative) {
        URL url = getClass().getResource(TEXTURES + relative);
        assertNotNull(url, "missing texture " + TEXTURES + relative);
        return url;
    }
}
