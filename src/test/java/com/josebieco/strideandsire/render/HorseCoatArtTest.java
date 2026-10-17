package com.josebieco.strideandsire.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Pins the art of the exclusive coat textures (drawn by tools/pixelart/generate.py) against the MC 26.3 adult horse
 * box-UV layout in AbstractEquineModel.createBodyMesh.
 *
 * <p>Head: texOffs(0, 13), box 6 (w) x 5 (h) x 7 (d) -> side row y 20..24: west side x 0..6 (front column x 6),
 * front x 7..12, east side x 13..19 (front column x 13), back x 20..25.
 * Legs (all four share the UV): texOffs(48, 21), box 4 x 11 x 4 -> side row y 25..35 with faces x 48..51, 52..55,
 * 56..59, 60..63; the sole (bottom face) is x 56..59, y 21..24.
 */
class HorseCoatArtTest {
    private static final int HEAD_SIDE_TOP = 20;
    private static final int HEAD_SIDE_BOTTOM = 24;
    /** {first x, last x, front x} of the two head side faces. */
    private static final int[][] HEAD_SIDES = {{0, 6, 6}, {13, 19, 13}};
    /** Manhattan RGB distance from the face's median coat colour that marks a pixel as part of the eye. */
    private static final int EYE_CONTRAST = 90;
    /** Minimum contrast inside the eye (iris vs highlight or iris shades). */
    private static final int EYE_DETAIL_CONTRAST = 60;
    private static final int HOOF_CONTRAST = 40;

    private static BufferedImage load(String coat) throws IOException {
        String path = "/assets/strideandsire/textures/entity/horse/" + coat + ".png";
        try (InputStream stream = HorseCoatArtTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, "missing coat texture " + path);
            BufferedImage img = ImageIO.read(stream);
            assertNotNull(img, "could not decode " + path);
            return img;
        }
    }

    private static int[] rgb(int argb) {
        return new int[] {(argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF};
    }

    private static int distance(int[] a, int[] b) {
        return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]) + Math.abs(a[2] - b[2]);
    }

    private static int[] median(List<int[]> colours) {
        int[] out = new int[3];
        for (int c = 0; c < 3; c++) {
            final int channel = c;
            int[] values = colours.stream().mapToInt(p -> p[channel]).sorted().toArray();
            out[c] = values[values.length / 2];
        }
        return out;
    }

    private static int[] mean(BufferedImage img, int x0, int y0, int x1, int y1) {
        long[] sum = new long[3];
        int n = 0;
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                int[] p = rgb(img.getRGB(x, y));
                for (int c = 0; c < 3; c++) {
                    sum[c] += p[c];
                }
                n++;
            }
        }
        return new int[] {(int) (sum[0] / n), (int) (sum[1] / n), (int) (sum[2] / n)};
    }

    @ParameterizedTest
    @ValueSource(strings = {"obsidian", "albino", "silver_dapple"})
    void coatIs64SquareAndFullyOpaque(String coat) throws IOException {
        BufferedImage img = load(coat);
        assertEquals(64, img.getWidth());
        assertEquals(64, img.getHeight());
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                assertEquals(0xFF, img.getRGB(x, y) >>> 24, coat + ": translucent texel at " + x + "," + y);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"obsidian", "albino", "silver_dapple"})
    void eyesSitInsideBothHeadSideFacesNearTheFront(String coat) throws IOException {
        BufferedImage img = load(coat);
        List<List<int[]>> eyes = new ArrayList<>();
        for (int[] side : HEAD_SIDES) {
            List<int[]> face = new ArrayList<>();
            for (int y = HEAD_SIDE_TOP; y <= HEAD_SIDE_BOTTOM; y++) {
                for (int x = side[0]; x <= side[1]; x++) {
                    face.add(rgb(img.getRGB(x, y)));
                }
            }
            int[] coatColour = median(face);
            List<int[]> eye = new ArrayList<>();
            for (int y = HEAD_SIDE_TOP; y <= HEAD_SIDE_BOTTOM; y++) {
                for (int x = side[0]; x <= side[1]; x++) {
                    if (distance(rgb(img.getRGB(x, y)), coatColour) >= EYE_CONTRAST) {
                        eye.add(new int[] {x, y});
                    }
                }
            }
            String where = coat + " head side x " + side[0] + ".." + side[1];
            assertTrue(eye.size() >= 2 && eye.size() <= 6,
                where + ": expected a 2..6 px eye clearly unlike the coat " + Arrays.toString(coatColour) + ", found " + eye.size());
            for (int[] p : eye) {
                String at = where + ": eye pixel " + p[0] + "," + p[1];
                assertTrue(p[0] > side[0] && p[0] < side[1], at + " touches the face's side edge");
                assertTrue(p[1] > HEAD_SIDE_TOP && p[1] < HEAD_SIDE_BOTTOM, at + " touches the face's top/bottom edge");
                assertTrue(Math.abs(p[0] - side[2]) <= 3, at + " is not near the front of the head");
            }
            int contrast = 0;
            for (int[] a : eye) {
                for (int[] b : eye) {
                    contrast = Math.max(contrast, distance(rgb(img.getRGB(a[0], a[1])), rgb(img.getRGB(b[0], b[1]))));
                }
            }
            assertTrue(contrast >= EYE_DETAIL_CONTRAST, where + ": eye needs an iris plus a highlight pixel, max contrast " + contrast);
            eyes.add(eye);
        }
        // The two side faces are mirror images (west x <-> east 19 - x), so both eyes sit at the same spot on the head.
        List<String> west = eyes.get(0).stream().map(p -> (19 - p[0]) + "," + p[1]).sorted().toList();
        List<String> east = eyes.get(1).stream().map(p -> p[0] + "," + p[1]).sorted().toList();
        assertEquals(west, east, coat + ": eyes are not mirrored across the head");
    }

    @ParameterizedTest
    @ValueSource(strings = {"obsidian", "albino", "silver_dapple"})
    void hoovesAtTheBottomOfEveryLegFaceDifferFromTheUpperLeg(String coat) throws IOException {
        BufferedImage img = load(coat);
        for (int x0 = 48; x0 < 64; x0 += 4) {
            int[] upper = mean(img, x0, 25, x0 + 3, 28);
            int[] hoof = mean(img, x0, 34, x0 + 3, 35);
            assertTrue(distance(upper, hoof) >= HOOF_CONTRAST,
                coat + " leg face x " + x0 + ": hoof " + Arrays.toString(hoof) + " too close to upper leg " + Arrays.toString(upper));
        }
        int[] upper = mean(img, 48, 25, 63, 28);
        int[] sole = mean(img, 56, 21, 59, 24);
        assertTrue(distance(upper, sole) >= HOOF_CONTRAST,
            coat + ": hoof sole " + Arrays.toString(sole) + " too close to upper leg " + Arrays.toString(upper));
    }
}
