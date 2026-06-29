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

/* Controller fuer den Use Case "Vorlage erstellen".
 * Nimmt die Eingaben der View entgegen und ruft die Operationen der Creator-Fassade
 * asynchron auf. Die Operationen sind void; das Ergebnis holt sich der Controller
 * als Observer in update() ab, nachdem sich der Zustand geaendert hat. */
public final class CreatorController implements ActionListener, Observer {

    // Namen der Buttons/Menuepunkte, die dieser Controller bedient
    public static final String CMD_OPEN_IMAGE   = "OPEN_IMAGE";
    public static final String CMD_REGENERATE   = "REGENERATE";
    public static final String CMD_UNDO         = "UNDO";
    public static final String CMD_REDO         = "REDO";
    public static final String CMD_SAVE         = "SAVE";
    public static final String CMD_SAVE_AS      = "SAVE_AS";
    public static final String CMD_OPEN_PROJECT = "OPEN_PROJECT";

    // Wartezeit, bis eine Parameteraenderung wirklich neu generiert
    private static final int DEBOUNCE_MS = 150;

    private final MainFrame view;
    private final Creator creator;
    private final Subject subject;

    // fasst schnelles Slider-Ziehen zu einer Generierung zusammen
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

    // --- Eingaben (ActionListener) ---

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
            default -> { } // nicht fuer diesen Controller
        }
    }

    // ruft das Parameter-Panel bei jeder Aenderung auf (mit Debounce)
    public void onParametersChanged() {
        paramDebounce.restart();
    }

    private void openImage() {
        File file = view.chooseImageFile();   // Dialog laeuft auf dem EDT
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

    // Undo/Redo ausfuehren und danach die Slider auf die alten Werte zuruecksetzen
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

    // --- Zustandsaenderungen (Observer) ---

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

        // fertiger Zustand: Anzeige aus dem Modell holen
        view.setOriginalImage(creator.getOriginal());   // View ignoriert gleiches Bild
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
