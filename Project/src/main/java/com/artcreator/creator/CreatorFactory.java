package com.artcreator.creator;

import com.artcreator.creator.port.Creator;

/**
 * Zugang zur Creator-Komponente. Liefert über die zentrale {@link #FACTORY}
 * die Implementierung des fachlichen Ports {@link Creator}.
 */
public interface CreatorFactory {

    /** Statischer Zugriff auf die Fassade. */
    CreatorFactory FACTORY = new CreatorFacade();

    /** Liefert die Implementierung des Interface {@link Creator}. */
    Creator creator();
}
