package com.artcreator.processing;

import com.artcreator.model.ColorPalette;
import com.artcreator.util.ImageOps;

import java.awt.image.BufferedImage;

/**
 * Floyd-Steinberg error-diffusion dithering.
 *
 * <p>The image is mapped to the palette in scanline order. The quantization
 * error at each pixel is propagated to its right and below-neighbours with
 * the classic 7/16, 3/16, 5/16, 1/16 weights, optionally scaled by the
 * {@code strength} parameter.</p>
 */
public final class Ditherer {

    private Ditherer() { /* static */ }

    /**
     * Returns a new image where every pixel is exactly one of the palette
     * colours; if {@code strength <= 0} this becomes plain nearest-neighbour
     * snapping (no dithering).
     */
    public static BufferedImage floydSteinberg(BufferedImage src,
                                               ColorPalette palette,
                                               double strength) {
        int w = src.getWidth(), h = src.getHeight();
        // working buffers in float so error can accumulate
        double[] r = new double[w * h];
        double[] g = new double[w * h];
        double[] b = new double[w * h];
        int[] inPixels = ImageOps.getPixels(src);
        for (int i = 0; i < inPixels.length; i++) {
            r[i] = (inPixels[i] >> 16) & 0xFF;
            g[i] = (inPixels[i] >>  8) & 0xFF;
            b[i] =  inPixels[i]        & 0xFF;
        }

        double s = ImageOps.clamp(strength, 0.0, 1.0);
        int[] out = new int[w * h];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int idx = y * w + x;
                int rOld = ImageOps.clamp8((int) Math.round(r[idx]));
                int gOld = ImageOps.clamp8((int) Math.round(g[idx]));
                int bOld = ImageOps.clamp8((int) Math.round(b[idx]));
                int rgbOld = (rOld << 16) | (gOld << 8) | bOld;
                ColorPalette.Entry e = palette.nearest(rgbOld);
                out[idx] = e.rgb;
                if (s <= 0.0) continue;
                double er = (rOld - e.r) * s;
                double eg = (gOld - e.g) * s;
                double eb = (bOld - e.b) * s;
                if (x + 1 < w) {
                    int j = idx + 1;
                    r[j] += er * 7.0 / 16.0;
                    g[j] += eg * 7.0 / 16.0;
                    b[j] += eb * 7.0 / 16.0;
                }
                if (y + 1 < h) {
                    if (x > 0) {
                        int j = idx + w - 1;
                        r[j] += er * 3.0 / 16.0;
                        g[j] += eg * 3.0 / 16.0;
                        b[j] += eb * 3.0 / 16.0;
                    }
                    int j = idx + w;
                    r[j] += er * 5.0 / 16.0;
                    g[j] += eg * 5.0 / 16.0;
                    b[j] += eb * 5.0 / 16.0;
                    if (x + 1 < w) {
                        int j2 = idx + w + 1;
                        r[j2] += er * 1.0 / 16.0;
                        g[j2] += eg * 1.0 / 16.0;
                        b[j2] += eb * 1.0 / 16.0;
                    }
                }
            }
        }
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        ImageOps.setPixels(img, out);
        return img;
    }

    /** Plain nearest-neighbour palette snap (no error diffusion). */
    public static BufferedImage snap(BufferedImage src, ColorPalette palette) {
        return floydSteinberg(src, palette, 0.0);
    }
}
