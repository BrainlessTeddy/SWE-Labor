package com.artcreator.statemachine.port;

/* Die zentrale Zustandsmaschine. Haelt den aktuellen Zustand und benachrichtigt
 * die angemeldeten Observer. Die Fassaden pruefen mit getState() ab und schalten
 * mit setState() weiter. */
public interface StateMachine extends Subject {

    State getState();

    void setState(State newState);
}
