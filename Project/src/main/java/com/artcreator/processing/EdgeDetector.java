package com.artcreator.processing;

import com.artcreator.util.ImageOps;

import java.awt.image.BufferedImage;

/**
 * Sobel edge detection.
 *
 * <p>{@link #sobelMagnitude(BufferedImage)} returns an 8-bit grey-scale
 * image where the intensity at each pixel is the magnitude of the Sobel
 * gradient at that point. The result image has the same dimensions as the
 * input.</p>
 *
 * <p>The implementation operates on luminance only and is good enough for
 * the "edge boost" pre-processing pass; it is not a Canny detector but is
 * sufficient for highlighting structure prior to quantization.</p>
 */
public final class EdgeDetector {

    private EdgeDetector() { /* static */ }

    public static BufferedImage sobelMagnitude(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        int[] in = ImageOps.getPixels(src);

        // luminance
        int[] lum = new int[w * h];
        for (int i = 0; i < in.length; i++) {
            int r = (in[i] >> 16) & 0xFF;
            int g = (in[i] >>  8) & 0xFF;
            int b =  in[i]        & 0xFF;
            lum[i] = (int) Math.round(0.299*r + 0.587*g + 0.114*b);
        }

        int[] out = new int[w * h];
        int max = 1;
        // First pass: compute raw magnitudes and the running max for normalization.
        int[] mag = new int[w * h];
        for (int y = 1; y < h - 1; y++) {
            int row0 = (y - 1) * w;
            int row1 =  y      * w;
            int row2 = (y + 1) * w;
            for (int x = 1; x < w - 1; x++) {
                int gx = - lum[row0 + x - 1] - 2 * lum[row1 + x - 1] - lum[row2 + x - 1]
                         + lum[row0 + x + 1] + 2 * lum[row1 + x + 1] + lum[row2 + x + 1];
                int gy = - lum[row0 + x - 1] - 2 * lum[row0 + x]    - lum[row0 + x + 1]
                         + lum[row2 + x - 1] + 2 * lum[row2 + x]    + lum[row2 + x + 1];
                int m = (int) Math.round(Math.sqrt((double) gx*gx + (double) gy*gy));
                mag[row1 + x] = m;
                if (m > max) max = m;
            }
        }
        // Normalize to 0..255
        double scale = 255.0 / max;
        for (int i = 0; i < mag.length; i++) {
            int v = ImageOps.clamp8((int) Math.round(mag[i] * scale));
            out[i] = ImageOps.packRgb(v, v, v);
        }
        BufferedImage edge = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        ImageOps.setPixels(edge, out);
        return edge;
    }
}
