package com.artcreator.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.image.BufferedImage;

/**
 * Side-by-side view: original image on the left, generated template on the
 * right, separated by a draggable splitter.
 */
@SuppressWarnings("serial")
public final class ComparisonPanel extends JPanel {

    private final ImageView left  = new ImageView();
    private final ImageView right = new ImageView();
    private final JLabel leftLabel  = new JLabel("Original",  SwingConstants.CENTER);
    private final JLabel rightLabel = new JLabel("Vorlage",   SwingConstants.CENTER);

    public ComparisonPanel() {
        super(new BorderLayout());
        Font lblFont = leftLabel.getFont().deriveFont(Font.BOLD);
        leftLabel.setFont(lblFont);
        rightLabel.setFont(lblFont);
        leftLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        rightLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        left.setEmptyMessage("Kein Bild geladen");
        right.setEmptyMessage("Noch keine Vorlage erzeugt");

        JPanel l = new JPanel(new BorderLayout());
        l.add(leftLabel, BorderLayout.NORTH);
        l.add(left, BorderLayout.CENTER);

        JPanel r = new JPanel(new BorderLayout());
        r.add(rightLabel, BorderLayout.NORTH);
        r.add(right, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, l, r);
        split.setResizeWeight(0.5);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);
    }

    public void setOriginal(BufferedImage img) { left.setImage(img); }
    public void setTemplatePreview(BufferedImage img) { right.setImage(img); }

    public void setRightStatus(String text) {
        if (text == null || text.isBlank()) rightLabel.setText("Vorlage");
        else                                rightLabel.setText("Vorlage -- " + text);
    }
}
