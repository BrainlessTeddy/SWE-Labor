package com.artcreator.output;

import com.artcreator.model.Parameters;
import com.artcreator.model.Template;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal in-house PDF emitter. Produces a multi-page PDF where each page
 * is filled with a single JPEG image (the rendered template tile).
 *
 * <p>This deliberately avoids any third-party PDF library while still
 * producing a fully spec-compliant PDF 1.4 file readable by every modern
 * viewer (Acrobat, Preview, Firefox, browsers, etc.).</p>
 *
 * <p>The structure is the simplest legal PDF: catalog -&gt; pages -&gt;
 * page(s) -&gt; image XObject + content stream that places the image to fill
 * the MediaBox.</p>
 */
public final class PdfExporter {

    private PdfExporter() { /* static */ }

    /** Export every tile of {@link PageLayout} into a single PDF file. */
    public static void export(Template tmpl,
                              Parameters params,
                              File pdfFile,
                              double dpi) throws IOException {
        PageLayout layout = PageLayout.compute(tmpl, params);
        List<TilePayload> payloads = new ArrayList<>();
        for (PageLayout.Tile t : layout.tiles()) {
            BufferedImage img = PageRenderer.renderTileToImage(tmpl, params, t, dpi);
            payloads.add(new TilePayload(t, encodeJpeg(img), img.getWidth(), img.getHeight()));
        }
        try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
            writePdf(fos, payloads);
        }
    }

    /** Convenience: write a single PNG-rendered image as a 1-page PDF. */
    public static void exportSinglePage(BufferedImage img, double widthPt, double heightPt,
                                        File pdfFile) throws IOException {
        TilePayload payload = new TilePayload(
                new PageLayout.Tile(0, 0, 1, 1, 0, 0, (int) widthPt, (int) heightPt,
                                    (int) Math.round(widthPt * 25.4 / 72.0),
                                    (int) Math.round(heightPt * 25.4 / 72.0), 0),
                encodeJpeg(img), img.getWidth(), img.getHeight());
        try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
            writePdf(fos, List.of(payload));
        }
    }

    /* ================================================================
       PDF writing
       ================================================================ */

    private static void writePdf(OutputStream out, List<TilePayload> payloads) throws IOException {
        OffsetTrackingStream os = new OffsetTrackingStream(out);
        os.writeAscii("%PDF-1.4\n");
        // binary marker (helps tools detect the file as binary)
        os.write(new byte[]{'%', (byte) 0xE2, (byte) 0xE3, (byte) 0xCF, (byte) 0xD3, '\n'});

        int totalObjects = 2 + 3 * payloads.size();   // catalog, pages, then 3 per page
        long[] offsets = new long[totalObjects + 1];  // 1-indexed

        // Object IDs:
        // 1 = Catalog
        // 2 = Pages
        // For page i (0-based): page = 3 + 3i, content = 4 + 3i, image = 5 + 3i

        // 1: Catalog
        offsets[1] = os.offset();
        os.writeAscii("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

        // 2: Pages
        offsets[2] = os.offset();
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < payloads.size(); i++) {
            int id = 3 + 3 * i;
            kids.append(id).append(" 0 R ");
        }
        os.writeAscii("2 0 obj\n<< /Type /Pages /Count " + payloads.size()
                + " /Kids [ " + kids + "] >>\nendobj\n");

        // 3+: per page
        for (int i = 0; i < payloads.size(); i++) {
            TilePayload p = payloads.get(i);
            int pageId = 3 + 3 * i;
            int contentId = 4 + 3 * i;
            int imageId = 5 + 3 * i;

            double widthPt  = p.tile.paperWmm * 72.0 / 25.4;
            double heightPt = p.tile.paperHmm * 72.0 / 25.4;

            // Page object
            offsets[pageId] = os.offset();
            os.writeAscii(pageId + " 0 obj\n"
                    + "<< /Type /Page /Parent 2 0 R "
                    + "/MediaBox [0 0 " + fmt(widthPt) + " " + fmt(heightPt) + "] "
                    + "/Resources << /XObject << /Im0 " + imageId + " 0 R >> "
                    + "/ProcSet [/PDF /ImageC] >> "
                    + "/Contents " + contentId + " 0 R >>\nendobj\n");

            // Content stream: place the image to fill MediaBox.
            String content =
                    "q\n" +
                    fmt(widthPt) + " 0 0 " + fmt(heightPt) + " 0 0 cm\n" +
                    "/Im0 Do\n" +
                    "Q\n";
            byte[] contentBytes = content.getBytes(StandardCharsets.US_ASCII);
            offsets[contentId] = os.offset();
            os.writeAscii(contentId + " 0 obj\n<< /Length " + contentBytes.length + " >>\nstream\n");
            os.write(contentBytes);
            os.writeAscii("\nendstream\nendobj\n");

            // Image XObject (JPEG via DCTDecode)
            offsets[imageId] = os.offset();
            os.writeAscii(imageId + " 0 obj\n"
                    + "<< /Type /XObject /Subtype /Image "
                    + "/Width " + p.widthPx + " /Height " + p.heightPx + " "
                    + "/ColorSpace /DeviceRGB /BitsPerComponent 8 "
                    + "/Filter /DCTDecode "
                    + "/Length " + p.jpegBytes.length + " >>\nstream\n");
            os.write(p.jpegBytes);
            os.writeAscii("\nendstream\nendobj\n");
        }

        // xref
        long xrefOffset = os.offset();
        os.writeAscii("xref\n");
        os.writeAscii("0 " + (totalObjects + 1) + "\n");
        os.writeAscii("0000000000 65535 f \n");
        for (int id = 1; id <= totalObjects; id++) {
            os.writeAscii(String.format("%010d 00000 n \n", offsets[id]));
        }

        // trailer
        os.writeAscii("trailer\n<< /Size " + (totalObjects + 1) + " /Root 1 0 R >>\n");
        os.writeAscii("startxref\n");
        os.writeAscii(xrefOffset + "\n");
        os.writeAscii("%%EOF\n");
    }

    private static String fmt(double v) {
        // 4 decimals is plenty for PDF coordinates
        return String.format(java.util.Locale.US, "%.4f", v);
    }

    private static byte[] encodeJpeg(BufferedImage img) throws IOException {
        // Make sure it's an RGB image (JPEG can't store an alpha channel reliably).
        BufferedImage rgb = img;
        if (img.getType() != BufferedImage.TYPE_INT_RGB
                && img.getType() != BufferedImage.TYPE_3BYTE_BGR) {
            rgb = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = rgb.createGraphics();
            try { g.drawImage(img, 0, 0, null); } finally { g.dispose(); }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream(64 * 1024);
        // Default JPEG quality from ImageIO is around 0.75, which is fine.
        if (!ImageIO.write(rgb, "jpg", baos)) {
            throw new IOException("No JPEG ImageWriter available in this JDK");
        }
        return baos.toByteArray();
    }

    /* ================================================================ */

    private static final class TilePayload {
        final PageLayout.Tile tile;
        final byte[] jpegBytes;
        final int widthPx;
        final int heightPx;
        TilePayload(PageLayout.Tile t, byte[] jpg, int w, int h) {
            this.tile = t; this.jpegBytes = jpg; this.widthPx = w; this.heightPx = h;
        }
    }

    private static final class OffsetTrackingStream extends OutputStream {
        private final OutputStream inner;
        private long offset = 0;
        OffsetTrackingStream(OutputStream inner) { this.inner = inner; }
        long offset() { return offset; }

        @Override public void write(int b) throws IOException {
            inner.write(b);
            offset++;
        }
        @Override public void write(byte[] buf, int off, int len) throws IOException {
            inner.write(buf, off, len);
            offset += len;
        }
        void writeAscii(String s) throws IOException {
            byte[] bs = s.getBytes(StandardCharsets.US_ASCII);
            write(bs, 0, bs.length);
        }
    }
}
