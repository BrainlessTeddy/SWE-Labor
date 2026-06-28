package com.artcreator.creator.impl;

import com.artcreator.history.UndoRedoManager;
import com.artcreator.io.ProjectIO;
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
 * <p>Hält den Projektzustand (Originalbild, Parameter, erzeugte Vorlage), die
 * zuletzt gerenderte Vorschau sowie die Undo/Redo-Historie. Jede Operation
 * liefert den Folgezustand zurück; das Schalten der Zustandsmaschine übernimmt
 * die Fassade.</p>
 */
public final class CreatorImpl {

    /** dpi der Vorschau-Rasterung (entspricht der bisherigen Logik). */
    private static final double PREVIEW_DPI = 96.0;

    private final Project project = new Project();
    private final UndoRedoManager history = new UndoRedoManager();
    private BufferedImage preview;

    /** true, wenn die nächste Parameteränderung einen neuen Undo-Schritt beginnt. */
    private boolean snapshotPending = true;

    /* ===================== Use-Case-Operationen ===================== */

    /** Liest das Bild ein und setzt es als neue Quelle. */
    public State loadImage(File file) {
        Objects.requireNonNull(file, "file must not be null");
        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null) {
                throw new IOException("Bildformat nicht erkannt: " + file.getName());
            }
            history.snapshot(project);
            project.setSourceFile(file);
            project.setOriginal(img);
            snapshotPending = true;          // neuer Bearbeitungs-Batch
            return CreatorState.IMAGE_LOADED;
        } catch (IOException ex) {
            throw new UncheckedIOException("Bild konnte nicht geladen werden", ex);
        }
    }

    /** Übernimmt die übergebenen Parameter in den internen Projektzustand. */
    public void updateParameters(Parameters from) {
        if (snapshotPending) {
            history.snapshot(project);
            snapshotPending = false;
        }
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

    /* ===================== Undo / Redo ===================== */

    public State undo() {
        if (!history.undo(project)) return currentState();
        snapshotPending = true;
        return rebuild();
    }

    public State redo() {
        if (!history.redo(project)) return currentState();
        snapshotPending = true;
        return rebuild();
    }

    public boolean canUndo() { return history.canUndo(); }
    public boolean canRedo() { return history.canRedo(); }

    /* ===================== Projekt speichern / öffnen ===================== */

    public void saveProject(File file) {
        try {
            ProjectIO.save(project, file);
            project.setProjectFile(file);
            project.markClean();
        } catch (IOException ex) {
            throw new UncheckedIOException("Speichern fehlgeschlagen", ex);
        }
    }

    public State openProject(File file) {
        try {
            Project loaded = ProjectIO.load(file);
            project.setOriginal(loaded.getOriginal());
            project.setSourceFile(loaded.getSourceFile());
            project.setProjectFile(loaded.getProjectFile());
            copyParameters(loaded.getParameters(), project.getParameters());
            history.clear();
            snapshotPending = true;
            return rebuild();
        } catch (IOException ex) {
            throw new UncheckedIOException("Laden fehlgeschlagen", ex);
        }
    }

    /* ===================== reads ===================== */

    public BufferedImage getOriginal() { return project.getOriginal(); }
    public BufferedImage getPreview()  { return preview; }
    public Template getTemplate()      { return project.getTemplate(); }
    public Parameters getParameters()  { return project.getParameters(); }
    public File getProjectFile()       { return project.getProjectFile(); }
    public File getSourceFile()        { return project.getSourceFile(); }

    /* ===================== helper ===================== */

    /** Regeneriert nach einer Zustands-Wiederherstellung (Undo/Redo/Open). */
    private State rebuild() {
        if (project.getOriginal() == null) {
            project.setTemplate(null);
            this.preview = null;
            return CreatorState.NO_IMAGE;
        }
        return createTemplate();
    }

    /** Aktueller Zustand abgeleitet aus dem Projektinhalt (für No-op-Fälle). */
    private State currentState() {
        if (project.getTemplate() != null) return CreatorState.TEMPLATE_READY;
        if (project.getOriginal() != null) return CreatorState.IMAGE_LOADED;
        return CreatorState.NO_IMAGE;
    }

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
