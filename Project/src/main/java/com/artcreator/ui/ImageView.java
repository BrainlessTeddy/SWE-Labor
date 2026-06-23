package com.artcreator.ui;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;

/**
 * A simple, fast image viewport that auto-fits its image into the available
 * space while preserving aspect ratio. Used by {@link ComparisonPanel} on
 * both sides of the split.
 */
@SuppressWarnings("serial")
public final class ImageView extends JComponent {
    private static final double MIN_ZOOM = 0.64;   // can't zoom out further than "fit"
    private static final double MAX_ZOOM = 20.0;
    private static final double WHEEL_STEP = 1.25; // zoom multiplier per wheel notch

    private BufferedImage image;
    private String emptyMessage = "(no image loaded)";
    
    /** Zoom relative to "fit to window" (1.0 = fitted). */
    private double zoom = 1.0;
    /** Pan offset, in image pixels, applied around the image centre. */
    private double panXImg = 0.0;
    private double panYImg = 0.0;
    
    /** Drag bookkeeping (screen-space). */
    private int dragStartX, dragStartY;
    private double dragStartPanX, dragStartPanY;
    private boolean dragging = false;

    public ImageView() {
        setBackground(new Color(255, 255, 255));
        setOpaque(true);
        setPreferredSize(new Dimension(400, 300));
        installInteractions();
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

    public void resetView() {
        this.zoom = 1.0;
        this.panXImg = 0.0;
        this.panYImg = 0.0;
        repaint();
    }
    
    private void installInteractions() {
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (image == null) return;
                if (e.getClickCount() >= 2) {
                    resetView();
                    return;
                }
                dragging = true;
                dragStartX = e.getX();
                dragStartY = e.getY();
                dragStartPanX = panXImg;
                dragStartPanY = panYImg;
                setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            }
 
            @Override public void mouseReleased(MouseEvent e) {
                dragging = false;
                setCursor(Cursor.getDefaultCursor());
            }
 
            @Override public void mouseExited(MouseEvent e) {
                if (!dragging) setCursor(Cursor.getDefaultCursor());
            }
        };
        addMouseListener(mouse);
 
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                if (!dragging || image == null) return;
                double scale = currentScale();
                if (scale <= 0) return;
                // Convert screen-pixel drag delta into image-pixel delta.
                double dxImg = (e.getX() - dragStartX) / scale;
                double dyImg = (e.getY() - dragStartY) / scale;
                panXImg = dragStartPanX - dxImg;
                panYImg = dragStartPanY - dyImg;
                repaint();
            }
        });
 
        addMouseWheelListener(new MouseWheelListener() {
            @Override public void mouseWheelMoved(MouseWheelEvent e) {
                if (image == null) return;
                double oldZoom = zoom;
                double factor = Math.pow(WHEEL_STEP, -e.getPreciseWheelRotation());
                double newZoom = clamp(oldZoom * factor, MIN_ZOOM, MAX_ZOOM);
                if (newZoom == oldZoom) return;
 
                // Keep the point under the cursor stationary while zooming:
                // find which image pixel is currently under the cursor, then
                // adjust the pan so that same pixel stays under the cursor
                // after the zoom changes.
                double fit = fitScale();
                double oldScale = fit * oldZoom;
                double newScale = fit * newZoom;
 
                int w = getWidth(), h = getHeight();
                double cxScreen = e.getX() - w / 2.0;
                double cyScreen = e.getY() - h / 2.0;
 
                double imgPxUnderCursorX = cxScreen / oldScale + panXImg;
                double imgPxUnderCursorY = cyScreen / oldScale + panYImg;
 
                zoom = newZoom;
                panXImg = imgPxUnderCursorX - cxScreen / newScale;
                panYImg = imgPxUnderCursorY - cyScreen / newScale;
                repaint();
            }
        });
    }
 
    private double fitScale() {
        if (image == null) return 1.0;
        int iw = image.getWidth(), ih = image.getHeight();
        int w = getWidth(), h = getHeight();
        if (iw <= 0 || ih <= 0 || w <= 0 || h <= 0) return 1.0;
        return Math.min((double) w / iw, (double) h / ih);
    }
 
    private double currentScale() {
        return fitScale() * zoom;
    }
 
    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
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

                g2.drawString(
                    emptyMessage,
                    (int) (w / 2.0 - b.getWidth() / 2.0),
                    (int) (h / 2.0 + b.getHeight() / 2.0)
                );

                return;
            }

            int iw = image.getWidth(), ih = image.getHeight();

            double fitScale = Math.min((double) w / iw, (double) h / ih);
            double scale = fitScale * zoom;

            int dw = (int) Math.round(iw * scale);
            int dh = (int) Math.round(ih * scale);

            double centerScreenX = w / 2.0 - panXImg * scale;
            double centerScreenY = h / 2.0 - panYImg * scale;

            int dx = (int) Math.round(centerScreenX - dw / 2.0);
            int dy = (int) Math.round(centerScreenY - dh / 2.0);

            g2.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR
            );

            g2.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY
            );

            g2.drawImage(image, dx, dy, dw, dh, null);
            g2.setColor(new Color(180, 180, 180));
            g2.drawRect(dx, dy, dw, dh);
        } finally {
            g2.dispose();
        }
    }
}
