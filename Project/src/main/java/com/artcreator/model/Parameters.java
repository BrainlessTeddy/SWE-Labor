package com.artcreator.model;

/**
 * Mutable parameter bag controlling the template generator and its preview.
 *
 * <p>Defaults are tuned to give a sensible starting result on a typical
 * portrait photograph. The {@link #copy()} method is used by the undo/redo
 * machinery to snapshot state.</p>
 */
public final class Parameters {

    /* ---------------- output / paper ---------------- */
    private PaperSize paperSize = PaperSize.A3;
    private PaperSize.Orientation orientation = PaperSize.Orientation.PORTRAIT;
    private int customWidthMm = 297;       // only used when paperSize == CUSTOM
    private int customHeightMm = 420;
    private int printerMarginMm = 5;       // non-printable margin on standard printers
    private int tileOverlapMm = 5;         // overlap between adjacent A4 tiles

    /* ---------------- stick geometry ---------------- */
    private double stickDiameterMm = 4.0;
    private double stickSpacingMm  = 5.0;  // centre-to-centre pitch
    private StickShape stickShape  = StickShape.CIRCLE;
    private GridLayout gridLayout  = GridLayout.SQUARE;

    /* ---------------- colour pipeline ---------------- */
    private QuantizationMethod quantizationMethod = QuantizationMethod.K_MEANS;
    private int paletteSize = 16;
    private boolean lockToPalette = false;
    private ColorPalette lockedPalette = ColorPalette.DEFAULT_24;
    private boolean dithering = true;
    private double ditherStrength = 1.0;   // [0..1], 1.0 = full Floyd-Steinberg

    /* ---------------- preprocessing ---------------- */
    private double brightness = 0.0;       // [-1..1]
    private double contrast   = 0.0;       // [-1..1]
    private double gamma      = 1.0;       // (0, ~3]
    private double saturation = 1.0;       // [0..2]
    private boolean histogramEqualization = false;
    private double edgeBoost = 0.0;        // [0..1] mix-in of a Sobel edge map

    /* ---------------- template overlay ---------------- */
    private boolean showColorCodes = true;
    private boolean showGrid       = true;
    private boolean showCropMarks  = true;

    /* ====================================================================
       Getters / setters
       ==================================================================== */

    public PaperSize getPaperSize()                  { return paperSize; }
    public void setPaperSize(PaperSize v)            { this.paperSize = v; }
    public PaperSize.Orientation getOrientation()    { return orientation; }
    public void setOrientation(PaperSize.Orientation v) { this.orientation = v; }
    public int getCustomWidthMm()                    { return customWidthMm; }
    public void setCustomWidthMm(int v)              { this.customWidthMm = v; }
    public int getCustomHeightMm()                   { return customHeightMm; }
    public void setCustomHeightMm(int v)             { this.customHeightMm = v; }
    public int getPrinterMarginMm()                  { return printerMarginMm; }
    public void setPrinterMarginMm(int v)            { this.printerMarginMm = v; }
    public int getTileOverlapMm()                    { return tileOverlapMm; }
    public void setTileOverlapMm(int v)              { this.tileOverlapMm = v; }

    public double getStickDiameterMm()               { return stickDiameterMm; }
    public void setStickDiameterMm(double v)         { this.stickDiameterMm = v; }
    public double getStickSpacingMm()                { return stickSpacingMm; }
    public void setStickSpacingMm(double v)          { this.stickSpacingMm = v; }
    public StickShape getStickShape()                { return stickShape; }
    public void setStickShape(StickShape v)          { this.stickShape = v; }
    public GridLayout getGridLayout()                { return gridLayout; }
    public void setGridLayout(GridLayout v)          { this.gridLayout = v; }

    public QuantizationMethod getQuantizationMethod(){ return quantizationMethod; }
    public void setQuantizationMethod(QuantizationMethod v) { this.quantizationMethod = v; }
    public int getPaletteSize()                      { return paletteSize; }
    public void setPaletteSize(int v)                { this.paletteSize = v; }
    public boolean isLockToPalette()                 { return lockToPalette; }
    public void setLockToPalette(boolean v)          { this.lockToPalette = v; }
    public ColorPalette getLockedPalette()           { return lockedPalette; }
    public void setLockedPalette(ColorPalette v)     { this.lockedPalette = v; }
    public boolean isDithering()                     { return dithering; }
    public void setDithering(boolean v)              { this.dithering = v; }
    public double getDitherStrength()                { return ditherStrength; }
    public void setDitherStrength(double v)          { this.ditherStrength = v; }

    public double getBrightness()                    { return brightness; }
    public void setBrightness(double v)              { this.brightness = v; }
    public double getContrast()                      { return contrast; }
    public void setContrast(double v)                { this.contrast = v; }
    public double getGamma()                         { return gamma; }
    public void setGamma(double v)                   { this.gamma = v; }
    public double getSaturation()                    { return saturation; }
    public void setSaturation(double v)              { this.saturation = v; }
    public boolean isHistogramEqualization()         { return histogramEqualization; }
    public void setHistogramEqualization(boolean v)  { this.histogramEqualization = v; }
    public double getEdgeBoost()                     { return edgeBoost; }
    public void setEdgeBoost(double v)               { this.edgeBoost = v; }

    public boolean isShowColorCodes()                { return showColorCodes; }
    public void setShowColorCodes(boolean v)         { this.showColorCodes = v; }
    public boolean isShowGrid()                      { return showGrid; }
    public void setShowGrid(boolean v)               { this.showGrid = v; }
    public boolean isShowCropMarks()                 { return showCropMarks; }
    public void setShowCropMarks(boolean v)          { this.showCropMarks = v; }

    /* ---------------- helpers ---------------- */

    public int getPageWidthMm() {
        if (paperSize == PaperSize.CUSTOM) {
            return orientation == PaperSize.Orientation.LANDSCAPE
                    ? Math.max(customWidthMm, customHeightMm)
                    : Math.min(customWidthMm, customHeightMm);
        }
        return paperSize.widthMm(orientation);
    }

    public int getPageHeightMm() {
        if (paperSize == PaperSize.CUSTOM) {
            return orientation == PaperSize.Orientation.LANDSCAPE
                    ? Math.min(customWidthMm, customHeightMm)
                    : Math.max(customWidthMm, customHeightMm);
        }
        return paperSize.heightMm(orientation);
    }

    /** Deep copy. Used by the undo manager. */
    public Parameters copy() {
        Parameters p = new Parameters();
        p.paperSize          = this.paperSize;
        p.orientation        = this.orientation;
        p.customWidthMm      = this.customWidthMm;
        p.customHeightMm     = this.customHeightMm;
        p.printerMarginMm    = this.printerMarginMm;
        p.tileOverlapMm      = this.tileOverlapMm;
        p.stickDiameterMm    = this.stickDiameterMm;
        p.stickSpacingMm     = this.stickSpacingMm;
        p.stickShape         = this.stickShape;
        p.gridLayout         = this.gridLayout;
        p.quantizationMethod = this.quantizationMethod;
        p.paletteSize        = this.paletteSize;
        p.lockToPalette      = this.lockToPalette;
        p.lockedPalette      = this.lockedPalette;
        p.dithering          = this.dithering;
        p.ditherStrength     = this.ditherStrength;
        p.brightness         = this.brightness;
        p.contrast           = this.contrast;
        p.gamma              = this.gamma;
        p.saturation         = this.saturation;
        p.histogramEqualization = this.histogramEqualization;
        p.edgeBoost          = this.edgeBoost;
        p.showColorCodes     = this.showColorCodes;
        p.showGrid           = this.showGrid;
        p.showCropMarks      = this.showCropMarks;
        return p;
    }
}
