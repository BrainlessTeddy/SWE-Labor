package com.artcreator.history;

import com.artcreator.model.Parameters;
import com.artcreator.model.Project;

import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Snapshot-based undo / redo. The application calls {@link #snapshot(Project)}
 * before mutating state; {@link #undo(Project)} / {@link #redo(Project)} swap
 * the snapshot back into the project.
 *
 * <p>Snapshots store a deep copy of the {@link Parameters} object plus a
 * reference to the original image (which is treated as immutable while it is
 * the active source). The processed image and template are not snapshotted
 * because they will be re-derived on the next preview pass.</p>
 */
public final class UndoRedoManager {

    private final int capacity;
    private final Deque<Snapshot> undoStack = new ArrayDeque<>();
    private final Deque<Snapshot> redoStack = new ArrayDeque<>();

    public UndoRedoManager() { this(50); }
    public UndoRedoManager(int capacity) { this.capacity = capacity; }

    /** Capture the current project state so it can later be restored. */
    public void snapshot(Project p) {
        if (p == null) return;
        undoStack.push(new Snapshot(p.getParameters().copy(), p.getOriginal()));
        if (undoStack.size() > capacity) undoStack.removeLast();
        redoStack.clear();
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }

    public boolean undo(Project p) {
        if (undoStack.isEmpty() || p == null) return false;
        // push the current state onto the redo stack first
        redoStack.push(new Snapshot(p.getParameters().copy(), p.getOriginal()));
        Snapshot s = undoStack.pop();
        p.setParameters(s.params);
        p.setOriginal(s.original);
        return true;
    }

    public boolean redo(Project p) {
        if (redoStack.isEmpty() || p == null) return false;
        undoStack.push(new Snapshot(p.getParameters().copy(), p.getOriginal()));
        Snapshot s = redoStack.pop();
        p.setParameters(s.params);
        p.setOriginal(s.original);
        return true;
    }

    public void clear() { undoStack.clear(); redoStack.clear(); }

    /* ----------------------------------------------------- */

    private static final class Snapshot {
        final Parameters params;
        final BufferedImage original;
        Snapshot(Parameters params, BufferedImage original) {
            this.params = params; this.original = original;
        }
    }
}
