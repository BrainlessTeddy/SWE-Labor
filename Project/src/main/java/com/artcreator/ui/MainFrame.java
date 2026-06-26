package com.artcreator.ui;

import com.artcreator.creator.port.Creator;
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
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Top-level Swing-Fenster – im Sinne von MVC eine <b>reine View</b> für den
 * Use Case „Vorlage erstellen".
 *
 * <p>Die View enthält keine Geschäftslogik: Sie baut die Widgets auf, leitet
 * Use-Case-Aktionen über Action-Commands an den {@link CreatorController}
 * weiter und bietet Anzeige-Methoden ({@code setPreview}, {@code setBusy} …),
 * die der Controller nach Zustandsänderungen aufruft. Das Domänenmodell liegt
 * hinter der {@link Creator}-Fassade; die View greift nur lesend darauf zu, um
 * Ergebnisse anzuzeigen bzw. (für nicht zum Use Case gehörende Aktionen wie
 * Export/Druck) die fertige Vorlage abzuholen.</p>
 */
@SuppressWarnings("serial")
public final class MainFrame extends JFrame {

    private final Creator creator;

    /** Eingabepuffer der View für die Parameter (wird vom Controller transportiert). */
    private final Parameters parameterBuffer = new Parameters();

    private final ComparisonPanel comparison = new ComparisonPanel();
    private final ParameterPanel parameters;
    private final JLabel status = new JLabel(" ");
    private final JProgressBar busy = new JProgressBar();
    private final JSplitPane mainSplit;

    private JMenuItem miExportPng, miExportPdf, miPrint, miPartsList, miBuildInstructions;

    /** Widgets, deren Klick eine Use-Case-Operation auslöst (vom Controller verdrahtet). */
    private final List<AbstractButton> openImageTriggers = new ArrayList<>();
    private final List<AbstractButton> regenerateTriggers = new ArrayList<>();

    /** Wird vom Controller gesetzt; Default ist ein No-op. */
    private Runnable paramChangeHandler = () -> { };

    /** Zuletzt gewählte Bilddatei – nur für abgeleitete Export-Dateinamen. */
    private File lastImageFile;

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
    }

    /**
     * Verbindet die Use-Case-Aktionen der View mit dem Controller. Wird nach
     * der Konstruktion von außen aufgerufen (zweiphasiges Wiring, da Controller
     * und View sich gegenseitig kennen müssen).
     */
    public void setController(CreatorController controller) {
        for (AbstractButton b : openImageTriggers)  b.addActionListener(controller);
        for (AbstractButton b : regenerateTriggers) b.addActionListener(controller);
        this.paramChangeHandler = controller::onParametersChanged;
    }

    /* ============================ View-API ============================ */

    /** Öffnet einen Datei-Dialog zur Bildauswahl; {@code null} bei Abbruch. */
    public File chooseImageFile() {
        JFileChooser ch = new JFileChooser();
        ch.setFileFilter(new FileNameExtensionFilter(
                "Bilddateien (jpg, png, bmp, gif)", "jpg", "jpeg", "png", "bmp", "gif"));
        if (ch.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return null;
        this.lastImageFile = ch.getSelectedFile();
        return lastImageFile;
    }

    /** Aktueller Eingabepuffer der Parameter (vom Controller an die Fassade gereicht). */
    public Parameters getParameterBuffer() {
        return parameterBuffer;
    }

    public void setOriginalImage(BufferedImage img) {
        comparison.setOriginal(img);
        if (lastImageFile != null) status.setText("Bild geladen: " + lastImageFile.getName());
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
        if (miExportPng != null)         miExportPng.setEnabled(hasTemplate);
        if (miExportPdf != null)         miExportPdf.setEnabled(hasTemplate);
        if (miPrint != null)             miPrint.setEnabled(hasTemplate);
        if (miPartsList != null)         miPartsList.setEnabled(hasTemplate);
        if (miBuildInstructions != null) miBuildInstructions.setEnabled(hasTemplate);
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
        JMenuItem open = item("Bild oeffnen...", KeyStroke.getKeyStroke(KeyEvent.VK_O, mod));
        registerOpenImage(open);
        file.add(open);
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
        JMenuItem regen = item("Vorlage neu erzeugen", KeyStroke.getKeyStroke(KeyEvent.VK_R, mod));
        registerRegenerate(regen);
        edit.add(regen);
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
        JButton open = new JButton("Bild oeffnen");
        registerOpenImage(open);
        tb.add(open);
        tb.addSeparator();
        JButton regen = new JButton("Neu erzeugen");
        registerRegenerate(regen);
        tb.add(regen);
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

    /* -------- Verdrahtung der Use-Case-Trigger -------- */

    private void registerOpenImage(AbstractButton b) {
        b.setActionCommand(CreatorController.CMD_OPEN_IMAGE);
        openImageTriggers.add(b);
    }

    private void registerRegenerate(AbstractButton b) {
        b.setActionCommand(CreatorController.CMD_REGENERATE);
        regenerateTriggers.add(b);
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
        if (lastImageFile != null) {
            String n = lastImageFile.getName();
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
