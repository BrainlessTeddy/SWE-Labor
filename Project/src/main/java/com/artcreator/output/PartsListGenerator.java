package com.artcreator.output;

import com.artcreator.model.ColorPalette;
import com.artcreator.model.Template;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Computes a parts list (sticks per colour) and writes it as a plain text file.
 *
 * <p>Output is plain UTF-8 text laid out in columns, intentionally trivial to
 * print on a normal page or paste into a spreadsheet.</p>
 */
public final class PartsListGenerator {

    private PartsListGenerator() { /* static */ }

    /** Build the in-memory list, sorted by descending count. */
    public static List<Entry> build(Template tmpl) {
        ColorPalette palette = tmpl.getPalette();
        int[] counts = tmpl.countsByIndex();
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] == 0) continue;
            entries.add(new Entry(palette.get(i), counts[i]));
        }
        entries.sort(Comparator.<Entry>comparingInt(e -> e.count).reversed());
        return entries;
    }

    public static void writeTextFile(Template tmpl, File outFile) throws IOException {
        List<Entry> entries = build(tmpl);
        int total = tmpl.totalSticks();
        try (PrintWriter pw = new PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream(outFile), StandardCharsets.UTF_8))) {
            pw.println("3D-Art-Creator -- Bestueckungsliste / Parts list");
            pw.println("====================================================");
            pw.printf("Board size:   %.1f x %.1f mm%n",
                    tmpl.getBoardWidthMm(), tmpl.getBoardHeightMm());
            pw.printf("Grid:         %d cols x %d rows%n", tmpl.getCols(), tmpl.getRows());
            pw.printf("Stick pitch:  %.2f mm%n", tmpl.getPitchMm());
            pw.printf("Stick diam.:  %.2f mm%n", tmpl.getDiameterMm());
            pw.printf("Total sticks: %d%n", total);
            pw.println();
            pw.printf("%-6s  %-22s  %-12s  %8s  %6s%n",
                    "Code", "Name", "RGB", "Count", "%");
            pw.println("-------------------------------------------------------------------");
            for (Entry e : entries) {
                double pct = total == 0 ? 0.0 : (100.0 * e.count / total);
                pw.printf("%-6s  %-22s  %3d/%3d/%3d   %8d  %5.1f%%%n",
                        e.entry.code, truncate(e.entry.name, 22),
                        e.entry.r, e.entry.g, e.entry.b,
                        e.count, pct);
            }
        }
    }

    private static String truncate(String s, int maxLen) {
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 1) + "...";
    }

    public static final class Entry {
        public final ColorPalette.Entry entry;
        public final int count;
        Entry(ColorPalette.Entry e, int count) { this.entry = e; this.count = count; }
    }
}
