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

/* Hier liegt die eigentliche Logik des Use Case. Die Klasse haelt den
 * Projektzustand (Bild, Parameter, Vorlage), die letzte Vorschau und die
 * Undo-Historie. Jede Operation gibt den neuen Zustand zurueck; gesetzt wird
 * er von der Fassade. */
public final class CreatorImpl {

    private static final double PREVIEW_DPI = 96.0;   // Aufloesung der Vorschau

    private final Project project = new Project();
    private final UndoRedoManager history = new UndoRedoManager();
    private BufferedImage preview;

    // true = der naechste Parameter-Edit startet einen neuen Undo-Schritt
    private boolean snapshotPending = true;

    // --- Use-Case-Operationen ---

    // Bild einlesen und als neue Quelle setzen
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
            snapshotPending = true;          // neuer Bearbeitungsabschnitt
            return CreatorState.IMAGE_LOADED;
        } catch (IOException ex) {
            throw new UncheckedIOException("Bild konnte nicht geladen werden", ex);
        }
    }

    // uebergebene Parameter ins Projekt uebernehmen
    public void updateParameters(Parameters from) {
        if (snapshotPending) {
            history.snapshot(project);
            snapshotPending = false;
        }
        copyParameters(from, project.getParameters());
    }

    // Vorlage erzeugen und Vorschau rendern
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

    // --- Undo / Redo ---

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

    // --- Projekt speichern / oeffnen ---

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

    // --- Getter ---

    public BufferedImage getOriginal() { return project.getOriginal(); }
    public BufferedImage getPreview()  { return preview; }
    public Template getTemplate()      { return project.getTemplate(); }
    public Parameters getParameters()  { return project.getParameters(); }
    public File getProjectFile()       { return project.getProjectFile(); }
    public File getSourceFile()        { return project.getSourceFile(); }

    // --- Hilfsmethoden ---

    // nach Undo/Redo/Oeffnen wieder neu generieren
    private State rebuild() {
        if (project.getOriginal() == null) {
            project.setTemplate(null);
            this.preview = null;
            return CreatorState.NO_IMAGE;
        }
        return createTemplate();
    }

    // Zustand aus dem Projektinhalt ableiten (falls nichts zu tun war)
    private State currentState() {
        if (project.getTemplate() != null) return CreatorState.TEMPLATE_READY;
        if (project.getOriginal() != null) return CreatorState.IMAGE_LOADED;
        return CreatorState.NO_IMAGE;
    }

    // Parameter Feld fuer Feld kopieren (war vorher in der UI)
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
