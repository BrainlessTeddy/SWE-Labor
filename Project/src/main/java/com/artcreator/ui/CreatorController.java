package com.artcreator.ui;

import com.artcreator.creator.port.Creator;
import com.artcreator.model.Template;
import com.artcreator.statemachine.port.CreatorState;
import com.artcreator.statemachine.port.Observer;
import com.artcreator.statemachine.port.State;
import com.artcreator.statemachine.port.Subject;

import javax.swing.SwingUtilities;
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
    public static final String CMD_OPEN_IMAGE = "OPEN_IMAGE";
    public static final String CMD_REGENERATE = "REGENERATE";

    private final MainFrame view;
    private final Creator creator;
    private final Subject subject;

    public CreatorController(MainFrame view, Creator creator, Subject subject) {
        this.view = view;
        this.creator = creator;
        this.subject = subject;
        this.subject.attach(this);
    }

    /* -------- Eingaben (ActionListener) -------- */

    @Override
    public void actionPerformed(ActionEvent e) {
        switch (e.getActionCommand()) {
            case CMD_OPEN_IMAGE -> openImage();
            case CMD_REGENERATE -> CompletableFuture.runAsync(creator::createTemplate);
            default -> { /* nicht für diesen Controller */ }
        }
    }

    /** Wird vom Parameter-Panel bei jeder Änderung aufgerufen. */
    public void onParametersChanged() {
        CompletableFuture.runAsync(() -> {
            creator.updateParameters(view.getParameterBuffer());
            creator.createTemplate();
        });
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

    /* -------- Zustandsänderungen (Observer) -------- */

    @Override
    public void update(State newState) {
        SwingUtilities.invokeLater(() -> render(newState));
    }

    private void render(State state) {
        boolean hasImage = state.isSubStateOf(CreatorState.HAS_IMAGE);
        boolean hasTemplate = state.isSubStateOf(CreatorState.TEMPLATE_READY);
        view.setActionsEnabled(hasImage, hasTemplate);

        if (state == CreatorState.IMAGE_LOADED) {
            view.setBusy(false);
            view.setOriginalImage(creator.getOriginal());
        } else if (state == CreatorState.GENERATING) {
            view.setBusy(true);
            view.setStatus("Erzeuge Vorlage...");
        } else if (state == CreatorState.TEMPLATE_READY) {
            view.setBusy(false);
            view.setPreview(creator.getPreview());
            Template t = creator.getTemplate();
            if (t != null) {
                view.setRightStatus(t.getCols() + " x " + t.getRows()
                        + " Sticks (" + t.totalSticks() + ")");
            }
        }
    }

    private static Throwable unwrap(Throwable ex) {
        return (ex.getCause() != null) ? ex.getCause() : ex;
    }
}
