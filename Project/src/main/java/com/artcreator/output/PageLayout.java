package com.artcreator.output;

import com.artcreator.model.Parameters;
import com.artcreator.model.Template;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits the full board into a grid of A4 (or full-page) tiles, taking the
 * non-printable margin and tile overlap into account.
 *
 * If the chosen output paper size already fits on a single A4, no tiling
 * occurs and the layout contains exactly one tile.
 */
public final class PageLayout {

    public static final int TILE_WIDTH_MM  = 210;   // A4 portrait
    public static final int TILE_HEIGHT_MM = 297;

    private final List<Tile> tiles;
    private final int rows;
    private final int cols;
    private final boolean tiled;
    private final int tilePaperWidthMm;
    private final int tilePaperHeightMm;

    private PageLayout(List<Tile> tiles, int rows, int cols, boolean tiled,
                       int tileWmm, int tileHmm) {
        this.tiles = tiles;
        this.rows = rows;
        this.cols = cols;
        this.tiled = tiled;
        this.tilePaperWidthMm  = tileWmm;
        this.tilePaperHeightMm = tileHmm;
    }

    public List<Tile> tiles()         { return tiles; }
    public int getRows()              { return rows; }
    public int getCols()              { return cols; }
    public boolean isTiled()          { return tiled; }
    public int tilePaperWidthMm()     { return tilePaperWidthMm; }
    public int tilePaperHeightMm()    { return tilePaperHeightMm; }

    public static PageLayout compute(Template tmpl, Parameters params) {
        int boardW = (int) Math.ceil(tmpl.getBoardWidthMm());
        int boardH = (int) Math.ceil(tmpl.getBoardHeightMm());
        int margin   = params.getPrinterMarginMm();
        int overlap  = params.getTileOverlapMm();

        int tileW, tileH;
        if (params.getPaperSize().fitsOnA4()
                && params.getPaperSize() != com.artcreator.model.PaperSize.CUSTOM) {
            tileW = params.getPageWidthMm();
            tileH = params.getPageHeightMm();
        } else {
            tileW = TILE_WIDTH_MM;
            tileH = TILE_HEIGHT_MM;
        }

        int printableW = tileW - 2 * margin;
        int printableH = tileH - 2 * margin;

        int cols, rows;
        if (boardW <= printableW && boardH <= printableH) {
            // Whole board fits on a single tile.
            cols = 1;
            rows = 1;
        } else {
            int stepW = Math.max(1, printableW - overlap);
            int stepH = Math.max(1, printableH - overlap);
            cols = (int) Math.ceil(boardW / (double) stepW);
            rows = (int) Math.ceil(boardH / (double) stepH);
        }

        int stepW = (cols == 1) ? printableW : Math.max(1, printableW - overlap);
        int stepH = (rows == 1) ? printableH : Math.max(1, printableH - overlap);

        List<Tile> tiles = new ArrayList<>();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int xOnBoard = col * stepW;
                int yOnBoard = row * stepH;
                int wOnBoard = Math.min(printableW, boardW - xOnBoard);
                int hOnBoard = Math.min(printableH, boardH - yOnBoard);
                tiles.add(new Tile(row, col, rows, cols,
                                   xOnBoard, yOnBoard, wOnBoard, hOnBoard,
                                   tileW, tileH, margin));
            }
        }
        boolean tiled = !(rows == 1 && cols == 1);
        return new PageLayout(tiles, rows, cols, tiled, tileW, tileH);
    }

    /** A single A4 tile with its position on the full board. */
    public static final class Tile {
        public final int row, col;
        public final int totalRows, totalCols;
        public final int boardXmm, boardYmm, boardWmm, boardHmm;
        public final int paperWmm, paperHmm;
        public final int marginMm;

        public Tile(int row, int col, int totalRows, int totalCols,
                    int boardXmm, int boardYmm, int boardWmm, int boardHmm,
                    int paperWmm, int paperHmm, int marginMm) {
            this.row = row; this.col = col;
            this.totalRows = totalRows; this.totalCols = totalCols;
            this.boardXmm = boardXmm; this.boardYmm = boardYmm;
            this.boardWmm = boardWmm; this.boardHmm = boardHmm;
            this.paperWmm = paperWmm; this.paperHmm = paperHmm;
            this.marginMm = marginMm;
        }

        public String label() {
            return String.format("R%dC%d (%d/%d)", row + 1, col + 1, totalRows, totalCols);
        }
    }
}
