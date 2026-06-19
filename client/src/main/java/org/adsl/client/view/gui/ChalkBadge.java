package org.adsl.client.view.gui;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;

import java.util.HashMap;
import java.util.Map;

/**
 * Small Christmas-Chalk number used as an overlay on deck-summary icons.
 *
 * Rendering strategy — zero runtime snapshot cost:
 *   Numbers are composed from single-glyph PNGs ({@code 0}-{@code 9}, {@code x},
 *   {@code minus}) by {@link GlyphStrip}, so any value renders without a per-value
 *   asset. Plain (un-stroked) styles bake WHITE and are tinted at display time.
 *   Falls back to a live JavaFX snapshot only when no glyph style matches the
 *   requested size, or a glyph asset is missing.
 *
 * Glyph styles (baked by tools/GlyphAtlasGenerator):
 *   stroke_29/  — WHITE fill + BLACK OUTSIDE stroke, 14.5pt×2
 *   plain_25/   — WHITE fill only, 12.5pt×2
 *   plain_20/   — WHITE fill only, 10pt×2
 */
public final class ChalkBadge {

    private static final double SNAPSHOT_SCALE = 2.0;

    /** Runtime-snapshot fallback cache (text:size:fill:stroke:strokeW → image). */
    private static final Map<String, Image> CACHE = new HashMap<>();

    private ChalkBadge() {}

    /**
     * Returns a chalk-font number as a {@link Node}.
     * Composes from per-glyph PNGs when the size matches a baked style; otherwise
     * falls back to a live snapshot.
     *
     * @param text    the string to render
     * @param size    font size in points (14.5, 12.5, or 10.0 have baked glyphs)
     * @param fill    glyph fill colour
     * @param stroke  outline colour, or {@code null} for no outline
     * @param strokeW outline width (ignored when {@code stroke} is {@code null})
     */
    public static Node number(String text, double size, Color fill, Color stroke, double strokeW) {
        GlyphStrip.Style style = styleFor(size, stroke != null);
        if (style != null) {
            // Natural size (fitHeight <= 0) preserves the previous per-badge sizing.
            Group strip = GlyphStrip.build(text, style, fill, 0);
            if (strip != null) return strip;
            // fall through to runtime snapshot
        }

        // Runtime snapshot fallback
        String key = text + ":" + size + ":" + fill + ":" + stroke + ":" + strokeW;
        Image img = CACHE.computeIfAbsent(key, k -> rasterize(text, size, fill, stroke, strokeW));
        ImageView v = new ImageView(img);
        v.setFitWidth(img.getWidth()  / SNAPSHOT_SCALE);
        v.setFitHeight(img.getHeight() / SNAPSHOT_SCALE);
        v.setSmooth(true);
        return v;
    }

    /**
     * Returns the baked glyph style for the given font size and stroke flag,
     * or {@code null} if no baked style matches.
     */
    private static GlyphStrip.Style styleFor(double size, boolean hasStroke) {
        if (hasStroke  && near(size, 14.5)) return GlyphStrip.Style.STROKE_29;
        if (!hasStroke && near(size, 12.5)) return GlyphStrip.Style.PLAIN_25;
        if (!hasStroke && near(size, 10.0)) return GlyphStrip.Style.PLAIN_20;
        return null;
    }

    private static boolean near(double a, double b) { return Math.abs(a - b) < 0.05; }

    private static Image rasterize(String text, double size, Color fill, Color stroke, double strokeW) {
        Text t = new Text(text);
        t.setFont(ImageCatalog.chalkFont(size));
        t.setFill(fill);
        if (stroke != null) {
            t.setStroke(stroke);
            t.setStrokeType(StrokeType.OUTSIDE);
            t.setStrokeWidth(strokeW);
        }
        t.applyCss();
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        sp.setTransform(new Scale(SNAPSHOT_SCALE, SNAPSHOT_SCALE));
        return t.snapshot(sp, null);
    }
}
