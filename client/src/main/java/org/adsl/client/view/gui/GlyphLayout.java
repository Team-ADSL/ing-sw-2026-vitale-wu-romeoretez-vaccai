package org.adsl.client.view.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure (no-JavaFX) layout maths for the ChristmasChalk PNG bitmap font.
 *
 * <p>Numbers are composed at runtime from single-glyph PNGs ({@code 0}–{@code 9},
 * {@code x}, {@code minus}) instead of one pre-baked PNG per value. This class
 * owns the two pieces that can be reasoned about without a scene graph:
 * tokenising a string into glyph asset names, and computing each glyph's
 * top-left position from the font metrics emitted by
 * {@code tools/GlyphAtlasGenerator}.
 *
 * <p>All stored metrics are in oversampled (2×) pixels; {@link #place} divides by
 * {@code scale} so the returned placements are already in display space. Bearings
 * are baked by the generator to point straight at the glyph image's top-left, so
 * the layout here is a plain pen walk with no padding term.
 */
public final class GlyphLayout {

    private GlyphLayout() {}

    /** Per-glyph metrics: {@code advance} pen step, image top-left {@code bearingX}/{@code bearingTop}. */
    public record Glyph(double advance, double bearingX, double bearingTop) {}

    /** A glyph image's display-space top-left placement. */
    public record Placement(String name, double x, double y) {}

    /** Parsed {@code metrics.properties}: oversampling scale, baseline ascent, and glyph table. */
    public static final class Metrics {
        private final double scale;
        private final double ascent;
        private final Map<String, Glyph> glyphs;

        Metrics(double scale, double ascent, Map<String, Glyph> glyphs) {
            this.scale = scale;
            this.ascent = ascent;
            this.glyphs = glyphs;
        }

        public double scale()          { return scale; }
        public boolean has(String n)   { return glyphs.containsKey(n); }
        Glyph glyph(String n)          { return glyphs.get(n); }
    }

    /**
     * Maps a string to glyph asset names: digits map to themselves, {@code 'x'}
     * to {@code "x"}, {@code '-'} to {@code "minus"}. Returns {@code null} if any
     * character has no glyph (caller should fall back to a live snapshot).
     */
    public static List<String> tokens(String text) {
        if (text == null) return null;
        List<String> out = new ArrayList<>(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '0' && c <= '9') out.add(String.valueOf(c));
            else if (c == 'x')        out.add("x");
            else if (c == '-')        out.add("minus");
            else return null;
        }
        return out;
    }

    /** Parses a {@code metrics.properties} text body into {@link Metrics}. */
    public static Metrics parse(String body) {
        double scale = 1.0;
        double ascent = 0.0;
        Map<String, double[]> raw = new HashMap<>(); // name -> {advance, bearingX, bearingTop}

        for (String line : body.split("\\R")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int eq = line.indexOf('=');
            if (eq < 0) continue;
            String key = line.substring(0, eq).trim();
            double val = Double.parseDouble(line.substring(eq + 1).trim());

            if (key.equals("scale")) {
                scale = val;
            } else if (key.equals("ascent")) {
                ascent = val;
            } else if (key.startsWith("glyph.")) {
                int dot = key.lastIndexOf('.');
                String name = key.substring("glyph.".length(), dot);
                String attr = key.substring(dot + 1);
                double[] g = raw.computeIfAbsent(name, k -> new double[3]);
                switch (attr) {
                    case "advance"    -> g[0] = val;
                    case "bearingX"   -> g[1] = val;
                    case "bearingTop" -> g[2] = val;
                    default -> { /* ignore unknown attr */ }
                }
            }
        }

        Map<String, Glyph> glyphs = new HashMap<>();
        raw.forEach((name, g) -> glyphs.put(name, new Glyph(g[0], g[1], g[2])));
        return new Metrics(scale, ascent, glyphs);
    }

    /**
     * Computes the display-space top-left of each glyph image for {@code text}.
     * Returns an empty list when the string is unmappable or any glyph is absent
     * from {@code m} (caller falls back).
     */
    public static List<Placement> place(String text, Metrics m) {
        List<String> names = tokens(text);
        if (names == null) return List.of();
        for (String n : names) {
            if (!m.has(n)) return List.of();
        }
        List<Placement> out = new ArrayList<>(names.size());
        double pen = 0;
        for (String n : names) {
            Glyph g = m.glyph(n);
            double x = (pen + g.bearingX()) / m.scale;
            double y = (m.ascent - g.bearingTop()) / m.scale;
            out.add(new Placement(n, x, y));
            pen += g.advance();
        }
        return out;
    }
}
