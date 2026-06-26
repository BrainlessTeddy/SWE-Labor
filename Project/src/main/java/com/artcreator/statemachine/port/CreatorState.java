package com.artcreator.statemachine.port;

/**
 * Protokollautomat des Use Case „Vorlage erstellen".
 *
 * <p>Die Zustände bilden eine Hierarchie:</p>
 * <pre>
 * ROOT
 * ├── NO_IMAGE        (Startzustand: noch kein Bild geladen)
 * └── HAS_IMAGE       (Composite: ein Bild liegt vor)
 *     ├── IMAGE_LOADED
 *     ├── GENERATING
 *     └── TEMPLATE_READY
 * </pre>
 *
 * <p>Gültige Trigger je Zustand:</p>
 * <ul>
 *   <li>{@code loadImage}        – gültig in jedem Zustand (Unterzustand von ROOT)</li>
 *   <li>{@code updateParameters} – gültig in jedem Zustand (Unterzustand von ROOT)</li>
 *   <li>{@code createTemplate}   – gültig sobald ein Bild vorliegt (Unterzustand von HAS_IMAGE)</li>
 *   <li>Ergebnis abholen         – nur in TEMPLATE_READY</li>
 * </ul>
 */
public enum CreatorState implements State {

    ROOT(null),
    NO_IMAGE(ROOT),
    HAS_IMAGE(ROOT),
    IMAGE_LOADED(HAS_IMAGE),
    GENERATING(HAS_IMAGE),
    TEMPLATE_READY(HAS_IMAGE);

    private final CreatorState parent;

    CreatorState(CreatorState parent) {
        this.parent = parent;
    }

    @Override
    public boolean isSubStateOf(State other) {
        for (CreatorState s = this; s != null; s = s.parent) {
            if (s == other) return true;
        }
        return false;
    }
}
