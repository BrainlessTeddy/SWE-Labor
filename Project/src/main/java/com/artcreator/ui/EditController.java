package com.artcreator.ui;

import com.artcreator.history.UndoRedoManager;
import com.artcreator.model.Project;
import com.artcreator.model.Template;
import com.artcreator.template.TemplateGenerator;
import com.artcreator.template.TemplateRenderer;

import javax.swing.SwingWorker;
import java.awt.image.BufferedImage;

/**
 * Coordinates regeneration of the template in response to user actions.
 *
 * <p>Regeneration runs on a background thread via {@link SwingWorker} so the
 * UI stays responsive even on large images. Parameter changes are debounced
 * by 150 ms so a slider drag doesn't fire dozens of overlapping jobs.</p>
 */
public final class EditController {

    public interface Listener {
        void onProjectChanged(Project project);
        void onTemplateGenerated(Project project, BufferedImage previewImage);
        void onError(String message, Throwable cause);
        void onBusyStateChanged(boolean busy);
    }

    private final Project project;
    private final UndoRedoManager history = new UndoRedoManager();
    private final Listener listener;
    private final javax.swing.Timer debounce;
    private SwingWorker<TemplateGenerator.GenerationResult, Void> running;
    private boolean snapshotPendingForNextChange = true;

    public EditController(Project project, Listener listener) {
        this.project = project;
        this.listener = listener;
        this.debounce = new javax.swing.Timer(150, e -> regenerateNow());
        debounce.setRepeats(false);
    }

    public Project getProject() { return project; }
    public UndoRedoManager getHistory() { return history; }

    /** Call from sliders / spinners. Bookkeeps undo + debounced regeneration. */
    public void parametersChanged() {
        if (snapshotPendingForNextChange) {
            history.snapshot(project);
            snapshotPendingForNextChange = false;
        }
        project.markDirty();
        listener.onProjectChanged(project);
        debounce.restart();
    }

    /** End the current "edit batch" so the next change creates a new undo step. */
    public void commitEditBatch() { snapshotPendingForNextChange = true; }

    public void regenerateNow() {
        if (project.getOriginal() == null) return;
        if (running != null && !running.isDone()) running.cancel(true);

        listener.onBusyStateChanged(true);
        running = new SwingWorker<>() {
            @Override protected TemplateGenerator.GenerationResult doInBackground() {
                return TemplateGenerator.generate(project.getOriginal(), project.getParameters());
            }
            @Override protected void done() {
                try {
                    if (isCancelled()) return;
                    TemplateGenerator.GenerationResult r = get();
                    project.setProcessed(r.processedImage);
                    project.setTemplate(r.template);
                    BufferedImage preview = TemplateRenderer.renderPreview(
                            r.template, 96.0,
                            project.getParameters().isShowColorCodes(),
                            project.getParameters().isShowGrid());
                    listener.onTemplateGenerated(project, preview);
                } catch (Exception ex) {
                    listener.onError("Vorlage konnte nicht erzeugt werden", ex);
                } finally {
                    listener.onBusyStateChanged(false);
                }
            }
        };
        running.execute();
    }

    public void undo() {
        if (history.undo(project)) {
            commitEditBatch();
            listener.onProjectChanged(project);
            regenerateNow();
        }
    }
    public void redo() {
        if (history.redo(project)) {
            commitEditBatch();
            listener.onProjectChanged(project);
            regenerateNow();
        }
    }

    public void setOriginalImage(BufferedImage img) {
        history.snapshot(project);
        project.setOriginal(img);
        commitEditBatch();
        listener.onProjectChanged(project);
        regenerateNow();
    }

    public Template getTemplateOrThrow() {
        if (project.getTemplate() == null) {
            throw new IllegalStateException("No template generated yet");
        }
        return project.getTemplate();
    }
}
