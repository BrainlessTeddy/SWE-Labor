package com.artcreator.statemachine.impl;

import com.artcreator.statemachine.port.CreatorState;
import com.artcreator.statemachine.port.Observer;
import com.artcreator.statemachine.port.State;
import com.artcreator.statemachine.port.StateMachine;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implementierung der Zustandsmaschine.
 *
 * <p>Hält den aktuellen Zustand und eine Liste registrierter Beobachter. Der
 * Zustandszugriff ist synchronisiert; die Benachrichtigung der Beobachter
 * erfolgt außerhalb des Locks über eine {@link CopyOnWriteArrayList}, damit
 * Beobachter nicht reentrant in den Lock laufen.</p>
 */
public final class StateMachineImpl implements StateMachine {

    private State current = CreatorState.NO_IMAGE;
    private final CopyOnWriteArrayList<Observer> observers = new CopyOnWriteArrayList<>();

    @Override
    public synchronized State getState() {
        return current;
    }

    @Override
    public void setState(State newState) {
        synchronized (this) {
            this.current = newState;
        }
        for (Observer o : observers) {
            o.update(newState);
        }
    }

    @Override
    public void attach(Observer observer) {
        observers.addIfAbsent(observer);
    }

    @Override
    public void detach(Observer observer) {
        observers.remove(observer);
    }
}
