package org.adsl.client.view.gui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;

import java.util.HashMap;
import java.util.Map;

/**
 * Token-style chips for food and PP. The background image sits beneath a
 * Christmas-Chalk number. Food is masked into a perfect circle; PP earn/loss
 * are already octagonal PNGs with transparency and don't need masking.
 *
 * <ul>
 *   <li>Food: white digits with thick black external outline, anchored
 *       slightly below center over the fish/orange artwork.</li>
 *   <li>PP positive: dark-brown digits, centered in the laurel.</li>
 *   <li>PP negative: cream digits (including minus sign), centered.</li>
 * </ul>
 *
 * Long values are font-scaled so 3+ char strings ("-100", "150") stay inside
 * the laurel/circle.
 *
 * <h2>Why chips are rasterised to a cached image</h2>
 * The chalk-font {@link Text} with an OUTSIDE stroke is expensive to rasterise
 * (Marlin builds a stroked glyph-outline path every time the node is first
 * drawn). The board is fully rebuilt on every interaction, so re-creating live
 * Text nodes froze the GPU for ~1s per render on weaker iGPUs. Instead each
 * distinct chip (type + value + width) is rendered once via {@link Node#snapshot}
 * into an {@link Image} and cached; subsequent calls return a cheap
 * {@link ImageView} of that image — pixel-identical, near-zero cost.
 */
public final class Chip {

    private static final Color FOOD_FILL    = Color.WHITE;
    private static final Color FOOD_STROKE  = Color.BLACK;
    private static final Color PP_POS_FILL  = Color.web("#4A1F18");
    private static final Color PP_NEG_FILL  = Color.web("#F2E2C6");

    private static final double FOOD_OFFSET_Y_RATIO = 0.18;
    private static final double BASE_FONT_RATIO     = 0.5;
    private static final double STROKE_RATIO        = 0.025;
    private static final double PP_BASE_MUL         = 0.90;
    private static final double PP_NEG_SHRINK       = 0.90;

    /** Oversampling factor for the cached snapshot so chips stay crisp when the
     *  display runs at HiDPI (the ImageView downscales the 2x raster). */
    private static final double SNAPSHOT_SCALE = 2.0;

    // Multiplier on BASE_FONT_RATIO by string length. Index 0 unused;
    // longer than the array clamps to the last value.
    private static final double[] LENGTH_SHRINK = { 1.0, 1.0, 0.88, 0.65, 0.50, 0.40 };

    /** type+value+width -> pre-rasterised chip image. */
    private static final Map<String, Image> CACHE = new HashMap<>();

    private Chip() {}

    public static StackPane food(int value, double width) {
        Image img = CACHE.computeIfAbsent("food:" + value + ":" + width,
                k -> rasterize(buildFood(value, width), width));
        return wrap(img, width);
    }

    public static StackPane pp(int value, double width) {
        Image img = CACHE.computeIfAbsent("pp:" + value + ":" + width,
                k -> rasterize(buildPp(value, width), width));
        return wrap(img, width);
    }

    // ── Live chip construction (run once per distinct chip, then cached) ──────

    private static StackPane buildFood(int value, double width) {
        StackPane chip = sizedPane(width);

        ImageView bg = roundImage(ImageCatalog.foodBack(), width);
        Circle clip = new Circle(width / 2.0, width / 2.0, width / 2.0);
        chip.setClip(clip);

        Text num = chalkNumber(String.valueOf(value), width, FOOD_FILL, 1.0);
        num.setStroke(FOOD_STROKE);
        num.setStrokeType(StrokeType.OUTSIDE);
        num.setStrokeWidth(Math.max(0.8, width * STROKE_RATIO));
        // Round join/cap so the chalk font's sharp corners don't shoot miter
        // spikes past the outline (same treatment as the move-count glyphs).
        num.setStrokeLineJoin(StrokeLineJoin.ROUND);
        num.setStrokeLineCap(StrokeLineCap.ROUND);
        num.setStrokeMiterLimit(1.0);
        num.setTranslateY(width * FOOD_OFFSET_Y_RATIO);

        chip.getChildren().addAll(bg, num);
        return chip;
    }

    private static StackPane buildPp(int value, double width) {
        boolean positive = value >= 0;
        Image bgImg = positive ? ImageCatalog.ppEarnBack() : ImageCatalog.ppLossBack();
        Color fill  = positive ? PP_POS_FILL : PP_NEG_FILL;
        double extra = PP_BASE_MUL * (positive ? 1.0 : PP_NEG_SHRINK);

        StackPane chip = sizedPane(width);
        ImageView bg = roundImage(bgImg, width);
        Text num = chalkNumber(String.valueOf(value), width, fill, extra);
        chip.getChildren().addAll(bg, num);
        return chip;
    }

    // ── Snapshot / cache plumbing ─────────────────────────────────────────────

    /** Renders a freshly built chip node into a transparent oversampled image. */
    private static Image rasterize(StackPane node, double width) {
        // An unattached node has width/height 0 until something resizes it, so
        // StackPane would crowd its children into the top-left. Resize to the
        // chip box first, then lay out, so children center correctly.
        node.resize(width, width);
        node.applyCss();
        node.layout();
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        sp.setTransform(new Scale(SNAPSHOT_SCALE, SNAPSHOT_SCALE));
        int px = (int) Math.ceil(width * SNAPSHOT_SCALE);
        WritableImage img = new WritableImage(px, px);
        node.snapshot(sp, img);
        return img;
    }

    /** Cheap reusable view of a cached chip image at the requested size. */
    private static StackPane wrap(Image img, double width) {
        StackPane chip = sizedPane(width);
        ImageView v = new ImageView(img);
        v.setFitWidth(width);
        v.setFitHeight(width);
        v.setPreserveRatio(false);
        v.setSmooth(true);
        chip.getChildren().add(v);
        return chip;
    }

    private static StackPane sizedPane(double width) {
        StackPane chip = new StackPane();
        chip.setMinSize(width, width);
        chip.setPrefSize(width, width);
        chip.setMaxSize(width, width);
        chip.setAlignment(Pos.CENTER);
        return chip;
    }

    private static ImageView roundImage(Image img, double width) {
        ImageView v = new ImageView(img);
        v.setFitWidth(width);
        v.setFitHeight(width);
        v.setPreserveRatio(false);
        v.setSmooth(true);
        return v;
    }

    private static Text chalkNumber(String text, double width, Color fill, double extraShrink) {
        int len = Math.max(1, text.length());
        double mul = len < LENGTH_SHRINK.length
                ? LENGTH_SHRINK[len]
                : LENGTH_SHRINK[LENGTH_SHRINK.length - 1];
        double size = width * BASE_FONT_RATIO * mul * extraShrink;
        Text t = new Text(text);
        t.setFont(ImageCatalog.chalkFont(size));
        t.setFill(fill);
        return t;
    }
}
