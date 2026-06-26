package com.artcreator.statemachine;

import com.artcreator.statemachine.port.StateMachine;

/**
 * Zugang zur StateMachine-Komponente. Liefert über die zentrale
 * {@link #FACTORY}-Instanz die einzige Zustandsmaschine des Systems.
 */
public interface StateMachineFactory {

    /** Statischer Zugriff auf die Fassade. */
    StateMachineFactory FACTORY = new StateMachineFacade();

    /** Liefert die Implementierung des Interface {@link StateMachine}. */
    StateMachine stateMachine();
}
