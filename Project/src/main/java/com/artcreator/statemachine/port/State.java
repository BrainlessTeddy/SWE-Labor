package com.artcreator.statemachine.port;

/* Ein Zustand im Automaten. Zustaende koennen ineinander verschachtelt sein;
 * mit isSubStateOf kann die Fassade gegen einen ganzen Teilbaum pruefen,
 * statt jeden Einzelzustand aufzuzaehlen. */
public interface State {

    String name();

    // true, wenn dieser Zustand gleich other oder ein Unterzustand davon ist
    boolean isSubStateOf(State other);
}
