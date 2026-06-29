package com.artcreator.creator;

import com.artcreator.creator.port.Creator;

/* Einstieg in die Creator-Komponente. Ueber FACTORY kommt man an die Fassade,
 * creator() gibt die Implementierung des Creator-Interface zurueck. */
public interface CreatorFactory {

    CreatorFactory FACTORY = new CreatorFacade();   // einziger Zugang von aussen

    Creator creator();
}
