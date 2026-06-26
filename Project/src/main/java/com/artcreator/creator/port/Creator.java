package com.artcreator.creator.port;

import com.artcreator.model.Parameters;
import com.artcreator.model.Template;
import com.artcreator.statemachine.port.State;
import com.artcreator.statemachine.port.Subject;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Fachlicher Port des Use Case „Vorlage erstellen".
 *
 * <p>Die Systemoperationen sind {@code void} und werden vom Controller
 * asynchron aufgerufen. Ergebnisse werden nicht zurückgegeben, sondern nach
 * der zugehörigen Zustandsänderung über die Getter abgeholt (Pull statt
 * Push).</p>
 *
 * <p>{@link Creator} ist zugleich {@link Subject}: Views/Controller
 * registrieren sich, um über Zustandsänderungen informiert zu werden.</p>
 */
public interface Creator extends Subject {

    /* -------- Systemoperationen (void, asynchron) -------- */

    /** Lädt ein Bild von der Platte als neue Quelle. */
    void loadImage(File file);

    /** Übernimmt die aktuellen Parameter (ohne sofort zu generieren). */
    void updateParameters(Parameters params);

    /** Erzeugt die Vorlage aus Quelle + aktuellen Parametern. */
    void createTemplate();

    /* -------- Ergebnis nach Zustandsänderung abholen -------- */

    State getState();

    BufferedImage getOriginal();

    BufferedImage getPreview();

    Template getTemplate();

    Parameters getParameters();
}
