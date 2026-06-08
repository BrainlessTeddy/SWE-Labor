package com.artcreator.ui;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

/** Tiny helpers to build labelled sliders mapped to doubles. */
public final class Sliders {

    private Sliders() { /* static */ }

    /**
     * Build a slider for a double value in {@code [min..max]}, exposed via
     * {@code getValue} / {@code onChange}. The slider has 1000 integer steps.
     */
    public static JPanel doubleSlider(String label,
                                      double min, double max,
                                      DoubleSupplier currentValue,
                                      Consumer<Double> onChange) {
        JPanel p = new JPanel(new BorderLayout(6, 0));
        JLabel l = new JLabel(label);
        l.setPreferredSize(new Dimension(110, 20));
        JLabel v = new JLabel();
        v.setPreferredSize(new Dimension(50, 20));
        JSlider slider = new JSlider(0, 1000);
        slider.setValue(toInt(currentValue.getAsDouble(), min, max));
        v.setText(formatDouble(currentValue.getAsDouble()));

        ChangeListener cl = e -> {
            double d = toDouble(slider.getValue(), min, max);
            v.setText(formatDouble(d));
            onChange.accept(d);
        };
        slider.addChangeListener(cl);
        p.add(l, BorderLayout.WEST);
        p.add(slider, BorderLayout.CENTER);
        p.add(v, BorderLayout.EAST);
        return p;
    }

    public static JPanel intSlider(String label, int min, int max,
                                   java.util.function.IntSupplier currentValue,
                                   java.util.function.IntConsumer onChange) {
        JPanel p = new JPanel(new BorderLayout(6, 0));
        JLabel l = new JLabel(label);
        l.setPreferredSize(new Dimension(110, 20));
        JLabel v = new JLabel();
        v.setPreferredSize(new Dimension(50, 20));
        JSlider slider = new JSlider(min, max, currentValue.getAsInt());
        v.setText(Integer.toString(slider.getValue()));
        slider.addChangeListener(e -> {
            v.setText(Integer.toString(slider.getValue()));
            onChange.accept(slider.getValue());
        });
        p.add(l, BorderLayout.WEST);
        p.add(slider, BorderLayout.CENTER);
        p.add(v, BorderLayout.EAST);
        return p;
    }

    private static int toInt(double d, double min, double max) {
        d = Math.max(min, Math.min(max, d));
        return (int) Math.round((d - min) / (max - min) * 1000.0);
    }

    private static double toDouble(int v, double min, double max) {
        return min + (v / 1000.0) * (max - min);
    }

    private static String formatDouble(double d) {
        if (Math.abs(d) >= 100) return String.format(java.util.Locale.US, "%.0f", d);
        if (Math.abs(d) >= 10)  return String.format(java.util.Locale.US, "%.1f", d);
        return String.format(java.util.Locale.US, "%.2f", d);
    }
}
