package com.artcreator.ui;

import com.artcreator.creator.port.Creator;
import com.artcreator.io.ProjectIO;
import com.artcreator.model.Parameters;
import com.artcreator.output.BuildInstructionsGenerator;
import com.artcreator.output.PartsListGenerator;
import com.artcreator.output.PdfExporter;
import com.artcreator.output.PngExporter;
import com.artcreator.output.PrintAdapter;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Top-level Swing-Fenster – im Sinne von MVC eine <b>reine View</b>.
 *
 * <p>Die View enthält keine Geschäftslogik: Sie baut die Widgets auf, leitet
 * Aktionen über Action-Commands an den {@link CreatorController} weiter und
 * bietet Anzeige-Methoden ({@code setPreview}, {@code setBusy} …), die der
 * Controller nach Zustandsänderungen aufruft. Das Domänenmodell liegt hinter
 * der {@link Creator}-Fassade; die View greift nur lesend darauf zu, um
 * Ergebnisse anzuzeigen bzw. (für nicht zum Use Case gehörende Aktionen wie
 * Export/Druck) die fertige Vorlage abzuholen.</p>
 */
@SuppressWarnings("serial")
public final class MainFrame extends JFrame {

    private final Creator creator;

    /** Eingabepuffer der View für die Parameter (wird vom Controller transportiert). */
    private Parameters parameterBuffer = new Parameters();

    private final ComparisonPanel comparison = new ComparisonPanel();
    private ParameterPanel parameters;
    private final JLabel status = new JLabel(" ");
    private final JProgressBar busy = new JProgressBar();
    private final JSplitPane mainSplit;

    private JMenuItem miUndo, miRedo, miSave, miSaveAs,
            miExportPng, miExportPdf, miPrint, miPartsList, miBuildInstructions;

    /** Widgets, deren Klick eine Operation auslöst (vom Controller verdrahtet). */
    private final List<AbstractButton> controllerTriggers = new ArrayList<>();

    /** Wird vom Controller gesetzt; Default ist ein No-op. */
    private Runnable paramChangeHandler = () -> { };

    /** Zuletzt angezeigtes Originalbild – Identitäts-Schutz gegen unnötige Repaints. */
    private BufferedImage lastOriginalShown;

    public MainFrame(Creator creator) {
        super("3D-Art-Creator");
        this.creator = creator;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(1400, screen.width - 100), Math.min(900, screen.height - 100));
        setLocationRelativeTo(null);

        parameters = new ParameterPanel(parameterBuffer, () -> paramChangeHandler.run());
        setJMenuBar(buildMenuBar());

        mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, comparison, parameters);
        mainSplit.setResizeWeight(1.0);
        mainSplit.setBorder(null);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(buildToolbar(), BorderLayout.NORTH);
        getContentPane().add(mainSplit, BorderLayout.CENTER);
        getContentPane().add(buildStatusBar(), BorderLayout.SOUTH);

        setActionsEnabled(false, false);
        setUndoRedoEnabled(false, false);
    }

    /**
     * Verbindet die Aktionen der View mit dem Controller. Wird nach der
     * Konstruktion von außen aufgerufen (zweiphasiges Wiring, da Controller und
     * View sich gegenseitig kennen müssen).
     */
    public void setController(CreatorController controller) {
        for (AbstractButton b : controllerTriggers) b.addActionListener(controller);
        this.paramChangeHandler = controller::onParametersChanged;
    }

    /* ============================ View-API ============================ */

    /** Öffnet einen Datei-Dialog zur Bildauswahl; {@code null} bei Abbruch. */
    public File chooseImageFile() {
        JFileChooser ch = new JFileChooser();
        ch.setFileFilter(new FileNameExtensionFilter(
                "Bilddateien (jpg, png, bmp, gif)", "jpg", "jpeg", "png", "bmp", "gif"));
        if (ch.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return null;
        return ch.getSelectedFile();
    }

    /** Datei-Dialog zum Speichern eines Projekts; hängt die Endung an. */
    public File chooseSaveProjectFile() {
        JFileChooser ch = new JFileChooser();
        ch.setFileFilter(new FileNameExtensionFilter(
                "3D-Art Projektdatei (." + ProjectIO.EXTENSION + ")", ProjectIO.EXTENSION));
        if (ch.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return null;
        File f = ch.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith("." + ProjectIO.EXTENSION)) {
            f = new File(f.getParentFile(), f.getName() + "." + ProjectIO.EXTENSION);
        }
        return f;
    }

    /** Datei-Dialog zum Öffnen eines Projekts; {@code null} bei Abbruch. */
    public File chooseOpenProjectFile() {
        JFileChooser ch = new JFileChooser();
        ch.setFileFilter(new FileNameExtensionFilter(
                "3D-Art Projektdatei (." + ProjectIO.EXTENSION + ")", ProjectIO.EXTENSION));
        if (ch.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return null;
        return ch.getSelectedFile();
    }

    /** Aktueller Eingabepuffer der Parameter (vom Controller an die Fassade gereicht). */
    public Parameters getParameterBuffer() {
        return parameterBuffer;
    }

    /** Baut das Parameter-Panel neu auf (nach Undo/Redo/Projekt öffnen). */
    public void refreshParameters(Parameters p) {
        this.parameterBuffer = p.copy();
        ParameterPanel fresh = new ParameterPanel(parameterBuffer, () -> paramChangeHandler.run());
        mainSplit.setRightComponent(fresh);
        mainSplit.revalidate();
        mainSplit.repaint();
        this.parameters = fresh;
    }

    public void setOriginalImage(BufferedImage img) {
        if (img == lastOriginalShown) return;   // gleiche Instanz -> kein Repaint/Zoom-Reset
        lastOriginalShown = img;
        comparison.setOriginal(img);
    }

    public void setPreview(BufferedImage preview) {
        comparison.setTemplatePreview(preview);
    }

    public void setRightStatus(String text) {
        comparison.setRightStatus(text);
    }

    public void setStatus(String text) {
        status.setText(text);
    }

    public void setBusy(boolean busyNow) {
        busy.setVisible(busyNow);
        busy.setIndeterminate(busyNow);
    }

    public void setActionsEnabled(boolean hasImage, boolean hasTemplate) {
        if (miSave != null)              miSave.setEnabled(hasImage);
        if (miSaveAs != null)            miSaveAs.setEnabled(hasImage);
        if (miExportPng != null)         miExportPng.setEnabled(hasTemplate);
        if (miExportPdf != null)         miExportPdf.setEnabled(hasTemplate);
        if (miPrint != null)             miPrint.setEnabled(hasTemplate);
        if (miPartsList != null)         miPartsList.setEnabled(hasTemplate);
        if (miBuildInstructions != null) miBuildInstructions.setEnabled(hasTemplate);
    }

    public void setUndoRedoEnabled(boolean canUndo, boolean canRedo) {
        if (miUndo != null) miUndo.setEnabled(canUndo);
        if (miRedo != null) miRedo.setEnabled(canRedo);
    }

    public void showError(String msg, Throwable t) {
        if (t != null) t.printStackTrace();
        JOptionPane.showMessageDialog(this, msg + (t != null ? ":\n" + t.getMessage() : ""),
                "Fehler", JOptionPane.ERROR_MESSAGE);
    }

    /* ============================ View-Aufbau ============================ */

    private JMenuBar buildMenuBar() {
        int mod = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        JMenuBar mb = new JMenuBar();
        JMenu file = new JMenu("Datei");
        file.add(trigger(item("Bild oeffnen...", KeyStroke.getKeyStroke(KeyEvent.VK_O, mod)),
                CreatorController.CMD_OPEN_IMAGE));
        file.addSeparator();
        miSave = trigger(item("Projekt speichern", KeyStroke.getKeyStroke(KeyEvent.VK_S, mod)),
                CreatorController.CMD_SAVE);
        miSaveAs = trigger(item("Projekt speichern unter...", null), CreatorController.CMD_SAVE_AS);
        file.add(miSave);
        file.add(miSaveAs);
        file.add(trigger(item("Projekt oeffnen...",
                        KeyStroke.getKeyStroke(KeyEvent.VK_O, mod | InputEvent.SHIFT_DOWN_MASK)),
                CreatorController.CMD_OPEN_PROJECT));
        file.addSeparator();
        miExportPng = item("PNG-Seiten exportieren...", null);
        miExportPng.addActionListener(e -> exportPng());
        miExportPdf = item("PDF exportieren...", null);
        miExportPdf.addActionListener(e -> exportPdf());
        miPrint = item("Drucken...", KeyStroke.getKeyStroke(KeyEvent.VK_P, mod));
        miPrint.addActionListener(e -> printTemplate());
        file.add(miExportPng);
        file.add(miExportPdf);
        file.add(miPrint);
        file.addSeparator();
        miPartsList = item("Bestueckungsliste speichern...", null);
        miPartsList.addActionListener(e -> savePartsList());
        miBuildInstructions = item("Bauanleitung speichern...", null);
        miBuildInstructions.addActionListener(e -> saveBuildInstructions());
        file.add(miPartsList);
        file.add(miBuildInstructions);
        file.addSeparator();
        JMenuItem quit = item("Beenden", null);
        quit.addActionListener(e -> dispose());
        file.add(quit);
        mb.add(file);

        JMenu edit = new JMenu("Bearbeiten");
        miUndo = trigger(item("Rueckgaengig", KeyStroke.getKeyStroke(KeyEvent.VK_Z, mod)),
                CreatorController.CMD_UNDO);
        miRedo = trigger(item("Wiederholen",
                        KeyStroke.getKeyStroke(KeyEvent.VK_Z, mod | InputEvent.SHIFT_DOWN_MASK)),
                CreatorController.CMD_REDO);
        edit.add(miUndo);
        edit.add(miRedo);
        edit.addSeparator();
        edit.add(trigger(item("Vorlage neu erzeugen", KeyStroke.getKeyStroke(KeyEvent.VK_R, mod)),
                CreatorController.CMD_REGENERATE));
        mb.add(edit);

        JMenu help = new JMenu("Hilfe");
        JMenuItem about = item("Ueber...", null);
        about.addActionListener(e -> showAbout());
        help.add(about);
        mb.add(help);
        return mb;
    }

    private JComponent buildToolbar() {
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        tb.add(trigger(new JButton("Bild oeffnen"), CreatorController.CMD_OPEN_IMAGE));
        tb.add(trigger(new JButton("Speichern"), CreatorController.CMD_SAVE));
        tb.add(trigger(new JButton("Oeffnen"), CreatorController.CMD_OPEN_PROJECT));
        tb.addSeparator();
        tb.add(trigger(new JButton("Rueckgaengig"), CreatorController.CMD_UNDO));
        tb.add(trigger(new JButton("Wiederholen"), CreatorController.CMD_REDO));
        tb.addSeparator();
        tb.add(trigger(new JButton("Neu erzeugen"), CreatorController.CMD_REGENERATE));
        tb.addSeparator();
        tb.add(button("PDF...", this::exportPdf));
        tb.add(button("PNG...", this::exportPng));
        tb.add(button("Drucken...", this::printTemplate));
        tb.add(Box.createHorizontalGlue());
        return tb;
    }

    private JComponent buildStatusBar() {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        busy.setIndeterminate(false);
        busy.setStringPainted(false);
        busy.setPreferredSize(new Dimension(120, 16));
        busy.setVisible(false);
        p.add(status, BorderLayout.CENTER);
        p.add(busy, BorderLayout.EAST);
        return p;
    }

    /* -------- Verdrahtung der Controller-Trigger -------- */

    /** Setzt Action-Command und merkt das Widget für {@link #setController}. */
    private <T extends AbstractButton> T trigger(T button, String command) {
        button.setActionCommand(command);
        controllerTriggers.add(button);
        return button;
    }

    private JMenuItem item(String text, KeyStroke key) {
        JMenuItem mi = new JMenuItem(text);
        if (key != null) mi.setAccelerator(key);
        return mi;
    }

    private JButton button(String text, Runnable r) {
        JButton b = new JButton(text);
        b.addActionListener(e -> r.run());
        return b;
    }

    /* ===================== Aktionen außerhalb des Use Case ===================== */
    /* Export/Druck gehören laut Aufgabenstellung NICHT zum Use Case „Vorlage    */
    /* erstellen". Sie holen die fertige Vorlage lesend über die Fassade ab.     */

    private void exportPng() {
        if (!ensureTemplate()) return;
        JFileChooser ch = new JFileChooser();
        ch.setDialogTitle("Zielordner waehlen");
        ch.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (ch.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File dir = ch.getSelectedFile();
        try {
            List<File> files = PngExporter.export(
                    creator.getTemplate(), creator.getParameters(), dir, baseName(), 200.0);
            status.setText("PNG exportiert: " + files.size() + " Datei(en) in " + dir);
        } catch (Exception ex) {
            showError("PNG-Export fehlgeschlagen", ex);
        }
    }

    private void exportPdf() {
        if (!ensureTemplate()) return;
        JFileChooser ch = new JFileChooser();
        ch.setFileFilter(new FileNameExtensionFilter("PDF-Dokument (*.pdf)", "pdf"));
        if (ch.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = ch.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".pdf")) {
            f = new File(f.getParentFile(), f.getName() + ".pdf");
        }
        try {
            PdfExporter.export(creator.getTemplate(), creator.getParameters(), f, 200.0);
            status.setText("PDF gespeichert: " + f.getName());
        } catch (Exception ex) {
            showError("PDF-Export fehlgeschlagen", ex);
        }
    }

    private void printTemplate() {
        if (!ensureTemplate()) return;
        try {
            new PrintAdapter(creator.getTemplate(), creator.getParameters()).printWithDialog();
        } catch (Exception ex) {
            showError("Drucken fehlgeschlagen", ex);
        }
    }

    private void savePartsList() {
        if (!ensureTemplate()) return;
        JFileChooser ch = new JFileChooser();
        ch.setSelectedFile(new File(baseName() + "_bestueckungsliste.txt"));
        if (ch.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            PartsListGenerator.writeTextFile(creator.getTemplate(), ch.getSelectedFile());
            status.setText("Bestueckungsliste gespeichert.");
        } catch (Exception ex) {
            showError("Speichern fehlgeschlagen", ex);
        }
    }

    private void saveBuildInstructions() {
        if (!ensureTemplate()) return;
        JFileChooser ch = new JFileChooser();
        ch.setSelectedFile(new File(baseName() + "_bauanleitung.txt"));
        if (ch.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            BuildInstructionsGenerator.writeTextFile(
                    creator.getTemplate(), creator.getParameters(), ch.getSelectedFile());
            status.setText("Bauanleitung gespeichert.");
        } catch (Exception ex) {
            showError("Speichern fehlgeschlagen", ex);
        }
    }

    private boolean ensureTemplate() {
        if (creator.getTemplate() == null) {
            JOptionPane.showMessageDialog(this,
                    "Bitte zuerst ein Bild laden und die Vorlage erzeugen.",
                    "Hinweis", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    private String baseName() {
        File f = (creator.getProjectFile() != null) ? creator.getProjectFile() : creator.getSourceFile();
        if (f != null) {
            String n = f.getName();
            int dot = n.lastIndexOf('.');
            return dot > 0 ? n.substring(0, dot) : n;
        }
        return "Vorlage";
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
                "3D-Art-Creator 1.0.0\n\n" +
                "Erzeugt aus Bildern druckbare Vorlagen fuer 3D-Pointillismus-\n" +
                "Bilder im Stil von Maxim Wakultschik oder StainsAndGrains.\n\n" +
                "Reine Java-Desktop-Anwendung, keine externen Abhaengigkeiten.",
                "Ueber",
                JOptionPane.INFORMATION_MESSAGE);
    }
}
