package com.artcreator.template;

import com.artcreator.model.ColorPalette;
import com.artcreator.model.GridLayout;
import com.artcreator.model.StickShape;
import com.artcreator.model.Template;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * Draws a {@link Template} onto a {@link Graphics2D} in millimetre coordinates.
 *
 * <p>The caller is responsible for setting up the appropriate transform so
 * that the graphics context's user-space units are millimetres. After the
 * call the transform is unchanged.</p>
 */
public final class TemplateRenderer {

    private TemplateRenderer() { /* static */ }

    /**
     * Render the template inside the rectangle (offsetXmm, offsetYmm,
     * boardWidth, boardHeight). Cells whose centres fall outside the
     * (clipXmm,...,clipWmm,clipHmm) clipping rectangle are skipped: that is
     * how page tiling works.
     */
    public static void render(Graphics2D g, Template tmpl,
                              double offsetXmm, double offsetYmm,
                              double clipXmm, double clipYmm,
                              double clipWmm, double clipHmm,
                              boolean showCodes, boolean showGrid) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);

        ColorPalette palette = tmpl.getPalette();
        double diameter = tmpl.getDiameterMm();

        if (showGrid) {
            g.setColor(new Color(220, 220, 220));
            g.setStroke(new BasicStroke(0.1f));
            // Faint board outline
            Rectangle2D r = new Rectangle2D.Double(offsetXmm, offsetYmm,
                                                   tmpl.getBoardWidthMm(),
                                                   tmpl.getBoardHeightMm());
            g.draw(r);
        }

        Font codeFont = new Font(Font.SANS_SERIF, Font.PLAIN, 1);
        codeFont = codeFont.deriveFont((float) (diameter * 0.45));
        g.setFont(codeFont);

        for (int row = 0; row < tmpl.getRows(); row++) {
            for (int col = 0; col < tmpl.getCols(); col++) {
                int idx = tmpl.indexAt(col, row);
                if (idx < 0) continue;
                ColorPalette.Entry e = palette.get(idx);
                double cx = offsetXmm + tmpl.centerXmm(col, row);
                double cy = offsetYmm + tmpl.centerYmm(col, row);

                // skip cells fully outside the clipping rect
                if (cx + diameter < clipXmm || cx - diameter > clipXmm + clipWmm) continue;
                if (cy + diameter < clipYmm || cy - diameter > clipYmm + clipHmm) continue;

                drawShape(g, tmpl.getShape(), cx, cy, diameter, e.awt());
                if (showCodes) {
                    drawCode(g, e.code, cx, cy, diameter);
                }
            }
        }
    }

    private static void drawShape(Graphics2D g, StickShape shape,
                                  double cx, double cy, double size,
                                  Color fill) {
        double r = size * 0.5;
        switch (shape) {
            case SQUARE: {
                Rectangle2D rect = new Rectangle2D.Double(cx - r, cy - r, size, size);
                g.setColor(fill);
                g.fill(rect);
                g.setColor(Color.BLACK);
                g.setStroke(new BasicStroke((float) (size * 0.04)));
                g.draw(rect);
                break;
            }
            case HEXAGON: {
                Path2D p = polygon(cx, cy, r, 6, Math.PI / 6.0);
                g.setColor(fill);
                g.fill(p);
                g.setColor(Color.BLACK);
                g.setStroke(new BasicStroke((float) (size * 0.04)));
                g.draw(p);
                break;
            }
            case TRIANGLE: {
                Path2D p = polygon(cx, cy, r, 3, -Math.PI / 2.0);
                g.setColor(fill);
                g.fill(p);
                g.setColor(Color.BLACK);
                g.setStroke(new BasicStroke((float) (size * 0.04)));
                g.draw(p);
                break;
            }
            case CIRCLE:
            default: {
                java.awt.geom.Ellipse2D.Double el =
                        new java.awt.geom.Ellipse2D.Double(cx - r, cy - r, size, size);
                g.setColor(fill);
                g.fill(el);
                g.setColor(Color.BLACK);
                g.setStroke(new BasicStroke((float) (size * 0.04)));
                g.draw(el);
                break;
            }
        }
    }

    private static Path2D polygon(double cx, double cy, double radius, int sides, double startAngle) {
        Path2D p = new Path2D.Double();
        for (int i = 0; i < sides; i++) {
            double a = startAngle + i * 2 * Math.PI / sides;
            double x = cx + radius * Math.cos(a);
            double y = cy + radius * Math.sin(a);
            if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        p.closePath();
        return p;
    }

    private static void drawCode(Graphics2D g, String code, double cx, double cy, double size) {
        // pick a label colour with decent contrast against the previously filled shape
        // by toggling between black and white based on a simple luminance test of the
        // current paint... Graphics2D doesn't expose that easily, so we cheat: use
        // a thin white outline + black fill, which is readable on any colour.
        java.awt.font.FontRenderContext frc = g.getFontRenderContext();
        java.awt.geom.Rectangle2D bounds = g.getFont().getStringBounds(code, frc);
        float tx = (float) (cx - bounds.getWidth() * 0.5);
        float ty = (float) (cy + bounds.getHeight() * 0.3);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke((float) (size * 0.02)));
        // simple 4-direction outline
        for (int[] off : new int[][]{{-1,0},{1,0},{0,-1},{0,1}}) {
            g.drawString(code, tx + off[0]*0.05f*(float)size, ty + off[1]*0.05f*(float)size);
        }
        g.setColor(Color.BLACK);
        g.drawString(code, tx, ty);
    }

    /** Convenience: render a preview to a fresh BufferedImage at the requested DPI. */
    public static java.awt.image.BufferedImage renderPreview(Template tmpl, double dpi,
                                                             boolean showCodes, boolean showGrid) {
        double mmPerInch = 25.4;
        int wPx = (int) Math.ceil(tmpl.getBoardWidthMm()  / mmPerInch * dpi);
        int hPx = (int) Math.ceil(tmpl.getBoardHeightMm() / mmPerInch * dpi);
        wPx = Math.max(64, wPx);
        hPx = Math.max(64, hPx);
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                wPx, hPx, java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, wPx, hPx);
            // scale: pixels per millimetre
            double scale = dpi / mmPerInch;
            g.scale(scale, scale);
            // ignore "layout grid lines" outline; render contents only
            int gridUsedSuppress = (tmpl.getLayout() == GridLayout.SQUARE) ? 1 : 0;
            @SuppressWarnings("unused") int x = gridUsedSuppress;
            render(g, tmpl,
                   0, 0,
                   0, 0,
                   tmpl.getBoardWidthMm(), tmpl.getBoardHeightMm(),
                   showCodes, showGrid);
        } finally {
            g.dispose();
        }
        return img;
    }
}
