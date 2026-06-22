package com.artcreator.model;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * The result of running the template generator: a regular grid of "stick"
 * cells, each holding an index into a {@link ColorPalette}.
 *
 * <p>The grid coordinate system is row-major, origin at the top-left, and
 * is independent of the printed page size: the millimetre dimensions of the
 * board are stored alongside the grid so that downstream code can place the
 * sticks at the right physical location.</p>
 *
 * <p>Hexagonal layouts encode the row offset implicitly: even rows are at
 * column*pitch, odd rows are at column*pitch + pitch/2.</p>
 */
public final class Template {

    private final int cols;
    private final int rows;
    private final int[] paletteIndices;          // length = cols*rows; -1 = empty cell
    private final ColorPalette palette;
    private final double pitchMm;                // centre-to-centre spacing
    private final double diameterMm;
    private final StickShape shape;
    private final GridLayout layout;
    private final double boardWidthMm;
    private final double boardHeightMm;

    /** Snapshot of the source image (post-quantization), useful for the comparison view. */
    private final BufferedImage previewImage;

    public Template(int cols, int rows,
                    int[] paletteIndices,
                    ColorPalette palette,
                    double pitchMm,
                    double diameterMm,
                    StickShape shape,
                    GridLayout layout,
                    double boardWidthMm,
                    double boardHeightMm,
                    BufferedImage previewImage) {

        if (paletteIndices.length != cols * rows) {
            throw new IllegalArgumentException(
                "paletteIndices length " + paletteIndices.length
                + " does not match grid size " + (cols*rows));
        }
        this.cols = cols;
        this.rows = rows;
        this.paletteIndices = paletteIndices;
        this.palette = Objects.requireNonNull(palette);
        this.pitchMm = pitchMm;
        this.diameterMm = diameterMm;
        this.shape = shape;
        this.layout = layout;
        this.boardWidthMm = boardWidthMm;
        this.boardHeightMm = boardHeightMm;
        this.previewImage = previewImage;
    }

    public int getCols()                  { return cols; }
    public int getRows()                  { return rows; }
    public ColorPalette getPalette()      { return palette; }
    public double getPitchMm()            { return pitchMm; }
    public double getDiameterMm()         { return diameterMm; }
    public StickShape getShape()          { return shape; }
    public GridLayout getLayout()         { return layout; }
    public double getBoardWidthMm()       { return boardWidthMm; }
    public double getBoardHeightMm()      { return boardHeightMm; }
    public BufferedImage getPreviewImage(){ return previewImage; }

    /** Palette index at (col, row). -1 means an empty cell (not placed). */
    public int indexAt(int col, int row) {
        return paletteIndices[row * cols + col];
    }

    /** Total number of placed sticks (excluding empty cells, if any). */
    public int totalSticks() {
        int n = 0;
        for (int idx : paletteIndices) if (idx >= 0) n++;
        return n;
    }

    /** Counts of sticks per palette index. Zero-length array if no palette. */
    public int[] countsByIndex() {
        int[] counts = new int[palette.size()];
        for (int idx : paletteIndices) {
            if (idx >= 0 && idx < counts.length) counts[idx]++;
        }
        return counts;
    }

    /** Centre of cell (col, row) in millimetres relative to the board origin (top-left). */
    public double centerXmm(int col, int row) {
        double offset = (layout == GridLayout.HEXAGONAL && (row & 1) == 1)
                ? pitchMm * 0.5
                : 0.0;
        return diameterMm * 0.5 + offset + col * pitchMm;
    }

    public double centerYmm(int col, int row) {
        // For hex layout the vertical pitch is reduced (sqrt(3)/2 * pitch). Keeping
        // it equal to the horizontal pitch keeps the math simple and is still close
        // enough for normal board sizes; users wanting denser packing can shrink
        // the pitch parameter directly.
        double yPitch = (layout == GridLayout.HEXAGONAL) ? pitchMm * 0.866 : pitchMm;
        return diameterMm * 0.5 + row * yPitch;
    }
}
