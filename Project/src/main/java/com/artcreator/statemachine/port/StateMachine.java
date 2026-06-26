package com.artcreator.statemachine.port;

/**
 * Zentrale Zustandsmaschine. Hält den aktuellen Zustand des Use Case und
 * benachrichtigt registrierte {@link Observer} über Zustandsübergänge.
 *
 * <p>Die Fassaden der fachlichen Komponenten sichern ihre Systemoperationen
 * über {@link #getState()} ab und schalten den Zustand anschließend über
 * {@link #setState(State)} weiter.</p>
 */
public interface StateMachine extends Subject {

    /** Aktueller Zustand. */
    State getState();

    /** Setzt den neuen Zustand und benachrichtigt alle Beobachter. */
    void setState(State newState);
}
