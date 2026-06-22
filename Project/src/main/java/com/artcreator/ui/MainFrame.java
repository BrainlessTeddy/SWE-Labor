package com.artcreator.ui;

import com.artcreator.io.ProjectIO;
import com.artcreator.model.Parameters;
import com.artcreator.model.Project;
import com.artcreator.output.BuildInstructionsGenerator;
import com.artcreator.output.PartsListGenerator;
import com.artcreator.output.PdfExporter;
import com.artcreator.output.PngExporter;
import com.artcreator.output.PrintAdapter;

import javax.imageio.ImageIO;
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
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Top-level Swing window. Owns the EditController and the visual widgets,
 * hooks up menus, toolbar and keyboard shortcuts.
 */
@SuppressWarnings("serial")
public final class MainFrame extends JFrame implements EditController.Listener {

    private final Project project = new Project();
    private final EditController controller = new EditController(project, this);
    private final ComparisonPanel comparison = new ComparisonPanel();
    private ParameterPanel parameters;
    private final JLabel status = new JLabel(" ");
    private final JProgressBar busy = new JProgressBar();
    private JSplitPane mainSplit;

    private JMenuItem miUndo, miRedo, miSave, miSaveAs, miExportPng, miExportPdf, miPrint,
            miPartsList, miBuildInstructions;

    public MainFrame() {
        super("3D-Art-Creator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(1400, screen.width - 100), Math.min(900, screen.height - 100));
        setLocationRelativeTo(null);

        parameters = new ParameterPanel(project.getParameters(), controller::parametersChanged);
        setJMenuBar(buildMenuBar());

        mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, comparison, parameters);
        mainSplit.setResizeWeight(1.0);
        mainSplit.setBorder(null);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(buildToolbar(), BorderLayout.NORTH);
        getContentPane().add(mainSplit, BorderLayout.CENTER);
        getContentPane().add(buildStatusBar(), BorderLayout.SOUTH);

        updateActionsEnabled();
    }

    /* -------- menus + toolbar -------- */

    private JMenuBar buildMenuBar() {
        int mod = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        JMenuBar mb = new JMenuBar();
        JMenu file = new JMenu("Datei");
        file.add(item("Bild oeffnen...", KeyStroke.getKeyStroke(KeyEvent.VK_O, mod), this::openImage));
        file.addSeparator();
        miSave   = item("Projekt speichern", KeyStroke.getKeyStroke(KeyEvent.VK_S, mod), this::saveProject);
        miSaveAs = item("Projekt speichern unter...", null, this::saveProjectAs);
        file.add(miSave);
        file.add(miSaveAs);
        file.add(item("Projekt oeffnen...",
                KeyStroke.getKeyStroke(KeyEvent.VK_O, mod | InputEvent.SHIFT_DOWN_MASK),
                this::openProject));
        file.addSeparator();
        miExportPng = item("PNG-Seiten exportieren...", null, this::exportPng);
        miExportPdf = item("PDF exportieren...",        null, this::exportPdf);
        miPrint     = item("Drucken...",
                KeyStroke.getKeyStroke(KeyEvent.VK_P, mod), this::printTemplate);
        file.add(miExportPng);
        file.add(miExportPdf);
        file.add(miPrint);
        file.addSeparator();
        miPartsList         = item("Bestueckungsliste speichern...", null, this::savePartsList);
        miBuildInstructions = item("Bauanleitung speichern...",     null, this::saveBuildInstructions);
        file.add(miPartsList);
        file.add(miBuildInstructions);
        file.addSeparator();
        file.add(item("Beenden", null, e -> dispose()));
        mb.add(file);

        JMenu edit = new JMenu("Bearbeiten");
        miUndo = item("Rueckgaengig",
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, mod), e -> controller.undo());
        miRedo = item("Wiederholen",
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, mod | InputEvent.SHIFT_DOWN_MASK),
                e -> controller.redo());
        edit.add(miUndo);
        edit.add(miRedo);
        edit.addSeparator();
        edit.add(item("Vorlage neu erzeugen",
                KeyStroke.getKeyStroke(KeyEvent.VK_R, mod),
                e -> controller.regenerateNow()));
        mb.add(edit);

        JMenu help = new JMenu("Hilfe");
        help.add(item("Ueber...", null, e -> showAbout()));
        mb.add(help);
        return mb;
    }

    private JComponent buildToolbar() {
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        tb.add(button("Bild oeffnen", this::openImage));
        tb.add(button("Speichern", this::saveProject));
        tb.add(button("Oeffnen", this::openProject));
        tb.addSeparator();
        tb.add(button("Rueckgaengig", e -> controller.undo()));
        tb.add(button("Wiederholen", e -> controller.redo()));
        tb.addSeparator();
        tb.add(button("Neu erzeugen", e -> controller.regenerateNow()));
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

    private JMenuItem item(String text, KeyStroke key, java.awt.event.ActionListener l) {
        JMenuItem mi = new JMenuItem(text);
        if (key != null) mi.setAccelerator(key);
        mi.addActionListener(l);
        return mi;
    }
    private JMenuItem item(String text, KeyStroke key, Runnable r) {
        return item(text, key, e -> r.run());
    }

    private JButton button(String text, java.awt.event.ActionListener l) {
        JButton b = new JButton(text);
        b.addActionListener(l);
        return b;
    }
    private JButton button(String text, Runnable r) { return button(text, e -> r.run()); }

    /* -------- actions -------- */

    private void openImage() {
        JFileChooser ch = new JFileChooser();
        ch.setFileFilter(new FileNameExtensionFilter(
                "Bilddateien (jpg, png, bmp, gif)", "jpg", "jpeg", "png", "bmp", "gif"));
        if (ch.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = ch.getSelectedFile();
        try {
            BufferedImage img = ImageIO.read(f);
            if (img == null) throw new java.io.IOException("Format nicht erkannt");
            project.setSourceFile(f);
            controller.setOriginalImage(img);
            comparison.setOriginal(img);
            status.setText("Bild geladen: " + f.getName());
            updateActionsEnabled();
        } catch (Exception ex) {
            error("Fehler beim Laden des Bildes", ex);
        }
    }

    private void saveProject() {
        if (project.getProjectFile() == null) { saveProjectAs(); return; }
        try {
            ProjectIO.save(project, project.getProjectFile());
            status.setText("Projekt gespeichert: " + project.getProjectFile().getName());
        } catch (Exception ex) {
            error("Speichern fehlgeschlagen", ex);
        }
    }

    private void saveProjectAs() {
        if (project.getOriginal() == null) {
            warn("Kein Bild geladen.");
            return;
        }
        JFileChooser ch = new JFileChooser();
        ch.setFileFilter(new FileNameExtensionFilter(
                "3D-Art Projektdatei (." + ProjectIO.EXTENSION + ")", ProjectIO.EXTENSION));
        if (ch.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = ch.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith("." + ProjectIO.EXTENSION)) {
            f = new File(f.getParentFile(), f.getName() + "." + ProjectIO.EXTENSION);
        }
        try {
            ProjectIO.save(project, f);
            project.setProjectFile(f);
            status.setText("Gespeichert: " + f.getName());
        } catch (Exception ex) {
            error("Speichern fehlgeschlagen", ex);
        }
    }

    private void openProject() {
        JFileChooser ch = new JFileChooser();
        ch.setFileFilter(new FileNameExtensionFilter(
                "3D-Art Projektdatei (." + ProjectIO.EXTENSION + ")", ProjectIO.EXTENSION));
        if (ch.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            Project loaded = ProjectIO.load(ch.getSelectedFile());
            project.setOriginal(loaded.getOriginal());
            project.setSourceFile(loaded.getSourceFile());
            project.setProjectFile(loaded.getProjectFile());
            copyParameters(loaded.getParameters(), project.getParameters());
            comparison.setOriginal(project.getOriginal());
            status.setText("Geladen: " + ch.getSelectedFile().getName());
            rebuildParameterPanel();
            controller.regenerateNow();
            updateActionsEnabled();
        } catch (Exception ex) {
            error("Laden fehlgeschlagen", ex);
        }
    }

    private void copyParameters(Parameters from, Parameters to) {
        to.setPaperSize(from.getPaperSize());
        to.setOrientation(from.getOrientation());
        to.setCustomWidthMm(from.getCustomWidthMm());
        to.setCustomHeightMm(from.getCustomHeightMm());
        to.setPrinterMarginMm(from.getPrinterMarginMm());
        to.setTileOverlapMm(from.getTileOverlapMm());
        to.setStickDiameterMm(from.getStickDiameterMm());
        to.setStickSpacingMm(from.getStickSpacingMm());
        to.setStickShape(from.getStickShape());
        to.setGridLayout(from.getGridLayout());
        to.setQuantizationMethod(from.getQuantizationMethod());
        to.setPaletteSize(from.getPaletteSize());
        to.setLockToPalette(from.isLockToPalette());
        to.setLockedPalette(from.getLockedPalette());
        to.setDithering(from.isDithering());
        to.setDitherStrength(from.getDitherStrength());
        to.setBrightness(from.getBrightness());
        to.setContrast(from.getContrast());
        to.setGamma(from.getGamma());
        to.setSaturation(from.getSaturation());
        to.setHistogramEqualization(from.isHistogramEqualization());
        to.setEdgeBoost(from.getEdgeBoost());
        to.setShowColorCodes(from.isShowColorCodes());
        to.setShowGrid(from.isShowGrid());
        to.setShowCropMarks(from.isShowCropMarks());
    }

    private void rebuildParameterPanel() {
        ParameterPanel fresh = new ParameterPanel(project.getParameters(),
                controller::parametersChanged);
        if (mainSplit != null) {
            mainSplit.setRightComponent(fresh);
            mainSplit.revalidate();
            mainSplit.repaint();
        }
        parameters = fresh;
    }

    private void exportPng() {
        if (!ensureTemplate()) return;
        JFileChooser ch = new JFileChooser();
        ch.setDialogTitle("Zielordner waehlen");
        ch.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (ch.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File dir = ch.getSelectedFile();
        try {
            java.util.List<File> files = PngExporter.export(
                    controller.getTemplateOrThrow(), project.getParameters(),
                    dir, baseName(), 200.0);
            status.setText("PNG exportiert: " + files.size() + " Datei(en) in " + dir);
        } catch (Exception ex) {
            error("PNG-Export fehlgeschlagen", ex);
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
            PdfExporter.export(controller.getTemplateOrThrow(), project.getParameters(),
                    f, 200.0);
            status.setText("PDF gespeichert: " + f.getName());
        } catch (Exception ex) {
            error("PDF-Export fehlgeschlagen", ex);
        }
    }

    private void printTemplate() {
        if (!ensureTemplate()) return;
        try {
            PrintAdapter adapter = new PrintAdapter(
                    controller.getTemplateOrThrow(), project.getParameters());
            adapter.printWithDialog();
        } catch (Exception ex) {
            error("Drucken fehlgeschlagen", ex);
        }
    }

    private void savePartsList() {
        if (!ensureTemplate()) return;
        JFileChooser ch = new JFileChooser();
        ch.setSelectedFile(new File(baseName() + "_bestueckungsliste.txt"));
        if (ch.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            PartsListGenerator.writeTextFile(controller.getTemplateOrThrow(),
                    ch.getSelectedFile());
            status.setText("Bestueckungsliste gespeichert.");
        } catch (Exception ex) {
            error("Speichern fehlgeschlagen", ex);
        }
    }

    private void saveBuildInstructions() {
        if (!ensureTemplate()) return;
        JFileChooser ch = new JFileChooser();
        ch.setSelectedFile(new File(baseName() + "_bauanleitung.txt"));
        if (ch.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            BuildInstructionsGenerator.writeTextFile(
                    controller.getTemplateOrThrow(), project.getParameters(),
                    ch.getSelectedFile());
            status.setText("Bauanleitung gespeichert.");
        } catch (Exception ex) {
            error("Speichern fehlgeschlagen", ex);
        }
    }

    private boolean ensureTemplate() {
        if (project.getTemplate() == null) {
            warn("Bitte zuerst ein Bild laden und die Vorlage erzeugen.");
            return false;
        }
        return true;
    }

    private String baseName() {
        if (project.getProjectFile() != null) {
            String n = project.getProjectFile().getName();
            int dot = n.lastIndexOf('.');
            return dot > 0 ? n.substring(0, dot) : n;
        }
        if (project.getSourceFile() != null) {
            String n = project.getSourceFile().getName();
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

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Hinweis", JOptionPane.WARNING_MESSAGE);
    }

    private void error(String msg, Throwable t) {
        t.printStackTrace();
        JOptionPane.showMessageDialog(this, msg + ":\n" + t.getMessage(),
                "Fehler", JOptionPane.ERROR_MESSAGE);
    }

    private void updateActionsEnabled() {
        boolean hasImg = project.hasImage();
        boolean hasTpl = project.hasTemplate();
        if (miSave != null)             miSave.setEnabled(hasImg);
        if (miSaveAs != null)           miSaveAs.setEnabled(hasImg);
        if (miExportPng != null)        miExportPng.setEnabled(hasTpl);
        if (miExportPdf != null)        miExportPdf.setEnabled(hasTpl);
        if (miPrint != null)            miPrint.setEnabled(hasTpl);
        if (miPartsList != null)        miPartsList.setEnabled(hasTpl);
        if (miBuildInstructions != null) miBuildInstructions.setEnabled(hasTpl);
        if (miUndo != null) miUndo.setEnabled(controller.getHistory().canUndo());
        if (miRedo != null) miRedo.setEnabled(controller.getHistory().canRedo());
    }

    /* -------- EditController.Listener -------- */

    @Override public void onProjectChanged(Project p) {
        SwingUtilities.invokeLater(this::updateActionsEnabled);
    }

    @Override public void onTemplateGenerated(Project p, BufferedImage preview) {
        SwingUtilities.invokeLater(() -> {
            comparison.setTemplatePreview(preview);
            int sticks = p.getTemplate().totalSticks();
            int cols = p.getTemplate().getCols();
            int rows = p.getTemplate().getRows();
            comparison.setRightStatus(cols + " x " + rows + " Sticks (" + sticks + ")");
            updateActionsEnabled();
        });
    }

    @Override public void onError(String message, Throwable cause) {
        SwingUtilities.invokeLater(() -> error(message, cause));
    }

    @Override public void onBusyStateChanged(boolean busyNow) {
        SwingUtilities.invokeLater(() -> {
            busy.setVisible(busyNow);
            busy.setIndeterminate(busyNow);
            if (busyNow) status.setText("Erzeuge Vorlage...");
        });
    }
}
