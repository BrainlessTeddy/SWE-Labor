package com.artcreator.processing;

import com.artcreator.model.ColorPalette;
import com.artcreator.model.QuantizationMethod;
import com.artcreator.util.ImageOps;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Builds a palette of N colours from an input image using one of:
 * <ul>
 *   <li>k-means clustering (random init, weighted by pixel count after sub-sampling),</li>
 *   <li>median-cut (recursive split of the longest channel range),</li>
 *   <li>palette snap (no clustering: re-use a fixed palette).</li>
 * </ul>
 *
 * <p>The result is a {@link ColorPalette} whose entries are ordered by
 * descending pixel count (most-used colour first), which is friendlier to
 * the parts list and the build instructions.</p>
 */
public final class ColorQuantizer {

    private ColorQuantizer() { /* static */ }

    /** Build a palette with the requested number of colours. */
    public static ColorPalette buildPalette(BufferedImage img,
                                            int paletteSize,
                                            QuantizationMethod method,
                                            ColorPalette fallbackLockedPalette) {
        if (paletteSize < 2) paletteSize = 2;

        switch (method) {
            case PALETTE_SNAP:
                return fallbackLockedPalette;
            case MEDIAN_CUT:
                return medianCut(img, paletteSize);
            case K_MEANS:
            default:
                return kMeans(img, paletteSize);
        }
    }

    /* =================================================================
       k-means
       ================================================================= */

    private static ColorPalette kMeans(BufferedImage img, int k) {
        int[] pixels = ImageOps.getPixels(img);
        // Sub-sample at most ~50k pixels to keep this snappy for huge images.
        int[] sample = subSample(pixels, 50_000);

        // Init centroids randomly from the sample with a fixed seed so results
        // are reproducible across runs of the same image.
        Random rnd = new Random(0xC0FFEEL);
        double[][] cents = new double[k][3];
        for (int i = 0; i < k; i++) {
            int rgb = sample[rnd.nextInt(sample.length)];
            cents[i][0] = (rgb >> 16) & 0xFF;
            cents[i][1] = (rgb >>  8) & 0xFF;
            cents[i][2] =  rgb        & 0xFF;
        }

        int[] assign = new int[sample.length];
        int maxIter = 16;
        for (int iter = 0; iter < maxIter; iter++) {
            // assign
            boolean changed = false;
            for (int i = 0; i < sample.length; i++) {
                int rgb = sample[i];
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >>  8) & 0xFF;
                int b =  rgb        & 0xFF;
                int best = 0;
                double bestD = Double.MAX_VALUE;
                for (int c = 0; c < k; c++) {
                    double dr = r - cents[c][0];
                    double dg = g - cents[c][1];
                    double db = b - cents[c][2];
                    double d = dr*dr + dg*dg + db*db;
                    if (d < bestD) { bestD = d; best = c; }
                }
                if (assign[i] != best) { assign[i] = best; changed = true; }
            }
            // update
            double[][] sum = new double[k][3];
            int[] cnt = new int[k];
            for (int i = 0; i < sample.length; i++) {
                int rgb = sample[i];
                int c = assign[i];
                sum[c][0] += (rgb >> 16) & 0xFF;
                sum[c][1] += (rgb >>  8) & 0xFF;
                sum[c][2] +=  rgb        & 0xFF;
                cnt[c]++;
            }
            for (int c = 0; c < k; c++) {
                if (cnt[c] == 0) {
                    // re-seed empty cluster from a random sample
                    int rgb = sample[rnd.nextInt(sample.length)];
                    cents[c][0] = (rgb >> 16) & 0xFF;
                    cents[c][1] = (rgb >>  8) & 0xFF;
                    cents[c][2] =  rgb        & 0xFF;
                } else {
                    cents[c][0] = sum[c][0] / cnt[c];
                    cents[c][1] = sum[c][1] / cnt[c];
                    cents[c][2] = sum[c][2] / cnt[c];
                }
            }
            if (!changed) break;
        }

        // count usage on the full image (so the palette is ordered properly)
        int[] counts = new int[k];
        for (int rgb : pixels) {
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >>  8) & 0xFF;
            int b =  rgb        & 0xFF;
            int best = 0;
            double bestD = Double.MAX_VALUE;
            for (int c = 0; c < k; c++) {
                double dr = r - cents[c][0];
                double dg = g - cents[c][1];
                double db = b - cents[c][2];
                double d = dr*dr + dg*dg + db*db;
                if (d < bestD) { bestD = d; best = c; }
            }
            counts[best]++;
        }
        return makePalette("k-means", cents, counts);
    }

    /* =================================================================
       Median-cut
       ================================================================= */

    private static ColorPalette medianCut(BufferedImage img, int k) {
        int[] pixels = ImageOps.getPixels(img);
        int[] sample = subSample(pixels, 100_000);
        // Box: list of integer RGBs.
        List<int[]> boxes = new ArrayList<>();
        boxes.add(sample);

        while (boxes.size() < k) {
            // pick the box with the largest channel range
            int splitIdx = -1;
            int bestRange = -1;
            int bestChan = 0;
            for (int i = 0; i < boxes.size(); i++) {
                int[] arr = boxes.get(i);
                if (arr.length < 2) continue;
                int rMin=255, gMin=255, bMin=255, rMax=0, gMax=0, bMax=0;
                for (int rgb : arr) {
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >>  8) & 0xFF;
                    int b =  rgb        & 0xFF;
                    if (r < rMin) rMin = r; if (r > rMax) rMax = r;
                    if (g < gMin) gMin = g; if (g > gMax) gMax = g;
                    if (b < bMin) bMin = b; if (b > bMax) bMax = b;
                }
                int dr = rMax - rMin, dg = gMax - gMin, db = bMax - bMin;
                int range = Math.max(dr, Math.max(dg, db));
                int chan = (range == dr) ? 0 : (range == dg) ? 1 : 2;
                if (range > bestRange) {
                    bestRange = range; splitIdx = i; bestChan = chan;
                }
            }
            if (splitIdx < 0) break;
            int[] arr = boxes.remove(splitIdx);
            final int chan = bestChan;
            Integer[] boxed = new Integer[arr.length];
            for (int i = 0; i < arr.length; i++) boxed[i] = arr[i];
            java.util.Arrays.sort(boxed, Comparator.comparingInt(rgb -> (rgb >> ((2 - chan) * 8)) & 0xFF));
            int mid = boxed.length / 2;
            int[] left  = new int[mid];
            int[] right = new int[boxed.length - mid];
            for (int i = 0; i < mid;             i++) left[i]  = boxed[i];
            for (int i = mid; i < boxed.length;  i++) right[i - mid] = boxed[i];
            boxes.add(left);
            boxes.add(right);
        }
        // average each box -> palette colour
        double[][] cents = new double[boxes.size()][3];
        int[] counts = new int[boxes.size()];
        for (int i = 0; i < boxes.size(); i++) {
            int[] arr = boxes.get(i);
            long rs = 0, gs = 0, bs = 0;
            for (int rgb : arr) {
                rs += (rgb >> 16) & 0xFF;
                gs += (rgb >>  8) & 0xFF;
                bs +=  rgb        & 0xFF;
            }
            int n = Math.max(1, arr.length);
            cents[i][0] = rs / (double) n;
            cents[i][1] = gs / (double) n;
            cents[i][2] = bs / (double) n;
            counts[i]   = n;
        }
        return makePalette("median-cut", cents, counts);
    }

    /* =================================================================
       Helpers
       ================================================================= */

    private static int[] subSample(int[] pixels, int target) {
        if (pixels.length <= target) return pixels;
        int step = pixels.length / target;
        int n = pixels.length / step;
        int[] out = new int[n];
        for (int i = 0; i < n; i++) out[i] = pixels[i * step];
        return out;
    }

    private static ColorPalette makePalette(String tag, double[][] cents, int[] counts) {
        // sort by usage descending
        Integer[] order = new Integer[cents.length];
        for (int i = 0; i < order.length; i++) order[i] = i;
        java.util.Arrays.sort(order, (a, b) -> Integer.compare(counts[b], counts[a]));

        List<ColorPalette.Entry> entries = new ArrayList<>();
        for (int i = 0; i < order.length; i++) {
            int idx = order[i];
            int r = ImageOps.clamp8((int) Math.round(cents[idx][0]));
            int g = ImageOps.clamp8((int) Math.round(cents[idx][1]));
            int b = ImageOps.clamp8((int) Math.round(cents[idx][2]));
            String code = String.format("C%02d", i + 1);
            String name = String.format("RGB %d/%d/%d", r, g, b);
            entries.add(new ColorPalette.Entry(code, name, r, g, b));
        }
        return new ColorPalette("Auto (" + tag + ")", entries);
    }
}
