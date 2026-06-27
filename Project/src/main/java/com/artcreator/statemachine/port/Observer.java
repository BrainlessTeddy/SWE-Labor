package com.artcreator.statemachine.port;

/**
 * Beobachter der Zustandsmaschine (MVC: Controller/View).
 *
 * <p>Wird nach jedem Zustandsübergang mit dem neuen Zustand benachrichtigt und
 * kann daraufhin das zugehörige Ergebnis abholen und sich aktualisieren.</p>
 */
public interface Observer {

    void update(State newState);
}
