package com.artcreator.statemachine.port;

/**
 * Beobachtbares Subjekt im Sinne des Observer-Patterns.
 */
public interface Subject {

    void attach(Observer observer);

    void detach(Observer observer);
}
