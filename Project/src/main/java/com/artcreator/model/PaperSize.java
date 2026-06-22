package com.artcreator.model;

/**
 * Standard ISO 216 paper sizes, in millimetres.
 *
 * <p>The enum is immutable; orientation is carried by {@link Parameters},
 * because the same paper size may be used in either orientation.</p>
 */
public enum PaperSize {

    A0    (841,  1189),
    A1    (594,   841),
    A2    (420,   594),
    A3    (297,   420),
    A4    (210,   297),
    A5    (148,   210),
    LETTER(216,   279),
    LEGAL (216,   356),
    /** Custom sizes are stored on {@link Parameters} directly. */
    CUSTOM(0,     0);

    private final int shortEdgeMm;
    private final int longEdgeMm;

    PaperSize(int shortEdgeMm, int longEdgeMm) {
        this.shortEdgeMm = shortEdgeMm;
        this.longEdgeMm  = longEdgeMm;
    }

    public int shortEdgeMm() { return shortEdgeMm; }
    public int longEdgeMm()  { return longEdgeMm;  }

    public int widthMm(Orientation o) {
        return o == Orientation.LANDSCAPE ? longEdgeMm : shortEdgeMm;
    }

    public int heightMm(Orientation o) {
        return o == Orientation.LANDSCAPE ? shortEdgeMm : longEdgeMm;
    }

    /** Whether this format fits onto a single A4 sheet without tiling. */
    public boolean fitsOnA4() {
        return shortEdgeMm <= A4.shortEdgeMm && longEdgeMm <= A4.longEdgeMm;
    }

    public enum Orientation { PORTRAIT, LANDSCAPE }
}
