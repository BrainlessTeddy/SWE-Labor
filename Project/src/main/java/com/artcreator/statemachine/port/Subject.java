package com.artcreator.statemachine.port;

// Subjekt im Observer-Pattern: hier melden sich die Beobachter an und ab.
public interface Subject {

    void attach(Observer observer);

    void detach(Observer observer);
}
