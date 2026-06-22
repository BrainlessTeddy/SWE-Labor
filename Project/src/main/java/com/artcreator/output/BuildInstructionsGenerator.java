package com.artcreator.output;

import com.artcreator.model.Parameters;
import com.artcreator.model.Template;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Produces a short, human-readable build instructions document. The text is
 * intentionally template-based and explains the assembly process step by
 * step.
 */
public final class BuildInstructionsGenerator {

    private BuildInstructionsGenerator() { /* static */ }

    public static void writeTextFile(Template tmpl, Parameters params, File outFile)
            throws IOException {
        try (PrintWriter pw = new PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream(outFile), StandardCharsets.UTF_8))) {
            writeTo(pw, tmpl, params);
        }
    }

    public static String asString(Template tmpl, Parameters params) {
        java.io.StringWriter sw = new java.io.StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            writeTo(pw, tmpl, params);
        }
        return sw.toString();
    }

    private static void writeTo(PrintWriter pw, Template tmpl, Parameters params) {
        PageLayout layout = PageLayout.compute(tmpl, params);
        List<PartsListGenerator.Entry> entries = PartsListGenerator.build(tmpl);

        pw.println("3D-Art-Creator -- Bauanleitung / Build instructions");
        pw.println("====================================================");
        pw.println();
        pw.println("1. Vorbereitung");
        pw.println("---------------");
        pw.printf ("   Brettmasse:      %.1f x %.1f mm%n",
                tmpl.getBoardWidthMm(), tmpl.getBoardHeightMm());
        pw.printf ("   Raster:         %d Spalten x %d Zeilen%n",
                tmpl.getCols(), tmpl.getRows());
        pw.printf ("   Abstand:        %.2f mm (Mitte zu Mitte)%n", tmpl.getPitchMm());
        pw.printf ("   Staebchen O:     %.2f mm%n", tmpl.getDiameterMm());
        pw.printf ("   Form:           %s%n", tmpl.getShape());
        pw.printf ("   Layout:         %s%n", tmpl.getLayout());
        pw.printf ("   Sticks gesamt:  %d%n", tmpl.totalSticks());
        pw.println();

        pw.println("2. Druck der Vorlage");
        pw.println("--------------------");
        if (layout.isTiled()) {
            pw.printf ("   Die Vorlage wird auf %d x %d = %d A4-Seiten verteilt.%n",
                    layout.getRows(), layout.getCols(), layout.tiles().size());
            pw.println("   - Drucke alle Seiten in 100 % (kein 'Anpassen an Seite').");
            pw.println("   - Schneide entlang der schwarzen Schnittmarken an den Ecken.");
            pw.println("   - Lege die Seiten anhand der Beschriftung 'R<Zeile> C<Spalte>' aus.");
            pw.printf ("   - Nutze den Ueberlappungsbereich von %d mm zum Ausrichten.%n",
                    params.getTileOverlapMm());
            pw.println("   - Die runden Passmarken in den Ecken muessen exakt aufeinander liegen.");
            pw.println("   - Verbinde die Seiten mit Klebeband oder Klebestift.");
        } else {
            pw.println("   Die Vorlage passt auf ein einziges Blatt; einfach in 100 % ausdrucken.");
        }
        pw.println();

        pw.println("3. Bestueckung");
        pw.println("--------------");
        pw.println("   Lege die Staebchen sortiert nach Farbe bereit. Reihenfolge");
        pw.println("   nach Haeufigkeit (von haeufig zu selten):");
        for (PartsListGenerator.Entry e : entries) {
            pw.printf ("     %s  %-22s  Anzahl: %d%n",
                    e.entry.code, e.entry.name, e.count);
        }
        pw.println();
        pw.println("   - Beginne mit der haeufigsten Farbe und arbeite dich Reihe fuer Reihe");
        pw.println("     von oben nach unten und von links nach rechts vor.");
        pw.println("   - Klebe pro Position einen Stab passender Farbe rechtwinklig auf das");
        pw.println("     Traegerbrett. Der Code in der Vorlage zeigt die Zielfarbe an.");
        pw.println("   - Tipp: setze zuerst alle Staebchen einer Farbe, bevor du zur naechsten");
        pw.println("     wechselst -- das ist deutlich schneller.");
        pw.println();

        pw.println("4. Abschluss");
        pw.println("------------");
        pw.println("   - Lasse den Klebstoff vollstaendig aushaerten, bevor du das Bild bewegst.");
        pw.println("   - Optional kannst du die Staebchen am Ende mit einem matten Klarlack");
        pw.println("     fixieren.");
        pw.println("   - Viel Spass beim Betrachten! Das Bild wirkt aus verschiedenen Blick-");
        pw.println("     winkeln unterschiedlich -- genau das ist der gewuenschte Effekt.");
    }
}
