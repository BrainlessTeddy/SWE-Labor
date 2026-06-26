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

/**
 * Fassade der Creator-Komponente.
 *
 * <p>Überwacht den Zugriff auf die Systemoperationen: vor jeder Operation wird
 * geprüft, ob sie im aktuellen Zustand ein gültiger Trigger ist; danach wird
 * der von der Implementierung gelieferte Folgezustand in die Zustandsmaschine
 * geschrieben. Die Operationen sind synchronisiert, damit nebenläufige Aufrufe
 * (asynchrone Controller) den Zustand nicht verschränken.</p>
 */
public final class CreatorFacade implements CreatorFactory, Creator {

    private CreatorImpl creatorImpl;
    private StateMachine stateMachine;

    @Override
    public Creator creator() {
        if (this.creatorImpl == null) { /* lazy initialization */
            this.stateMachine = StateMachineFactory.FACTORY.stateMachine();
            this.creatorImpl = new CreatorImpl();
        }
        return this;
    }

    /* -------- Systemoperationen (abgesichert + synchronisiert) -------- */

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
        // kein Zustandsübergang: reine Parameterübernahme
    }

    @Override
    public synchronized void createTemplate() {
        if (!stateMachine.getState().isSubStateOf(CreatorState.HAS_IMAGE)) return;
        stateMachine.setState(CreatorState.GENERATING);
        State newState = creatorImpl.createTemplate();
        stateMachine.setState(newState);
    }

    /* -------- Subject (delegiert an die Zustandsmaschine) -------- */

    @Override
    public void attach(Observer observer) {
        stateMachine.attach(observer);
    }

    @Override
    public void detach(Observer observer) {
        stateMachine.detach(observer);
    }

    /* -------- reads -------- */

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
}
