package com.artcreator.processing;

import com.artcreator.model.Parameters;
import com.artcreator.util.ImageOps;

import java.awt.image.BufferedImage;

/**
 * Pixel-level preprocessing of the source image: brightness, contrast, gamma,
 * saturation, histogram equalization and edge boost.
 *
 * <p>All operations work in sRGB on 8-bit channels. Saturation is computed in
 * HSL space using {@link java.awt.Color#RGBtoHSB(int, int, int, float[])}.</p>
 */
public final class ImagePreprocessor {

    private ImagePreprocessor() { /* static */ }

    /**
     * Apply all preprocessing steps in a fixed, sensible order:
     * <ol>
     *   <li>brightness</li>
     *   <li>contrast</li>
     *   <li>gamma</li>
     *   <li>saturation</li>
     *   <li>histogram equalization (luma channel only)</li>
     *   <li>edge boost (mix-in of Sobel edge map)</li>
     * </ol>
     *
     * <p>The input image is not modified; a fresh image is returned.</p>
     */
    public static BufferedImage apply(BufferedImage source, Parameters p) {
        BufferedImage out = ImageOps.cloneAsRgb(source);
        int[] pixels = ImageOps.getPixels(out);

        applyBrightnessContrastGamma(pixels, p.getBrightness(), p.getContrast(), p.getGamma());
        if (Math.abs(p.getSaturation() - 1.0) > 1e-6) {
            applySaturation(pixels, p.getSaturation());
        }
        ImageOps.setPixels(out, pixels);

        if (p.isHistogramEqualization()) {
            HistogramEqualizer.equalizeLuma(out);
        }
        if (p.getEdgeBoost() > 1e-6) {
            BufferedImage edges = EdgeDetector.sobelMagnitude(out);
            blendEdges(out, edges, p.getEdgeBoost());
        }
        return out;
    }

    /* =================================================================== */

    private static void applyBrightnessContrastGamma(int[] pixels,
                                                     double brightness,
                                                     double contrast,
                                                     double gamma) {
        // Lookup table for speed: same operation applies to every channel.
        int[] lut = new int[256];
        double bAdd = brightness * 255.0;
        // contrast in [-1..1] -> factor c
        double c = (1.0 + contrast) / Math.max(1e-6, (1.0 - contrast));
        double inv = 1.0 / Math.max(1e-3, gamma);
        for (int i = 0; i < 256; i++) {
            double v = i;
            v += bAdd;
            v = (v - 128.0) * c + 128.0;
            v = ImageOps.clamp(v, 0.0, 255.0);
            v = 255.0 * Math.pow(v / 255.0, inv);
            lut[i] = ImageOps.clamp8((int) Math.round(v));
        }
        for (int i = 0; i < pixels.length; i++) {
            int rgb = pixels[i];
            int r = lut[(rgb >> 16) & 0xFF];
            int g = lut[(rgb >>  8) & 0xFF];
            int b = lut[ rgb        & 0xFF];
            pixels[i] = ImageOps.packRgb(r, g, b);
        }
    }

    private static void applySaturation(int[] pixels, double sat) {
        float[] hsb = new float[3];
        for (int i = 0; i < pixels.length; i++) {
            int rgb = pixels[i];
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >>  8) & 0xFF;
            int b =  rgb        & 0xFF;
            java.awt.Color.RGBtoHSB(r, g, b, hsb);
            hsb[1] = (float) ImageOps.clamp(hsb[1] * sat, 0.0, 1.0);
            int newRgb = java.awt.Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
            pixels[i] = newRgb & 0x00FFFFFF;
        }
    }

    private static void blendEdges(BufferedImage target, BufferedImage edges, double mix) {
        int w = target.getWidth(), h = target.getHeight();
        int[] tgt = ImageOps.getPixels(target);
        int[] edg = ImageOps.getPixels(edges);
        double m = ImageOps.clamp(mix, 0.0, 1.0);
        for (int i = 0; i < tgt.length; i++) {
            int e = edg[i] & 0xFF;     // edge magnitude is encoded as grey in edge image
            int r = (tgt[i] >> 16) & 0xFF;
            int g = (tgt[i] >>  8) & 0xFF;
            int b =  tgt[i]        & 0xFF;
            // Subtract scaled edge to darken near edges (simple sharpen-like effect).
            r = ImageOps.clamp8((int) Math.round(r - m * e));
            g = ImageOps.clamp8((int) Math.round(g - m * e));
            b = ImageOps.clamp8((int) Math.round(b - m * e));
            tgt[i] = ImageOps.packRgb(r, g, b);
        }
        ImageOps.setPixels(target, tgt);
        // suppress unused warning
        @SuppressWarnings("unused") int wh = w * h;
    }
}
