package org.adsl.client.view.gui;

import javafx.scene.Group;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.ColorInput;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Composes a ChristmasChalk number from single-glyph PNGs ({@code 0}–{@code 9},
 * {@code x}, {@code minus}) so any value renders without a per-value asset and
 * without a runtime JavaFX snapshot. Glyphs and their layout metrics are baked
 * offline by {@code tools/GlyphAtlasGenerator}.
 *
 * <p>Layout maths live in {@link GlyphLayout} (pure, unit-tested); this class
 * only loads the assets, positions {@link ImageView}s in a {@link Group}, and
 * optionally tints white-fill glyphs to a target colour.
 */
public final class GlyphStrip {

    private GlyphStrip() {}

    /** A baked glyph style: its asset directory under {@code /assets/}. */
    public enum Style {
        STROKE_29("badge_glyphs/stroke_29"),
        PLAIN_25("badge_glyphs/plain_25"),
        PLAIN_20("badge_glyphs/plain_20"),
        MOVE_UP("move_counts/up"),
        MOVE_DOWN("move_counts/down");

        final String dir;
        Style(String dir) { this.dir = dir; }
    }

    /** Parsed metrics per style, loaded once from the bundled {@code metrics.properties}. */
    private static final Map<Style, GlyphLayout.Metrics> METRICS = new HashMap<>();

    private static GlyphLayout.Metrics metrics(Style style) {
        return METRICS.computeIfAbsent(style, s -> {
            String path = "/assets/" + s.dir + "/metrics.properties";
            try (InputStream in = GlyphStrip.class.getResourceAsStream(path)) {
                if (in == null) return null;
                return GlyphLayout.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            } catch (Exception e) {
                return null;
            }
        });
    }

    /**
     * Builds a composed glyph node for {@code text} in {@code style}.
     *
     * @param text       the number/string (e.g. {@code "x12"}, {@code "-7"}, {@code "42"})
     * @param style      which baked glyph set to use
     * @param tint       fill colour for white-fill styles, or {@code null} to leave glyphs as-is
     * @param fitHeight  target display height in px, or {@code <= 0} for the glyphs' natural size
     * @return the composed {@link Group}, or {@code null} if metrics/assets are missing
     *         (caller should fall back to a live snapshot)
     */
    public static Group build(String text, Style style, Color tint, double fitHeight) {
        GlyphLayout.Metrics m = metrics(style);
        if (m == null) return null;

        List<GlyphLayout.Placement> placements = GlyphLayout.place(text, m);
        if (placements.isEmpty()) return null;

        double scale = m.scale();
        boolean doTint = tint != null && !isWhite(tint);

        // First pass: load images and find the content height so a single uniform
        // factor can scale the whole strip to fitHeight without a transform (which
        // would not be reflected in the Group's layoutBounds for HBox sizing).
        int n = placements.size();
        Image[] imgs = new Image[n];
        double top = Double.MAX_VALUE, bottom = -Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            GlyphLayout.Placement p = placements.get(i);
            Image img;
            try {
                img = ImageCatalog.load("/assets/" + style.dir + "/" + p.name() + ".png");
            } catch (Exception e) {
                return null;
            }
            imgs[i] = img;
            double h = img.getHeight() / scale;
            top = Math.min(top, p.y());
            bottom = Math.max(bottom, p.y() + h);
        }
        double naturalH = bottom - top;
        double f = (fitHeight > 0 && naturalH > 0) ? fitHeight / naturalH : 1.0;

        Group g = new Group();
        for (int i = 0; i < n; i++) {
            GlyphLayout.Placement p = placements.get(i);
            Image img = imgs[i];
            double w = (img.getWidth() / scale) * f;
            double h = (img.getHeight() / scale) * f;
            ImageView iv = new ImageView(img);
            iv.setFitWidth(w);
            iv.setFitHeight(h);
            iv.setSmooth(true);
            iv.setLayoutX(p.x() * f);
            iv.setLayoutY(p.y() * f);
            if (doTint) {
                iv.setEffect(new Blend(BlendMode.SRC_ATOP, null, new ColorInput(0, 0, w, h, tint)));
            }
            g.getChildren().add(iv);
        }
        return g;
    }

    private static boolean isWhite(Color c) {
        return c.getRed() > 0.99 && c.getGreen() > 0.99 && c.getBlue() > 0.99;
    }
}
