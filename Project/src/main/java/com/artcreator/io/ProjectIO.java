package com.artcreator.io;

import com.artcreator.model.ColorPalette;
import com.artcreator.model.GridLayout;
import com.artcreator.model.PaperSize;
import com.artcreator.model.Parameters;
import com.artcreator.model.Project;
import com.artcreator.model.QuantizationMethod;
import com.artcreator.model.StickShape;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Reads / writes ".3dart" project files.
 *
 * <p>A .3dart file is a plain ZIP archive containing:</p>
 * <ul>
 *   <li>{@code manifest.properties} - all parameters in
 *       {@link Properties} format (UTF-8).</li>
 *   <li>{@code original.png} - a lossless PNG copy of the original
 *       source image, so the project is portable across machines.</li>
 *   <li>{@code palette.txt} - the locked palette, one line per entry,
 *       formatted as {@code code|name|r|g|b}.</li>
 * </ul>
 *
 * <p>Only the JDK's built-in zip and ImageIO classes are used.</p>
 */
public final class ProjectIO {

    public static final String EXTENSION = "3dart";

    private ProjectIO() { /* static */ }

    /* =================================================================
       SAVE
       ================================================================= */

    public static void save(Project project, File file) throws IOException {
        if (project.getOriginal() == null) {
            throw new IOException("Project has no source image");
        }
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file))) {
            // manifest
            Properties p = parametersToProps(project.getParameters());
            if (project.getSourceFile() != null) {
                p.setProperty("project.sourceFilePath", project.getSourceFile().getAbsolutePath());
            }
            zos.putNextEntry(new ZipEntry("manifest.properties"));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            p.store(baos, "3D-Art-Creator project");
            zos.write(baos.toByteArray());
            zos.closeEntry();

            // palette
            zos.putNextEntry(new ZipEntry("palette.txt"));
            zos.write(paletteToText(project.getParameters().getLockedPalette())
                    .getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // original image as PNG
            zos.putNextEntry(new ZipEntry("original.png"));
            ImageIO.write(project.getOriginal(), "png", zos);
            zos.closeEntry();
        }
        project.setProjectFile(file);
        project.markClean();
    }

    /* =================================================================
       LOAD
       ================================================================= */

    public static Project load(File file) throws IOException {
        Project project = new Project();
        Properties props = null;
        BufferedImage original = null;
        ColorPalette palette = null;

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(file))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                byte[] data = readAll(zis);
                switch (name) {
                    case "manifest.properties":
                        props = new Properties();
                        props.load(new ByteArrayInputStream(data));
                        break;
                    case "original.png":
                        original = ImageIO.read(new ByteArrayInputStream(data));
                        break;
                    case "palette.txt":
                        palette = paletteFromText(new String(data, StandardCharsets.UTF_8));
                        break;
                    default:
                        // ignore unknown entries for forward-compat
                }
            }
        }
        if (props == null) throw new IOException("Missing manifest.properties");
        if (original == null) throw new IOException("Missing original.png");

        Parameters params = paramsFromProps(props);
        if (palette != null) params.setLockedPalette(palette);
        project.setParameters(params);
        project.setOriginal(original);
        project.setProjectFile(file);
        String src = props.getProperty("project.sourceFilePath");
        if (src != null && !src.isBlank()) {
            project.setSourceFile(new File(src));
        }
        project.markClean();
        return project;
    }

    /* =================================================================
       Properties <-> Parameters
       ================================================================= */

    private static Properties parametersToProps(Parameters p) {
        Properties pr = new Properties();
        pr.setProperty("paperSize",        p.getPaperSize().name());
        pr.setProperty("orientation",      p.getOrientation().name());
        pr.setProperty("customWidthMm",    Integer.toString(p.getCustomWidthMm()));
        pr.setProperty("customHeightMm",   Integer.toString(p.getCustomHeightMm()));
        pr.setProperty("printerMarginMm",  Integer.toString(p.getPrinterMarginMm()));
        pr.setProperty("tileOverlapMm",    Integer.toString(p.getTileOverlapMm()));

        pr.setProperty("stickDiameterMm",  Double.toString(p.getStickDiameterMm()));
        pr.setProperty("stickSpacingMm",   Double.toString(p.getStickSpacingMm()));
        pr.setProperty("stickShape",       p.getStickShape().name());
        pr.setProperty("gridLayout",       p.getGridLayout().name());

        pr.setProperty("quantizationMethod", p.getQuantizationMethod().name());
        pr.setProperty("paletteSize",        Integer.toString(p.getPaletteSize()));
        pr.setProperty("lockToPalette",      Boolean.toString(p.isLockToPalette()));
        pr.setProperty("dithering",          Boolean.toString(p.isDithering()));
        pr.setProperty("ditherStrength",     Double.toString(p.getDitherStrength()));

        pr.setProperty("brightness",       Double.toString(p.getBrightness()));
        pr.setProperty("contrast",         Double.toString(p.getContrast()));
        pr.setProperty("gamma",            Double.toString(p.getGamma()));
        pr.setProperty("saturation",       Double.toString(p.getSaturation()));
        pr.setProperty("histogramEqualization",
                                           Boolean.toString(p.isHistogramEqualization()));
        pr.setProperty("edgeBoost",        Double.toString(p.getEdgeBoost()));

        pr.setProperty("showColorCodes",   Boolean.toString(p.isShowColorCodes()));
        pr.setProperty("showGrid",         Boolean.toString(p.isShowGrid()));
        pr.setProperty("showCropMarks",    Boolean.toString(p.isShowCropMarks()));
        return pr;
    }

    private static Parameters paramsFromProps(Properties pr) {
        Parameters p = new Parameters();
        p.setPaperSize(  enumOr(pr, "paperSize",   PaperSize.class,        p.getPaperSize()));
        p.setOrientation(enumOr(pr, "orientation", PaperSize.Orientation.class, p.getOrientation()));
        p.setCustomWidthMm( intOr(pr, "customWidthMm",  p.getCustomWidthMm()));
        p.setCustomHeightMm(intOr(pr, "customHeightMm", p.getCustomHeightMm()));
        p.setPrinterMarginMm(intOr(pr, "printerMarginMm", p.getPrinterMarginMm()));
        p.setTileOverlapMm(intOr(pr, "tileOverlapMm",   p.getTileOverlapMm()));

        p.setStickDiameterMm(dblOr(pr, "stickDiameterMm", p.getStickDiameterMm()));
        p.setStickSpacingMm( dblOr(pr, "stickSpacingMm",  p.getStickSpacingMm()));
        p.setStickShape(  enumOr(pr, "stickShape", StickShape.class, p.getStickShape()));
        p.setGridLayout(  enumOr(pr, "gridLayout", GridLayout.class, p.getGridLayout()));

        p.setQuantizationMethod(enumOr(pr, "quantizationMethod", QuantizationMethod.class,
                                       p.getQuantizationMethod()));
        p.setPaletteSize( intOr(pr, "paletteSize", p.getPaletteSize()));
        p.setLockToPalette(boolOr(pr, "lockToPalette", p.isLockToPalette()));
        p.setDithering(   boolOr(pr, "dithering",     p.isDithering()));
        p.setDitherStrength(dblOr(pr, "ditherStrength", p.getDitherStrength()));

        p.setBrightness(dblOr(pr, "brightness",  p.getBrightness()));
        p.setContrast(  dblOr(pr, "contrast",    p.getContrast()));
        p.setGamma(     dblOr(pr, "gamma",       p.getGamma()));
        p.setSaturation(dblOr(pr, "saturation",  p.getSaturation()));
        p.setHistogramEqualization(boolOr(pr, "histogramEqualization",
                                          p.isHistogramEqualization()));
        p.setEdgeBoost( dblOr(pr, "edgeBoost",   p.getEdgeBoost()));

        p.setShowColorCodes(boolOr(pr, "showColorCodes", p.isShowColorCodes()));
        p.setShowGrid(      boolOr(pr, "showGrid",       p.isShowGrid()));
        p.setShowCropMarks( boolOr(pr, "showCropMarks",  p.isShowCropMarks()));
        return p;
    }

    /* =================================================================
       Palette serialisation
       ================================================================= */

    private static String paletteToText(ColorPalette palette) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 3D-Art-Creator palette\n");
        sb.append("name=").append(palette.getName()).append('\n');
        for (ColorPalette.Entry e : palette.entries()) {
            sb.append(e.code).append('|').append(e.name).append('|')
              .append(e.r).append('|').append(e.g).append('|').append(e.b).append('\n');
        }
        return sb.toString();
    }

    private static ColorPalette paletteFromText(String text) {
        String name = "Custom";
        List<ColorPalette.Entry> entries = new ArrayList<>();
        for (String raw : text.split("\\R")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            if (line.startsWith("name=")) {
                name = line.substring(5);
                continue;
            }
            String[] parts = line.split("\\|");
            if (parts.length != 5) continue;
            try {
                String code = parts[0];
                String n    = parts[1];
                int r = Integer.parseInt(parts[2]);
                int g = Integer.parseInt(parts[3]);
                int b = Integer.parseInt(parts[4]);
                entries.add(new ColorPalette.Entry(code, n, r, g, b));
            } catch (NumberFormatException ignored) { /* skip */ }
        }
        if (entries.isEmpty()) return ColorPalette.DEFAULT_24;
        return new ColorPalette(name, entries);
    }

    /* =================================================================
       small helpers
       ================================================================= */

    private static int intOr(Properties pr, String key, int dft) {
        try { return Integer.parseInt(pr.getProperty(key, "")); }
        catch (NumberFormatException e) { return dft; }
    }
    private static double dblOr(Properties pr, String key, double dft) {
        try { return Double.parseDouble(pr.getProperty(key, "")); }
        catch (NumberFormatException e) { return dft; }
    }
    private static boolean boolOr(Properties pr, String key, boolean dft) {
        String v = pr.getProperty(key);
        return v == null ? dft : Boolean.parseBoolean(v);
    }
    @SuppressWarnings({"unchecked","rawtypes"})
    private static <E extends Enum<E>> E enumOr(Properties pr, String key, Class<E> cls, E dft) {
        String v = pr.getProperty(key);
        if (v == null) return dft;
        try { return (E) Enum.valueOf((Class) cls, v); }
        catch (IllegalArgumentException e) { return dft; }
    }

    private static byte[] readAll(java.io.InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) >= 0) baos.write(buf, 0, n);
        return baos.toByteArray();
    }
}
