package com.artcreator;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.artcreator.creator.CreatorFactory;
import com.artcreator.creator.port.Creator;
import com.artcreator.statemachine.StateMachineFactory;
import com.artcreator.statemachine.port.StateMachine;
import com.artcreator.ui.CreatorController;
import com.artcreator.ui.MainFrame;

/* Startpunkt der Anwendung. Setzt die Komponenten zusammen (Zustandsmaschine,
 * Creator-Fassade, View und Controller) und zeigt das Hauptfenster. */
public final class Main {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // fall back to the default Metal L&F; not fatal
        }

        // Zugriff auf die Komponenten nur ueber die Factories/Fassaden
        StateMachine stateMachine = StateMachineFactory.FACTORY.stateMachine();
        Creator creator = CreatorFactory.FACTORY.creator();

        SwingUtilities.invokeLater(() -> {
            MainFrame view = new MainFrame(creator);
            CreatorController controller = new CreatorController(view, creator, stateMachine);
            view.setController(controller);
            view.setVisible(true);
        });
    }
}
