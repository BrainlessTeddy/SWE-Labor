package com.artcreator.statemachine;

import com.artcreator.statemachine.impl.StateMachineImpl;
import com.artcreator.statemachine.port.Observer;
import com.artcreator.statemachine.port.State;
import com.artcreator.statemachine.port.StateMachine;

/**
 * Fassade der StateMachine-Komponente. Kapselt die einzige
 * {@link StateMachineImpl}-Instanz (lazy initialization) und delegiert alle
 * Zugriffe an sie.
 */
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
        if (this.stateMachineImpl == null) { /* lazy initialization */
            this.stateMachineImpl = new StateMachineImpl();
        }
    }

    /* -------- delegation to the implementation -------- */

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
