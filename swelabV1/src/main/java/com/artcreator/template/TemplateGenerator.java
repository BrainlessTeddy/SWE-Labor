package com.artcreator.template;

import com.artcreator.model.ColorPalette;
import com.artcreator.model.GridLayout;
import com.artcreator.model.Parameters;
import com.artcreator.model.QuantizationMethod;
import com.artcreator.model.Template;
import com.artcreator.processing.ColorQuantizer;
import com.artcreator.processing.Ditherer;
import com.artcreator.processing.ImagePreprocessor;
import com.artcreator.util.ImageOps;

import java.awt.image.BufferedImage;

/**
 * Top-level pipeline: image -> preprocessed image -> quantized image -> stick grid.
 *
 * <p>The generator is deterministic for a given image and {@link Parameters}
 * instance. It does not mutate either input.</p>
 */
public final class TemplateGenerator {

    private TemplateGenerator() { /* static */ }

    /**
     * @param source    untouched original image
     * @param params    current settings
     * @return          template ready for rendering / export
     */
    public static GenerationResult generate(BufferedImage source, Parameters params) {
        if (source == null) throw new IllegalArgumentException("source image is null");

        // 1) preprocessing
        BufferedImage processed = ImagePreprocessor.apply(source, params);

        // 2) compute target board size in millimetres minus printable margin.
        int boardWmm = params.getPageWidthMm()  - 2 * params.getPrinterMarginMm();
        int boardHmm = params.getPageHeightMm() - 2 * params.getPrinterMarginMm();
        if (boardWmm <= 0 || boardHmm <= 0) {
            throw new IllegalStateException("Page is smaller than printer margin");
        }

        // 3) compute number of cols/rows from spacing, fitting the board
        double pitch = Math.max(0.5, params.getStickSpacingMm());
        int cols = Math.max(1, (int) Math.floor(boardWmm / pitch));
        int rows;
        if (params.getGridLayout() == GridLayout.HEXAGONAL) {
            double yPitch = pitch * 0.866;
            rows = Math.max(1, (int) Math.floor(boardHmm / yPitch));
        } else {
            rows = Math.max(1, (int) Math.floor(boardHmm / pitch));
        }

        // 4) preserve image aspect ratio: if the source aspect doesn't match the
        //    grid aspect we shrink one dimension so the picture isn't stretched.
        double srcAspect = (double) processed.getWidth() / processed.getHeight();
        double gridAspect = (double) cols / rows;
        if (srcAspect > gridAspect) {
            // image is wider than the grid -> shrink rows
            rows = Math.max(1, (int) Math.round(cols / srcAspect));
        } else {
            // taller -> shrink cols
            cols = Math.max(1, (int) Math.round(rows * srcAspect));
        }

        // 5) Resize the source down to the grid size. This averages each
        //    cell's pixels into a single representative colour automatically.
        BufferedImage shrunk = ImageOps.resize(processed, cols, rows);

        // 6) Build / pick the palette.
        ColorPalette palette;
        if (params.isLockToPalette() || params.getQuantizationMethod() == QuantizationMethod.PALETTE_SNAP) {
            palette = params.getLockedPalette();
        } else {
            palette = ColorQuantizer.buildPalette(shrunk,
                                                  params.getPaletteSize(),
                                                  params.getQuantizationMethod(),
                                                  params.getLockedPalette());
        }

        // 7) Dither / snap the shrunk image to the palette.
        BufferedImage quantized = params.isDithering()
                ? Ditherer.floydSteinberg(shrunk, palette, params.getDitherStrength())
                : Ditherer.snap(shrunk, palette);

        // 8) Build the index grid by reading each cell's quantized colour.
        int[] indices = new int[cols * rows];
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                int rgb = quantized.getRGB(x, y) & 0x00FFFFFF;
                indices[y * cols + x] = palette.nearestIndex(rgb);
            }
        }

        Template tmpl = new Template(
                cols, rows,
                indices,
                palette,
                pitch,
                Math.min(params.getStickDiameterMm(), pitch * 0.95),
                params.getStickShape(),
                params.getGridLayout(),
                boardWmm, boardHmm,
                quantized);

        return new GenerationResult(processed, quantized, tmpl);
    }

    /** Bundles the intermediate images alongside the final template. */
    public static final class GenerationResult {
        public final BufferedImage processedImage;
        public final BufferedImage quantizedImage;
        public final Template template;

        public GenerationResult(BufferedImage processedImage,
                                BufferedImage quantizedImage,
                                Template template) {
            this.processedImage = processedImage;
            this.quantizedImage = quantizedImage;
            this.template = template;
        }
    }
}
