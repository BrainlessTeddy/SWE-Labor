package com.artcreator.creator.port;

import com.artcreator.model.Parameters;
import com.artcreator.model.Template;
import com.artcreator.statemachine.port.State;
import com.artcreator.statemachine.port.Subject;

import java.awt.image.BufferedImage;
import java.io.File;

/* Das nach aussen sichtbare Interface der Creator-Komponente.
 * Die Operationen sind void und werden vom Controller asynchron aufgerufen.
 * Das Ergebnis kommt nicht zurueck, sondern wird nach dem Zustandswechsel
 * ueber die Getter abgeholt. Creator ist gleichzeitig Subject, damit sich
 * die View/der Controller fuer Zustandsaenderungen anmelden koennen. */
public interface Creator extends Subject {

    // --- Systemoperationen (void, asynchron) ---

    void loadImage(File file);

    void updateParameters(Parameters params);

    void createTemplate();

    // --- nicht Teil des Use Case, aber unterstuetzt ---

    void undo();

    void redo();

    void saveProject(File file);

    void openProject(File file);

    // --- Ergebnis nach dem Zustandswechsel abholen ---

    State getState();

    BufferedImage getOriginal();

    BufferedImage getPreview();

    Template getTemplate();

    Parameters getParameters();

    boolean canUndo();

    boolean canRedo();

    File getProjectFile();

    File getSourceFile();
}
