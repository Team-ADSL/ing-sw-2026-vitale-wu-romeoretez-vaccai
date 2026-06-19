import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bakes the ChristmasChalk font into a tiny PNG bitmap font: one PNG per glyph
 * (digits {@code 0}-{@code 9} plus {@code x} / {@code minus}) per style, instead
 * of one PNG per full value. The runtime ({@link org.adsl.client.view.gui.GlyphStrip})
 * composes any number by placing single-glyph PNGs side by side using the
 * {@code metrics.properties} this generator writes — same look as the font, zero
 * runtime snapshot lag, a handful of assets instead of dozens.
 *
 * Replaces the old BadgeGlyphGenerator + CountGlyphGenerator (per-value bakers).
 *
 * Output (each dir also gets a metrics.properties):
 *   client/src/assets/badge_glyphs/stroke_29/  — 0-9, x      white fill + black OUTSIDE stroke
 *   client/src/assets/badge_glyphs/plain_25/   — 0-9         white fill (tinted at runtime)
 *   client/src/assets/badge_glyphs/plain_20/   — 0-9, minus  white fill (tinted at runtime)
 *   client/src/assets/move_counts/up/          — 0-9, minus  #1b7056 fill + white outline
 *   client/src/assets/move_counts/down/        — 0-9, minus  #9f3850 fill + white outline
 *
 * Run (headless, JDK only — no JavaFX needed):
 *   javac -d tools/out tools/GlyphAtlasGenerator.java
 *   java  -Djava.awt.headless=true -cp tools/out GlyphAtlasGenerator
 */
public final class GlyphAtlasGenerator {

    private static final double SCALE = 2.0;  // matches the runtime oversampling

    // Font sizes (logical pt × SCALE → AWT pt) — copied verbatim from the old generators.
    private static final float SIZE_STROKE   = (float) (14.5 * SCALE); // 29pt
    private static final float SIZE_PLAIN_25 = (float) (12.5 * SCALE); // 25pt
    private static final float SIZE_PLAIN_20 = (float) (10.0 * SCALE); // 20pt
    private static final float SIZE_MOVE     = (float) (22.0 * SCALE); // 44pt

    private static final float STROKE_W      = (float) (1.1 * SCALE * 2); // stroke_29 outline
    private static final double MOVE_OUTSIDE = 1.0 * SCALE;               // move_counts outline

    private static final Color WHITE = Color.WHITE;
    private static final Color BLACK = Color.BLACK;
    private static final Color UP_FILL   = new Color(0x1b, 0x70, 0x56);
    private static final Color DOWN_FILL = new Color(0x9f, 0x38, 0x50);

    private static final FontRenderContext FRC = new FontRenderContext(null, true, true);

    /** A glyph to bake: its asset filename and the source character to render. */
    private record Glyph(String name, String ch) {}

    private static final Glyph[] DIGITS = digits();
    private static final Glyph   X      = new Glyph("x", "x");
    private static final Glyph   MINUS  = new Glyph("minus", "-");

    public static void main(String[] args) throws Exception {
        String fontPath = "client/src/assets/fonts/ChristmasChalk.ttf";
        String outRoot  = "client/src/assets";

        Font base = Font.createFont(Font.TRUETYPE_FONT, new File(fontPath));

        // stroke_29: digits + "x", white fill + black OUTSIDE stroke.
        emit(base.deriveFont(SIZE_STROKE), outRoot + "/badge_glyphs/stroke_29",
                append(DIGITS, X), Mode.STROKE, null);

        // plain_25: digits only, white fill (tinted at runtime).
        emit(base.deriveFont(SIZE_PLAIN_25), outRoot + "/badge_glyphs/plain_25",
                DIGITS, Mode.PLAIN, null);

        // plain_20: digits + minus, white fill (tinted at runtime).
        emit(base.deriveFont(SIZE_PLAIN_20), outRoot + "/badge_glyphs/plain_20",
                append(DIGITS, MINUS), Mode.PLAIN, null);

        // move_counts: digits + minus, coloured fill + white outline.
        emit(base.deriveFont(SIZE_MOVE), outRoot + "/move_counts/up",
                append(DIGITS, MINUS), Mode.OUTLINED, UP_FILL);
        emit(base.deriveFont(SIZE_MOVE), outRoot + "/move_counts/down",
                append(DIGITS, MINUS), Mode.OUTLINED, DOWN_FILL);

        System.out.println("Done: glyph atlas generated.");
    }

    private enum Mode { STROKE, PLAIN, OUTLINED }

    /**
     * Renders each glyph to {@code <dir>/<name>.png} and writes {@code metrics.properties}.
     * Bearings are baked to point at the PNG's top-left so the runtime layout is a
     * plain pen walk: image-left = pen + bearingX, image-top = ascent − bearingTop.
     */
    private static void emit(Font font, String dir, Glyph[] glyphs, Mode mode, Color fill) throws Exception {
        new File(dir).mkdirs();

        // pad must match the per-mode rendering pad below.
        int pad = (mode == Mode.STROKE)   ? (int) Math.ceil(STROKE_W / 2.0) + 2
                : (mode == Mode.OUTLINED) ? (int) Math.ceil(MOVE_OUTSIDE) + 2
                : 2;

        Map<String, double[]> metrics = new LinkedHashMap<>(); // name -> {advance, bearingX, bearingTop}
        double ascent = 0;

        for (Glyph glyph : glyphs) {
            GlyphVector gv = font.createGlyphVector(FRC, glyph.ch());
            Rectangle2D b = gv.getOutline().getBounds2D();
            double advance  = gv.getGlyphMetrics(0).getAdvanceX();
            double bearingX = b.getMinX() - pad;         // pen → image left
            double bearingTop = -b.getMinY() + pad;      // baseline → image top
            metrics.put(glyph.name(), new double[]{advance, bearingX, bearingTop});
            ascent = Math.max(ascent, bearingTop);

            BufferedImage img = switch (mode) {
                case STROKE   -> render(gv, b, pad, BLACK, WHITE, STROKE_W);
                case PLAIN    -> render(gv, b, pad, null, WHITE, 0);
                case OUTLINED -> render(gv, b, pad, WHITE, fill, (float) (MOVE_OUTSIDE * 2.0));
            };
            ImageIO.write(img, "png", new File(dir + "/" + glyph.name() + ".png"));
        }

        writeMetrics(dir + "/metrics.properties", ascent, metrics);
    }

    /**
     * Renders {@code gv} into a tightly cropped image. When {@code outline} is
     * non-null, a centred stroke of {@code strokeW} is drawn first (its inner half
     * hidden by the fill = clean OUTSIDE outline), then {@code fill} on top.
     */
    private static BufferedImage render(GlyphVector gv, Rectangle2D b, int pad,
                                        Color outline, Color fill, float strokeW) {
        int w = (int) Math.ceil(b.getWidth())  + pad * 2;
        int h = (int) Math.ceil(b.getHeight()) + pad * 2;

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,   RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,      RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        var outlineShape = gv.getOutline((float) (-b.getMinX() + pad), (float) (-b.getMinY() + pad));
        if (outline != null) {
            g.setColor(outline);
            g.setStroke(new BasicStroke(strokeW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f));
            g.draw(outlineShape);
        }
        g.setColor(fill);
        g.fill(outlineShape);
        g.dispose();
        return img;
    }

    private static void writeMetrics(String path, double ascent, Map<String, double[]> metrics) throws IOException {
        try (FileWriter w = new FileWriter(path)) {
            w.write("# Generated by tools/GlyphAtlasGenerator — do not edit by hand.\n");
            w.write("scale=" + SCALE + "\n");
            w.write("ascent=" + fmt(ascent) + "\n");
            for (var e : metrics.entrySet()) {
                double[] g = e.getValue();
                w.write("glyph." + e.getKey() + ".advance="    + fmt(g[0]) + "\n");
                w.write("glyph." + e.getKey() + ".bearingX="   + fmt(g[1]) + "\n");
                w.write("glyph." + e.getKey() + ".bearingTop=" + fmt(g[2]) + "\n");
            }
        }
    }

    private static String fmt(double v) {
        return String.format(java.util.Locale.ROOT, "%.4f", v);
    }

    private static Glyph[] digits() {
        Glyph[] out = new Glyph[10];
        for (int i = 0; i <= 9; i++) out[i] = new Glyph(String.valueOf(i), String.valueOf(i));
        return out;
    }

    private static Glyph[] append(Glyph[] base, Glyph extra) {
        Glyph[] out = new Glyph[base.length + 1];
        System.arraycopy(base, 0, out, 0, base.length);
        out[base.length] = extra;
        return out;
    }
}
