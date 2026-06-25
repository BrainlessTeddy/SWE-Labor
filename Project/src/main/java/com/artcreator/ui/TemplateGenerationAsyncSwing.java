package com.artcreator.ui;

import com.artcreator.application.*;
import com.artcreator.model.Parameters;
import com.artcreator.template.TemplateGenerator;

import javax.swing.SwingWorker;
import java.awt.image.BufferedImage;

/**
 * Swing-spezifische Implementierung von {@link AsyncTemplateGenerationService}.
 *
 * <p>Fuehrt {@link TemplateCreationUseCase#createTemplate} auf einem
 * {@link SwingWorker}-Hintergrundthread aus und ruft den {@link
 * GenerationCallback} anschliessend auf dem Event-Dispatch-Thread auf (das
 * macht {@code SwingWorker} ueber {@code done()} automatisch).
 *
 * <p>Dies ist absichtlich die einzige Klasse in der gesamten Generierungs-
 * Kette, die {@code SwingWorker} kennt. Die eigentliche Use-Case-Logik in
 * {@link TemplateCreationUseCase} bleibt davon unberuehrt.</p>
 */
public final class TemplateGenerationAsyncSwing implements TemplateGenerationAsync {

    private final TemplateGeneration tempGeneration;
    private SwingWorker<TemplateGenerator.GenerationResult, Void> running;

    public TemplateGenerationAsyncSwing(TemplateGeneration tempGeneration) {
        this.tempGeneration = tempGeneration;
    }

    @Override
    public void generateAsync(BufferedImage source, Parameters params, InnerGenerationCallback callback) {
        cancelPending();
        running = new SwingWorker<>() {
            @Override protected TemplateGenerator.GenerationResult doInBackground() {
                return tempGeneration.createTemplate(source, params);
            }
            @Override protected void done() {
                if (isCancelled()) return;
                try {
                    callback.onSuccess(get());
                } catch (Exception ex) {
                    callback.onFailure(unwrap(ex));
                }
            }
        };
        running.execute();
    }

    @Override
    public void cancelPending() {
        if (running != null && !running.isDone()) {
            running.cancel(true);
        }
    }

    /** {@code SwingWorker.get()} wickelt echte Fehler in {@link java.util.concurrent.ExecutionException} ein. */
    private static Throwable unwrap(Exception ex) {
        Throwable cause = ex.getCause();
        return cause != null ? cause : ex;
    }
}