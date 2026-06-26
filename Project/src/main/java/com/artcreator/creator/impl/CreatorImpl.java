package com.artcreator.creator.impl;

import com.artcreator.model.Parameters;
import com.artcreator.model.Project;
import com.artcreator.model.Template;
import com.artcreator.statemachine.port.CreatorState;
import com.artcreator.statemachine.port.State;
import com.artcreator.template.TemplateGenerator;
import com.artcreator.template.TemplateRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

/**
 * Implementierung der Systemoperationen des Use Case „Vorlage erstellen".
 *
 * <p>Hält den Projektzustand (Originalbild, Parameter, erzeugte Vorlage) und
 * die zuletzt gerenderte Vorschau. Jede Operation liefert den Folgezustand
 * zurück; das Schalten der Zustandsmaschine übernimmt die Fassade.</p>
 */
public final class CreatorImpl {

    /** dpi der Vorschau-Rasterung (entspricht der bisherigen Logik). */
    private static final double PREVIEW_DPI = 96.0;

    private final Project project = new Project();
    private BufferedImage preview;

    /** Liest das Bild ein und setzt es als neue Quelle. */
    public State loadImage(File file) {
        Objects.requireNonNull(file, "file must not be null");
        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null) {
                throw new IOException("Bildformat nicht erkannt: " + file.getName());
            }
            project.setSourceFile(file);
            project.setOriginal(img);
            return CreatorState.IMAGE_LOADED;
        } catch (IOException ex) {
            throw new UncheckedIOException("Bild konnte nicht geladen werden", ex);
        }
    }

    /** Übernimmt die übergebenen Parameter in den internen Projektzustand. */
    public void updateParameters(Parameters from) {
        copyParameters(from, project.getParameters());
    }

    /** Generiert die Vorlage und rendert die Vorschau. */
    public State createTemplate() {
        BufferedImage original = project.getOriginal();
        if (original == null) {
            return CreatorState.NO_IMAGE;
        }
        Parameters params = project.getParameters();
        TemplateGenerator.GenerationResult result = TemplateGenerator.generate(original, params);

        project.setProcessed(result.processedImage);
        project.setTemplate(result.template);
        this.preview = TemplateRenderer.renderPreview(
                result.template,
                PREVIEW_DPI,
                params.isShowColorCodes(),
                params.isShowGrid());

        return CreatorState.TEMPLATE_READY;
    }

    /* -------- reads -------- */

    public BufferedImage getOriginal() { return project.getOriginal(); }
    public BufferedImage getPreview()  { return preview; }
    public Template getTemplate()      { return project.getTemplate(); }
    public Parameters getParameters()  { return project.getParameters(); }

    /** Quelldatei für abgeleitete Aktionen (Export-Dateinamen o.ä.). */
    public File getSourceFile()        { return project.getSourceFile(); }

    /* -------- helper -------- */

    /** Feldweises Übernehmen der Parameter (zuvor in der UI). */
    private static void copyParameters(Parameters from, Parameters to) {
        to.setPaperSize(from.getPaperSize());
        to.setOrientation(from.getOrientation());
        to.setCustomWidthMm(from.getCustomWidthMm());
        to.setCustomHeightMm(from.getCustomHeightMm());
        to.setPrinterMarginMm(from.getPrinterMarginMm());
        to.setTileOverlapMm(from.getTileOverlapMm());
        to.setStickDiameterMm(from.getStickDiameterMm());
        to.setStickSpacingMm(from.getStickSpacingMm());
        to.setStickShape(from.getStickShape());
        to.setGridLayout(from.getGridLayout());
        to.setQuantizationMethod(from.getQuantizationMethod());
        to.setPaletteSize(from.getPaletteSize());
        to.setLockToPalette(from.isLockToPalette());
        to.setLockedPalette(from.getLockedPalette());
        to.setDithering(from.isDithering());
        to.setDitherStrength(from.getDitherStrength());
        to.setBrightness(from.getBrightness());
        to.setContrast(from.getContrast());
        to.setGamma(from.getGamma());
        to.setSaturation(from.getSaturation());
        to.setHistogramEqualization(from.isHistogramEqualization());
        to.setEdgeBoost(from.getEdgeBoost());
        to.setShowColorCodes(from.isShowColorCodes());
        to.setShowGrid(from.isShowGrid());
        to.setShowCropMarks(from.isShowCropMarks());
    }
}
