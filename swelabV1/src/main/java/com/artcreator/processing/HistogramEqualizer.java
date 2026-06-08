package com.artcreator.processing;

import com.artcreator.util.ImageOps;

import java.awt.image.BufferedImage;

/**
 * Histogram equalization in the luma channel (Y in YCbCr) to enhance contrast
 * without altering hue. The image is mutated in place.
 */
public final class HistogramEqualizer {

    private HistogramEqualizer() { /* static */ }

    public static void equalizeLuma(BufferedImage img) {
        int[] pixels = ImageOps.getPixels(img);

        // 1) extract luma + chroma in floats
        int n = pixels.length;
        int[] yArr = new int[n];
        double[] cb = new double[n];
        double[] cr = new double[n];
        int[] hist = new int[256];

        for (int i = 0; i < n; i++) {
            int rgb = pixels[i];
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >>  8) & 0xFF;
            int b =  rgb        & 0xFF;
            // Rec.601 luma
            int y = (int) Math.round(0.299 * r + 0.587 * g + 0.114 * b);
            y = ImageOps.clamp8(y);
            yArr[i] = y;
            cb[i] = -0.168736 * r - 0.331264 * g + 0.5      * b;
            cr[i] =  0.5      * r - 0.418688 * g - 0.081312 * b;
            hist[y]++;
        }

        // 2) cdf
        int[] cdf = new int[256];
        cdf[0] = hist[0];
        for (int i = 1; i < 256; i++) cdf[i] = cdf[i - 1] + hist[i];

        // find first non-zero cdf value
        int cdfMin = 0;
        for (int v : cdf) if (v != 0) { cdfMin = v; break; }
        int denom = Math.max(1, n - cdfMin);

        int[] mapping = new int[256];
        for (int i = 0; i < 256; i++) {
            mapping[i] = ImageOps.clamp8((int) Math.round((cdf[i] - cdfMin) * 255.0 / denom));
        }

        // 3) reconstruct RGB from new luma + original chroma
        for (int i = 0; i < n; i++) {
            double y = mapping[yArr[i]];
            double r = y                          + 1.402   * cr[i];
            double g = y - 0.344136 * cb[i] - 0.714136 * cr[i];
            double b = y + 1.772    * cb[i];
            pixels[i] = ImageOps.packRgb(
                (int) Math.round(r),
                (int) Math.round(g),
                (int) Math.round(b));
        }
        ImageOps.setPixels(img, pixels);
    }
}
