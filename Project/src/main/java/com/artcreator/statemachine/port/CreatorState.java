package com.artcreator.statemachine.port;

/* Die Zustaende des Use Case "Vorlage erstellen", als Baum:
 *
 *   ROOT
 *   ├── NO_IMAGE        Start, noch kein Bild
 *   └── HAS_IMAGE       ein Bild liegt vor
 *       ├── IMAGE_LOADED
 *       ├── GENERATING
 *       └── TEMPLATE_READY
 *
 * Wann ist welche Operation erlaubt:
 *   loadImage / updateParameters  -> immer (alles ist Unterzustand von ROOT)
 *   createTemplate                -> nur wenn ein Bild da ist (unter HAS_IMAGE)
 *   Ergebnis abholen              -> nur in TEMPLATE_READY */
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
