package com.artcreator.ui;

import com.artcreator.creator.port.Creator;
import com.artcreator.model.Template;
import com.artcreator.statemachine.port.CreatorState;
import com.artcreator.statemachine.port.Observer;
import com.artcreator.statemachine.port.State;
import com.artcreator.statemachine.port.Subject;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Controller des Use Case „Vorlage erstellen" (MVC).
 *
 * <p>Liest Eingaben aus der {@link MainFrame}-View und ruft die
 * Systemoperationen der {@link Creator}-Fassade ausschließlich
 * <b>asynchron</b> auf (Operationen sind {@code void}). Als {@link Observer}
 * ist der Controller bei der Zustandsmaschine registriert und aktualisiert die
 * View nach jeder Zustandsänderung, indem er die Ergebnisse aus der Fassade
 * <b>abholt</b>.</p>
 */
public final class CreatorController implements ActionListener, Observer {

    /** Action-Commands der von diesem Controller bedienten Buttons/Menüpunkte. */
    public static final String CMD_OPEN_IMAGE   = "OPEN_IMAGE";
    public static final String CMD_REGENERATE   = "REGENERATE";
    public static final String CMD_UNDO         = "UNDO";
    public static final String CMD_REDO         = "REDO";
    public static final String CMD_SAVE         = "SAVE";
    public static final String CMD_SAVE_AS      = "SAVE_AS";
    public static final String CMD_OPEN_PROJECT = "OPEN_PROJECT";

    /** Verzögerung, bevor eine Parameteränderung eine Generierung auslöst. */
    private static final int DEBOUNCE_MS = 150;

    private final MainFrame view;
    private final Creator creator;
    private final Subject subject;

    /** Entprellt schnelle Slider-Bewegungen zu einer Generierung (wie zuvor). */
    private final Timer paramDebounce;

    public CreatorController(MainFrame view, Creator creator, Subject subject) {
        this.view = view;
        this.creator = creator;
        this.subject = subject;
        this.subject.attach(this);

        this.paramDebounce = new Timer(DEBOUNCE_MS, e -> CompletableFuture.runAsync(() -> {
            creator.updateParameters(view.getParameterBuffer());
            creator.createTemplate();
        }));
        this.paramDebounce.setRepeats(false);
    }

    /* -------- Eingaben (ActionListener) -------- */

    @Override
    public void actionPerformed(ActionEvent e) {
        switch (e.getActionCommand()) {
            case CMD_OPEN_IMAGE   -> openImage();
            case CMD_REGENERATE   -> CompletableFuture.runAsync(creator::createTemplate);
            case CMD_UNDO         -> restoreAsync(creator::undo);
            case CMD_REDO         -> restoreAsync(creator::redo);
            case CMD_SAVE         -> save(creator.getProjectFile());
            case CMD_SAVE_AS      -> save(null);
            case CMD_OPEN_PROJECT -> openProject();
            default -> { /* nicht für diesen Controller */ }
        }
    }

    /** Wird vom Parameter-Panel bei jeder Änderung aufgerufen (entprellt). */
    public void onParametersChanged() {
        paramDebounce.restart();
    }

    private void openImage() {
        File file = view.chooseImageFile();   // UI-Dialog – synchron auf dem EDT
        if (file == null) return;
        CompletableFuture
                .runAsync(() -> creator.loadImage(file))
                .thenRunAsync(() -> {
                    creator.updateParameters(view.getParameterBuffer());
                    creator.createTemplate();
                })
                .exceptionally(ex -> {
                    view.showError("Bild konnte nicht geladen werden", unwrap(ex));
                    return null;
                });
    }

    /** Undo/Redo: ausführen, danach die Slider an die wiederhergestellten Parameter angleichen. */
    private void restoreAsync(Runnable op) {
        CompletableFuture.runAsync(op)
                .thenRun(() -> SwingUtilities.invokeLater(
                        () -> view.refreshParameters(creator.getParameters())));
    }

    private void save(File target) {
        File file = (target != null) ? target : view.chooseSaveProjectFile();
        if (file == null) return;
        CompletableFuture
                .runAsync(() -> creator.saveProject(file))
                .thenRun(() -> SwingUtilities.invokeLater(
                        () -> view.setStatus("Projekt gespeichert: " + file.getName())))
                .exceptionally(ex -> {
                    view.showError("Speichern fehlgeschlagen", unwrap(ex));
                    return null;
                });
    }

    private void openProject() {
        File file = view.chooseOpenProjectFile();
        if (file == null) return;
        CompletableFuture
                .runAsync(() -> creator.openProject(file))
                .thenRun(() -> SwingUtilities.invokeLater(
                        () -> view.refreshParameters(creator.getParameters())))
                .exceptionally(ex -> {
                    view.showError("Laden fehlgeschlagen", unwrap(ex));
                    return null;
                });
    }

    /* -------- Zustandsänderungen (Observer) -------- */

    @Override
    public void update(State newState) {
        SwingUtilities.invokeLater(() -> render(newState));
    }

    private void render(State state) {
        boolean hasImage = state.isSubStateOf(CreatorState.HAS_IMAGE);
        boolean hasTemplate = state.isSubStateOf(CreatorState.TEMPLATE_READY);

        view.setActionsEnabled(hasImage, hasTemplate);
        view.setUndoRedoEnabled(creator.canUndo(), creator.canRedo());
        view.setBusy(state == CreatorState.GENERATING);

        if (state == CreatorState.GENERATING) {
            view.setStatus("Erzeuge Vorlage...");
            return;
        }

        // Eingeschwungene Zustände: Anzeige aus dem Modell ableiten (Pull).
        view.setOriginalImage(creator.getOriginal());   // Identitäts-geschützt in der View
        if (hasTemplate) {
            view.setPreview(creator.getPreview());
            Template t = creator.getTemplate();
            if (t != null) {
                view.setRightStatus(t.getCols() + " x " + t.getRows()
                        + " Sticks (" + t.totalSticks() + ")");
            }
        } else {
            view.setPreview(null);
        }
    }

    private static Throwable unwrap(Throwable ex) {
        return (ex.getCause() != null) ? ex.getCause() : ex;
    }
}
