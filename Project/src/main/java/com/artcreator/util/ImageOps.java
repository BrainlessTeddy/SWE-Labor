package com.artcreator.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Tiny grab-bag of static helpers for working with {@link BufferedImage}s.
 * Kept dependency-free so any module can use it.
 */
public final class ImageOps {

    private ImageOps() { /* no instances */ }

    /** Make a deep copy that always uses {@link BufferedImage#TYPE_INT_RGB}. */
    public static BufferedImage cloneAsRgb(BufferedImage src) {
        if (src == null) return null;
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(),
                                              BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        try { g.drawImage(src, 0, 0, null); } finally { g.dispose(); }
        return out;
    }

    /** Linear-interpolation high quality resize. */
    public static BufferedImage resize(BufferedImage src, int newWidth, int newHeight) {
        if (newWidth <= 0 || newHeight <= 0) {
            throw new IllegalArgumentException("Target size must be positive");
        }
        BufferedImage out = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                               RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                               RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(src, 0, 0, newWidth, newHeight, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    /**
     * Crop {@code src} to the rectangle (x, y, w, h), clamped to the image bounds.
     * Returns a fresh {@link BufferedImage}.
     */
    public static BufferedImage crop(BufferedImage src, int x, int y, int w, int h) {
        int sx = Math.max(0, Math.min(src.getWidth(),  x));
        int sy = Math.max(0, Math.min(src.getHeight(), y));
        int sw = Math.max(1, Math.min(src.getWidth()  - sx, w));
        int sh = Math.max(1, Math.min(src.getHeight() - sy, h));
        BufferedImage out = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        try {
            g.drawImage(src, 0, 0, sw, sh, sx, sy, sx + sw, sy + sh, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    /** Read all RGB pixels into a 1-D int array (length = w*h). */
    public static int[] getPixels(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int[] pixels = new int[w * h];
        img.getRGB(0, 0, w, h, pixels, 0, w);
        return pixels;
    }

    /** Write pixels back into the image. */
    public static void setPixels(BufferedImage img, int[] pixels) {
        img.setRGB(0, 0, img.getWidth(), img.getHeight(), pixels, 0, img.getWidth());
    }

    public static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    public static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    public static int clamp8(int v) { return clamp(v, 0, 255); }

    public static int packRgb(int r, int g, int b) {
        return ((clamp8(r) & 0xFF) << 16) | ((clamp8(g) & 0xFF) << 8) | (clamp8(b) & 0xFF);
    }

    /** Squared Euclidean distance between two RGB ints. */
    public static long rgbDistSq(int a, int b) {
        int dr = ((a >> 16) & 0xFF) - ((b >> 16) & 0xFF);
        int dg = ((a >>  8) & 0xFF) - ((b >>  8) & 0xFF);
        int db = ( a        & 0xFF) - ( b        & 0xFF);
        return (long) dr * dr + (long) dg * dg + (long) db * db;
    }
}
