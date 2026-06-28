package com.artcreator.output;

import com.artcreator.model.Parameters;
import com.artcreator.model.Template;

import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.List;

/**
 * Adapts the page layout to the {@code java.awt.print} API so the user can
 * print directly from the application without going through PDF.
 */
public final class PrintAdapter implements Pageable {

    private final Template template;
    private final Parameters params;
    private final List<PageLayout.Tile> tiles;
    private final PageFormat pageFormat;

    public PrintAdapter(Template template, Parameters params) {
        this.template = template;
        this.params = params;
        PageLayout layout = PageLayout.compute(template, params);
        this.tiles = layout.tiles();
        this.pageFormat = makePageFormat(layout);
    }

    @Override public int getNumberOfPages() { return tiles.size(); }
    @Override public PageFormat getPageFormat(int pageIndex) { return pageFormat; }
    @Override public Printable getPrintable(int pageIndex) {
        return (graphics, pf, idx) -> {
            if (idx < 0 || idx >= tiles.size()) return Printable.NO_SUCH_PAGE;
            Graphics2D g = (Graphics2D) graphics;
            // user-space units are 1/72 inch; convert to mm
            double scale = 72.0 / 25.4;
            g.scale(scale, scale);
            PageRenderer.renderTile(g, template, params, tiles.get(idx));
            return Printable.PAGE_EXISTS;
        };
    }

    /** Pop the system print dialog and (if accepted) print the template. */
    public boolean printWithDialog() throws PrinterException {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPageable(this);
        if (!job.printDialog()) return false;
        job.print();
        return true;
    }

    private static PageFormat makePageFormat(PageLayout layout) {
        PageFormat pf = new PageFormat();
        Paper paper = new Paper();
        double widthPt  = layout.tilePaperWidthMm()  * 72.0 / 25.4;
        double heightPt = layout.tilePaperHeightMm() * 72.0 / 25.4;
        paper.setSize(widthPt, heightPt);
        paper.setImageableArea(0, 0, widthPt, heightPt);
        pf.setPaper(paper);
        pf.setOrientation(widthPt > heightPt ? PageFormat.LANDSCAPE : PageFormat.PORTRAIT);
        return pf;
    }
}
