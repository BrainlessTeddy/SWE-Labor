package com.artcreator.output;

import com.artcreator.model.Parameters;
import com.artcreator.model.Template;
import com.artcreator.template.TemplateRenderer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;

/**
 * Renders a single {@link PageLayout.Tile} onto a {@link Graphics2D} that has
 * its user-space units set to millimetres (caller's responsibility, but the
 * convenience method {@link #renderTileToImage} sets it up for you).
 *
 * <p>What ends up on a tile, top to bottom:</p>
 * <ul>
 *   <li>printable rectangle outline (faint grey)</li>
 *   <li>the slice of the template that lies inside this tile</li>
 *   <li>crop marks at the four corners of the printable area</li>
 *   <li>registration crosses near the corners</li>
 *   <li>tile label "Rx Cy (N/M)" in the bottom-right</li>
 * </ul>
 */
public final class PageRenderer {

    private PageRenderer() { /* static */ }

    /** Render to a fresh raster at {@code dpi} dots per inch. */
    public static java.awt.image.BufferedImage renderTileToImage(Template tmpl,
                                                                 Parameters params,
                                                                 PageLayout.Tile tile,
                                                                 double dpi) {
        double mmPerInch = 25.4;
        int wPx = (int) Math.ceil(tile.paperWmm / mmPerInch * dpi);
        int hPx = (int) Math.ceil(tile.paperHmm / mmPerInch * dpi);
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                wPx, hPx, java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, wPx, hPx);
            // pixels per millimetre
            double scale = dpi / mmPerInch;
            g.scale(scale, scale);
            renderTile(g, tmpl, params, tile);
        } finally {
            g.dispose();
        }
        return img;
    }

    /** Render to {@code g}; assumes user-space units are millimetres. */
    public static void renderTile(Graphics2D g, Template tmpl, Parameters params,
                                  PageLayout.Tile tile) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                           RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        double m = tile.marginMm;
        double pw = tile.paperWmm;
        double ph = tile.paperHmm;
        double bw = tile.boardWmm;
        double bh = tile.boardHmm;

        // Printable area outline
        if (params.isShowGrid()) {
            g.setColor(new Color(230, 230, 230));
            g.setStroke(new BasicStroke(0.15f));
            g.draw(new Rectangle2D.Double(m, m, pw - 2*m, ph - 2*m));
        }

        // Render the tile's slice of the template.
        // Translate so that point (boardXmm, boardYmm) of the board sits at
        // (m, m) on the page; then clip the actual tile content rectangle.
        java.awt.Shape oldClip = g.getClip();
        g.clipRect((int) Math.floor(m), (int) Math.floor(m),
                   (int) Math.ceil(bw), (int) Math.ceil(bh));
        TemplateRenderer.render(g, tmpl,
                m - tile.boardXmm, m - tile.boardYmm,
                m, m,
                bw, bh,
                params.isShowColorCodes(),
                params.isShowGrid());
        g.setClip(oldClip);

        // Crop marks
        if (params.isShowCropMarks()) {
            drawCropMarks(g, m, ph - m, pw - m, m);
        }

        // Registration marks at corners (helps align tiles)
        drawRegistrationMarks(g, m, m, pw - m, ph - m);

        // Label in bottom-right corner of the printable area
        g.setColor(Color.BLACK);
        Font f = new Font(Font.SANS_SERIF, Font.BOLD, 1).deriveFont(4.0f);
        g.setFont(f);
        String label = tile.label();
        java.awt.font.FontRenderContext frc = g.getFontRenderContext();
        Rectangle2D r = g.getFont().getStringBounds(label, frc);
        float lx = (float) (pw - m - r.getWidth() - 2);
        float ly = (float) (ph - m - 2);
        g.drawString(label, lx, ly);
    }

    private static void drawCropMarks(Graphics2D g, double left, double bottom,
                                      double right, double top) {
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(0.2f));
        double len = 4.0; // mm
        // four corners
        double[][] corners = new double[][] {
                { left, top   },
                { right, top  },
                { left, bottom},
                { right, bottom}
        };
        for (double[] c : corners) {
            g.draw(new java.awt.geom.Line2D.Double(c[0] - len, c[1], c[0] + len, c[1]));
            g.draw(new java.awt.geom.Line2D.Double(c[0], c[1] - len, c[0], c[1] + len));
        }
    }

    private static void drawRegistrationMarks(Graphics2D g, double left, double top,
                                              double right, double bottom) {
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(0.15f));
        double r = 2.0;
        double[][] pts = {
                { left, top },
                { right, top },
                { left, bottom },
                { right, bottom }
        };
        for (double[] p : pts) {
            g.draw(new java.awt.geom.Ellipse2D.Double(p[0] - r, p[1] - r, 2*r, 2*r));
            g.draw(new java.awt.geom.Line2D.Double(p[0] - r - 1, p[1], p[0] + r + 1, p[1]));
            g.draw(new java.awt.geom.Line2D.Double(p[0], p[1] - r - 1, p[0], p[1] + r + 1));
        }
    }
}
