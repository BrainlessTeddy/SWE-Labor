package com.artcreator.statemachine.port;

/**
 * Ein Zustand des Protokollautomaten.
 *
 * <p>Zustände sind hierarchisch (Composite States): {@link #isSubStateOf(State)}
 * erlaubt den Fassaden, eine Systemoperation gegen einen ganzen Teilbaum von
 * Zuständen abzusichern, anstatt jeden Einzelzustand aufzuzählen.</p>
 */
public interface State {

    /** Eindeutiger Name des Zustands. */
    String name();

    /**
     * @return {@code true}, wenn dieser Zustand gleich {@code other} ist oder
     *         (transitiv) ein Unterzustand von {@code other} ist.
     */
    boolean isSubStateOf(State other);
}
