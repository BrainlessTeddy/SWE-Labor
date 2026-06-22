package com.artcreator.output;

import com.artcreator.model.Parameters;
import com.artcreator.model.Template;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Exports each tile of a {@link PageLayout} as a separate PNG file.
 *
 * <p>Files are named "&lt;basename&gt;_R&lt;row&gt;_C&lt;col&gt;.png" so they
 * can be sorted and assembled in the right order. Single-tile output drops
 * the suffix.</p>
 */
public final class PngExporter {

    private PngExporter() { /* static */ }

    public static List<File> export(Template tmpl,
                                    Parameters params,
                                    File outputDir,
                                    String basename,
                                    double dpi) throws IOException {
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Cannot create directory: " + outputDir);
        }
        PageLayout layout = PageLayout.compute(tmpl, params);
        java.util.ArrayList<File> written = new java.util.ArrayList<>();
        for (PageLayout.Tile t : layout.tiles()) {
            BufferedImage img = PageRenderer.renderTileToImage(tmpl, params, t, dpi);
            String filename = layout.isTiled()
                    ? String.format("%s_R%02d_C%02d.png", basename, t.row + 1, t.col + 1)
                    : basename + ".png";
            File f = new File(outputDir, filename);
            ImageIO.write(img, "png", f);
            written.add(f);
        }
        return written;
    }
}
