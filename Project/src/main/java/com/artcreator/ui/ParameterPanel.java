package com.artcreator.ui;

import com.artcreator.model.GridLayout;
import com.artcreator.model.PaperSize;
import com.artcreator.model.Parameters;
import com.artcreator.model.QuantizationMethod;
import com.artcreator.model.StickShape;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

/**
 * Right-hand-side panel containing every editable parameter, grouped into
 * logical sections. Each control wires directly into the underlying
 * Parameters object and notifies a change callback so the frame can trigger
 * a regeneration.
 */
@SuppressWarnings("serial")
public final class ParameterPanel extends JPanel {

    private final Parameters params;
    private final Runnable onChange;

    public ParameterPanel(Parameters params, Runnable onChange) {
        this.params = params;
        this.onChange = onChange;
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(340, 0));

        Box content = Box.createVerticalBox();
        content.add(buildPreprocessingGroup());
        content.add(Box.createVerticalStrut(6));
        content.add(buildPaletteGroup());
        content.add(Box.createVerticalStrut(6));
        content.add(buildStickGroup());
        content.add(Box.createVerticalStrut(6));
        content.add(buildPaperGroup());
        content.add(Box.createVerticalStrut(6));
        content.add(buildOverlayGroup());
        content.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel sectionPanel(String title) {
        JPanel p = new JPanel();
        p.setLayout(new javax.swing.BoxLayout(p, javax.swing.BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    private JPanel buildPreprocessingGroup() {
        JPanel p = sectionPanel("Bildanpassung");
        p.add(Sliders.doubleSlider("Helligkeit", -1.0, 1.0,
                params::getBrightness, v -> { params.setBrightness(v); fire(); }));
        p.add(Sliders.doubleSlider("Kontrast",   -1.0, 1.0,
                params::getContrast,   v -> { params.setContrast(v); fire(); }));
        p.add(Sliders.doubleSlider("Gamma",      0.2, 3.0,
                params::getGamma,      v -> { params.setGamma(v); fire(); }));
        p.add(Sliders.doubleSlider("Saettigung", 0.0, 2.0,
                params::getSaturation, v -> { params.setSaturation(v); fire(); }));
        p.add(Sliders.doubleSlider("Kantenboost", 0.0, 1.0,
                params::getEdgeBoost,  v -> { params.setEdgeBoost(v); fire(); }));
        JCheckBox eq = new JCheckBox("Histogramm ausgleichen", params.isHistogramEqualization());
        eq.addActionListener(e -> { params.setHistogramEqualization(eq.isSelected()); fire(); });
        p.add(eq);
        return p;
    }

    private JPanel buildPaletteGroup() {
        JPanel p = sectionPanel("Farben & Quantisierung");
        JComboBox<QuantizationMethod> qmCombo = new JComboBox<>(QuantizationMethod.values());
        qmCombo.setSelectedItem(params.getQuantizationMethod());
        qmCombo.addActionListener(e -> {
            params.setQuantizationMethod((QuantizationMethod) qmCombo.getSelectedItem());
            fire();
        });
        p.add(labelled("Methode", qmCombo));

        p.add(Sliders.intSlider("Palette-Groesse", 2, 64,
                params::getPaletteSize, v -> { params.setPaletteSize(v); fire(); }));

        JCheckBox lock = new JCheckBox("An feste Palette koppeln", params.isLockToPalette());
        lock.addActionListener(e -> { params.setLockToPalette(lock.isSelected()); fire(); });
        p.add(lock);

        JCheckBox dither = new JCheckBox("Floyd-Steinberg Dithering", params.isDithering());
        dither.addActionListener(e -> { params.setDithering(dither.isSelected()); fire(); });
        p.add(dither);

        p.add(Sliders.doubleSlider("Dither-Staerke", 0.0, 1.0,
                params::getDitherStrength, v -> { params.setDitherStrength(v); fire(); }));
        return p;
    }

    private JPanel buildStickGroup() {
        JPanel p = sectionPanel("Staebchen");
        p.add(Sliders.doubleSlider("Durchmesser (mm)", 0.5, 10.0,
                params::getStickDiameterMm,
                v -> { params.setStickDiameterMm(v); fire(); }));
        p.add(Sliders.doubleSlider("Abstand (mm)", 1.0, 15.0,
                params::getStickSpacingMm,
                v -> { params.setStickSpacingMm(v); fire(); }));

        JComboBox<StickShape> shape = new JComboBox<>(StickShape.values());
        shape.setSelectedItem(params.getStickShape());
        shape.addActionListener(e -> {
            params.setStickShape((StickShape) shape.getSelectedItem());
            fire();
        });
        p.add(labelled("Form", shape));

        JComboBox<GridLayout> layout = new JComboBox<>(GridLayout.values());
        layout.setSelectedItem(params.getGridLayout());
        layout.addActionListener(e -> {
            params.setGridLayout((GridLayout) layout.getSelectedItem());
            fire();
        });
        p.add(labelled("Anordnung", layout));
        return p;
    }

    private JPanel buildPaperGroup() {
        JPanel p = sectionPanel("Papier & Druck");

        JComboBox<PaperSize> ps = new JComboBox<>(PaperSize.values());
        ps.setSelectedItem(params.getPaperSize());
        ps.addActionListener(e -> {
            params.setPaperSize((PaperSize) ps.getSelectedItem());
            fire();
        });
        p.add(labelled("Format", ps));

        JComboBox<PaperSize.Orientation> ori = new JComboBox<>(PaperSize.Orientation.values());
        ori.setSelectedItem(params.getOrientation());
        ori.addActionListener(e -> {
            params.setOrientation((PaperSize.Orientation) ori.getSelectedItem());
            fire();
        });
        p.add(labelled("Orientierung", ori));

        JSpinner cw = new JSpinner(new SpinnerNumberModel(params.getCustomWidthMm(), 50, 2000, 10));
        cw.addChangeListener(e -> {
            params.setCustomWidthMm((Integer) cw.getValue());
            if (params.getPaperSize() == PaperSize.CUSTOM) fire();
        });
        p.add(labelled("Custom B (mm)", cw));

        JSpinner ch = new JSpinner(new SpinnerNumberModel(params.getCustomHeightMm(), 50, 2000, 10));
        ch.addChangeListener(e -> {
            params.setCustomHeightMm((Integer) ch.getValue());
            if (params.getPaperSize() == PaperSize.CUSTOM) fire();
        });
        p.add(labelled("Custom H (mm)", ch));

        JSpinner margin = new JSpinner(new SpinnerNumberModel(params.getPrinterMarginMm(), 0, 30, 1));
        margin.addChangeListener(e -> {
            params.setPrinterMarginMm((Integer) margin.getValue());
            fire();
        });
        p.add(labelled("Druckrand (mm)", margin));

        JSpinner overlap = new JSpinner(new SpinnerNumberModel(params.getTileOverlapMm(), 0, 30, 1));
        overlap.addChangeListener(e -> {
            params.setTileOverlapMm((Integer) overlap.getValue());
            fire();
        });
        p.add(labelled("Ueberlappung (mm)", overlap));
        return p;
    }

    private JPanel buildOverlayGroup() {
        JPanel p = sectionPanel("Anzeige");
        JCheckBox codes = new JCheckBox("Farbcodes anzeigen", params.isShowColorCodes());
        codes.addActionListener(e -> { params.setShowColorCodes(codes.isSelected()); fire(); });
        p.add(codes);
        JCheckBox grid = new JCheckBox("Raster anzeigen", params.isShowGrid());
        grid.addActionListener(e -> { params.setShowGrid(grid.isSelected()); fire(); });
        p.add(grid);
        JCheckBox crops = new JCheckBox("Schnittmarken anzeigen", params.isShowCropMarks());
        crops.addActionListener(e -> { params.setShowCropMarks(crops.isSelected()); fire(); });
        p.add(crops);
        return p;
    }

    private JPanel labelled(String text, Component editor) {
        JPanel p = new JPanel(new BorderLayout(6, 0));
        JLabel l = new JLabel(text);
        l.setPreferredSize(new Dimension(110, 22));
        p.add(l, BorderLayout.WEST);
        p.add(editor, BorderLayout.CENTER);
        return p;
    }

    private void fire() {
        if (onChange != null) onChange.run();
    }
}
