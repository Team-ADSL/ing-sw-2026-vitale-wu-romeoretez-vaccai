package org.adsl.client.view.gui;

import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;

import java.util.HashMap;
import java.util.Map;

/**
 * Small Christmas-Chalk number used as an overlay on the deck-summary icons
 * (the {@code xN} card counts and the per-type extra-info badges). Mirrors
 * {@link Chip}'s rasterise-and-cache strategy: a chalk {@link Text} with an
 * OUTSIDE stroke is expensive to draw live (Marlin rebuilds the stroked
 * glyph-outline every render and the player panels are rebuilt on every board
 * update), so each distinct number is snapshotted once into an {@link Image}
 * and cached; later calls return a cheap {@link ImageView}.
 */
public final class ChalkBadge {

    /** Oversample so the cached raster stays crisp on HiDPI displays. */
    private static final double SNAPSHOT_SCALE = 2.0;

    /** text + size + fill + stroke + strokeWidth -> pre-rasterised image. */
    private static final Map<String, Image> CACHE = new HashMap<>();

    private ChalkBadge() {}

    /**
     * Cached chalk number.
     *
     * @param text    the string to render
     * @param size    font size in points
     * @param fill    glyph fill colour
     * @param stroke  outline colour, or {@code null} for no outline
     * @param strokeW outline width (ignored when {@code stroke} is {@code null})
     */
    public static ImageView number(String text, double size, Color fill, Color stroke, double strokeW) {
        String key = text + ":" + size + ":" + fill + ":" + stroke + ":" + strokeW;
        Image img = CACHE.computeIfAbsent(key, k -> rasterize(text, size, fill, stroke, strokeW));
        ImageView v = new ImageView(img);
        v.setFitWidth(img.getWidth() / SNAPSHOT_SCALE);
        v.setFitHeight(img.getHeight() / SNAPSHOT_SCALE);
        v.setSmooth(true);
        return v;
    }

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
