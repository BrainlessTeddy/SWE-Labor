package com.artcreator.statemachine;

import com.artcreator.statemachine.port.StateMachine;

/* Einstieg in die StateMachine-Komponente. Ueber FACTORY bekommt man die
 * einzige Zustandsmaschine des Systems. */
public interface StateMachineFactory {

    StateMachineFactory FACTORY = new StateMachineFacade();

    StateMachine stateMachine();
}
