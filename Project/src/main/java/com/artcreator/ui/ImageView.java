package com.artcreator.ui;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * A simple, fast image viewport that auto-fits its image into the available
 * space while preserving aspect ratio. Used by {@link ComparisonPanel} on
 * both sides of the split.
 */
@SuppressWarnings("serial")
public final class ImageView extends JComponent {

    private BufferedImage image;
    private String emptyMessage = "(no image loaded)";

    public ImageView() {
        setBackground(new Color(245, 245, 245));
        setOpaque(true);
        setPreferredSize(new Dimension(400, 300));
    }

    public void setImage(BufferedImage img) {
        this.image = img;
        repaint();
    }

    public BufferedImage getImage() { return image; }

    public void setEmptyMessage(String msg) {
        this.emptyMessage = msg;
        repaint();
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            int w = getWidth(), h = getHeight();
            g2.setColor(getBackground());
            g2.fillRect(0, 0, w, h);
            if (image == null) {
                g2.setColor(Color.GRAY);
                java.awt.font.FontRenderContext frc = g2.getFontRenderContext();
                java.awt.geom.Rectangle2D b = g2.getFont().getStringBounds(emptyMessage, frc);
                g2.drawString(emptyMessage,
                        (int) (w / 2.0 - b.getWidth() / 2.0),
                        (int) (h / 2.0 + b.getHeight() / 2.0));
                return;
            }
            int iw = image.getWidth(), ih = image.getHeight();
            double scale = Math.min((double) w / iw, (double) h / ih);
            int dw = (int) Math.round(iw * scale);
            int dh = (int) Math.round(ih * scale);
            int dx = (w - dw) / 2;
            int dy = (h - dh) / 2;
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                                RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(image, dx, dy, dw, dh, null);
            g2.setColor(new Color(180, 180, 180));
            g2.drawRect(dx, dy, dw, dh);
        } finally {
            g2.dispose();
        }
    }
}
