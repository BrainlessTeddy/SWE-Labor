package com.artcreator.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Ordered, named colour palette. Each entry has a code (e.g. "C12"), a human
 * readable name and an RGB colour.
 *
 * <p>Palette entries are immutable; the palette itself can be edited (entries
 * added, removed, replaced).</p>
 */
public final class ColorPalette {

    public static final ColorPalette DEFAULT_24 = buildDefault24();

    private final String name;
    private final List<Entry> entries;

    public ColorPalette(String name, List<Entry> entries) {
        this.name = Objects.requireNonNull(name);
        this.entries = new ArrayList<>(entries);
    }

    public String getName()       { return name; }
    public List<Entry> entries()  { return Collections.unmodifiableList(entries); }
    public int size()             { return entries.size(); }
    public Entry get(int index)   { return entries.get(index); }

    public ColorPalette withEntries(List<Entry> newEntries) {
        return new ColorPalette(name, newEntries);
    }

    /** Find the closest palette entry to {@code rgb} using squared Euclidean distance in RGB. */
    public int nearestIndex(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >>  8) & 0xFF;
        int b =  rgb        & 0xFF;
        int best = 0;
        long bestDist = Long.MAX_VALUE;
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            int dr = r - e.r, dg = g - e.g, db = b - e.b;
            long d = (long) dr*dr + (long) dg*dg + (long) db*db;
            if (d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        return best;
    }

    public Entry nearest(int rgb) { return entries.get(nearestIndex(rgb)); }

    /** Generate a fresh code that doesn't yet exist (e.g. for added entries). */
    public String nextCode() {
        int max = 0;
        for (Entry e : entries) {
            if (e.code.startsWith("C")) {
                try { max = Math.max(max, Integer.parseInt(e.code.substring(1))); }
                catch (NumberFormatException ignored) { /* skip */ }
            }
        }
        return "C" + (max + 1);
    }

    /** Single colour entry. Immutable. */
    public static final class Entry {
        public final String code;
        public final String name;
        public final int r, g, b;
        public final int rgb;

        public Entry(String code, String name, int r, int g, int b) {
            this.code = code; this.name = name;
            this.r = r; this.g = g; this.b = b;
            this.rgb = ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
        }

        public Color awt() { return new Color(r, g, b); }

        @Override public String toString() { return code + " (" + name + ")"; }
    }

    private static ColorPalette buildDefault24() {
        // 24 reasonably distinct colours covering the main hues plus neutrals.
        List<Entry> e = Arrays.asList(
            new Entry("C01", "Schwarz",       0,   0,   0),
            new Entry("C02", "Anthrazit",    50,  50,  50),
            new Entry("C03", "Grau",        128, 128, 128),
            new Entry("C04", "Hellgrau",    200, 200, 200),
            new Entry("C05", "Weiss",        255, 255, 255),
            new Entry("C06", "Elfenbein",   245, 235, 205),
            new Entry("C07", "Sand",        210, 180, 140),
            new Entry("C08", "Braun",       120,  72,  40),
            new Entry("C09", "Dunkelbraun",  72,  44,  20),
            new Entry("C10", "Rot",         200,  35,  35),
            new Entry("C11", "Bordeaux",    120,  20,  40),
            new Entry("C12", "Orange",      240, 130,  40),
            new Entry("C13", "Gelb",        245, 215,  50),
            new Entry("C14", "Hellgelb",    255, 235, 130),
            new Entry("C15", "Limette",     180, 220,  80),
            new Entry("C16", "Gruen",         60, 160,  80),
            new Entry("C17", "Dunkelgruen",   25,  90,  50),
            new Entry("C18", "Tuerkis",       40, 175, 180),
            new Entry("C19", "Hellblau",    130, 195, 230),
            new Entry("C20", "Blau",         40, 100, 200),
            new Entry("C21", "Dunkelblau",   20,  40, 110),
            new Entry("C22", "Violett",     130,  60, 170),
            new Entry("C23", "Rosa",        235, 150, 180),
            new Entry("C24", "Magenta",     220,  40, 130)
        );
        return new ColorPalette("Standard 24", e);
    }
}
