package com.artcreator.creator;

import com.artcreator.creator.impl.CreatorImpl;
import com.artcreator.creator.port.Creator;
import com.artcreator.model.Parameters;
import com.artcreator.model.Template;
import com.artcreator.statemachine.StateMachineFactory;
import com.artcreator.statemachine.port.CreatorState;
import com.artcreator.statemachine.port.Observer;
import com.artcreator.statemachine.port.State;
import com.artcreator.statemachine.port.StateMachine;

import java.awt.image.BufferedImage;
import java.io.File;

/* Fassade der Creator-Komponente. Prueft vor jeder Operation den Zustand,
 * gibt sie an die Implementierung weiter und setzt danach den neuen Zustand.
 * Alles synchronized, damit sich die asynchronen Aufrufe nicht ins Gehege kommen. */
public final class CreatorFacade implements CreatorFactory, Creator {

    private CreatorImpl creatorImpl;
    private StateMachine stateMachine;

    @Override
    public Creator creator() {
        if (this.creatorImpl == null) { // erst beim ersten Zugriff anlegen
            this.stateMachine = StateMachineFactory.FACTORY.stateMachine();
            this.creatorImpl = new CreatorImpl();
        }
        return this;
    }

    // --- Systemoperationen ---

    @Override
    public synchronized void loadImage(File file) {
        if (!stateMachine.getState().isSubStateOf(CreatorState.ROOT)) return;
        State newState = creatorImpl.loadImage(file);
        stateMachine.setState(newState);
    }

    @Override
    public synchronized void updateParameters(Parameters params) {
        if (!stateMachine.getState().isSubStateOf(CreatorState.ROOT)) return;
        creatorImpl.updateParameters(params);
        // nur Parameter uebernehmen, kein Zustandswechsel
    }

    @Override
    public synchronized void createTemplate() {
        if (!stateMachine.getState().isSubStateOf(CreatorState.HAS_IMAGE)) return;
        stateMachine.setState(CreatorState.GENERATING);
        State newState = creatorImpl.createTemplate();
        stateMachine.setState(newState);
    }

    @Override
    public synchronized void undo() {
        if (!creatorImpl.canUndo()) return;
        stateMachine.setState(CreatorState.GENERATING);
        stateMachine.setState(creatorImpl.undo());
    }

    @Override
    public synchronized void redo() {
        if (!creatorImpl.canRedo()) return;
        stateMachine.setState(CreatorState.GENERATING);
        stateMachine.setState(creatorImpl.redo());
    }

    @Override
    public synchronized void saveProject(File file) {
        creatorImpl.saveProject(file);   // Fehler kommt als UncheckedIOException
    }

    @Override
    public synchronized void openProject(File file) {
        stateMachine.setState(CreatorState.GENERATING);
        stateMachine.setState(creatorImpl.openProject(file));
    }

    // --- Observer an-/abmelden (geht an die Zustandsmaschine) ---

    @Override
    public void attach(Observer observer) {
        stateMachine.attach(observer);
    }

    @Override
    public void detach(Observer observer) {
        stateMachine.detach(observer);
    }

    // --- Getter, um das Ergebnis abzuholen ---

    @Override
    public State getState() {
        return stateMachine.getState();
    }

    @Override
    public BufferedImage getOriginal() {
        return creatorImpl.getOriginal();
    }

    @Override
    public BufferedImage getPreview() {
        return creatorImpl.getPreview();
    }

    @Override
    public Template getTemplate() {
        return creatorImpl.getTemplate();
    }

    @Override
    public Parameters getParameters() {
        return creatorImpl.getParameters();
    }

    @Override
    public boolean canUndo() {
        return creatorImpl.canUndo();
    }

    @Override
    public boolean canRedo() {
        return creatorImpl.canRedo();
    }

    @Override
    public File getProjectFile() {
        return creatorImpl.getProjectFile();
    }

    @Override
    public File getSourceFile() {
        return creatorImpl.getSourceFile();
    }
}
