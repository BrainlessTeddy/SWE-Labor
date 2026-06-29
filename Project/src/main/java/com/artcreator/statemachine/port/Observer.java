package com.artcreator.statemachine.port;

/* Beobachter der Zustandsmaschine (in der MVC-Struktur der Controller).
 * Wird bei jedem Zustandswechsel benachrichtigt und kann sich dann aktualisieren. */
public interface Observer {

    void update(State newState);
}
