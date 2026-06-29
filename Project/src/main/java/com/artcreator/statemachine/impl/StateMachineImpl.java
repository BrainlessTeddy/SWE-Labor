package com.artcreator.statemachine.impl;

import java.util.concurrent.CopyOnWriteArrayList;

import com.artcreator.statemachine.port.CreatorState;
import com.artcreator.statemachine.port.Observer;
import com.artcreator.statemachine.port.State;
import com.artcreator.statemachine.port.StateMachine;

/* Implementierung der Zustandsmaschine. Haelt den aktuellen Zustand und die
 Liste der Beobachter. Der Zugriff auf den Zustand ist synchronized; die
 Beobachter werden ausserhalb des Locks benachrichtigt (CopyOnWriteArrayList),
 damit es keine Probleme gibt, wenn ein Beobachter selbst wieder zugreift. */
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
