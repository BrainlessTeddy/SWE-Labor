package com.artcreator.statemachine;

import com.artcreator.statemachine.impl.StateMachineImpl;
import com.artcreator.statemachine.port.Observer;
import com.artcreator.statemachine.port.State;
import com.artcreator.statemachine.port.StateMachine;

/* Fassade der StateMachine-Komponente. Haelt die einzige StateMachineImpl
 * (erst beim ersten Zugriff angelegt) und reicht alle Aufrufe an sie weiter. */
public final class StateMachineFacade implements StateMachineFactory, StateMachine {

    private StateMachineImpl stateMachineImpl;

    @Override
    public StateMachine stateMachine() {
        ensureInitialized();
        return this;
    }

    private StateMachineImpl impl() {
        ensureInitialized();
        return stateMachineImpl;
    }

    private void ensureInitialized() {
        if (this.stateMachineImpl == null) { // erst beim ersten Zugriff anlegen
            this.stateMachineImpl = new StateMachineImpl();
        }
    }

    // --- alles an die Implementierung weiterreichen ---

    @Override
    public State getState() {
        return impl().getState();
    }

    @Override
    public void setState(State newState) {
        impl().setState(newState);
    }

    @Override
    public void attach(Observer observer) {
        impl().attach(observer);
    }

    @Override
    public void detach(Observer observer) {
        impl().detach(observer);
    }
}
