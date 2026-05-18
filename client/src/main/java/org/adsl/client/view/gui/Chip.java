package org.adsl.client.view.gui;

import javafx.geometry.Pos;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;

/**
 * Token-style chips for food and PP. The background image sits beneath a
 * Christmas-Chalk number. Food is masked into a perfect circle; PP earn/loss
 * are already octagonal PNGs with transparency and don't need masking.
 *
 * <ul>
 *   <li>Food: white digits with thick black stroke + soft shadow, anchored
 *       slightly below center over the fish/orange artwork.</li>
 *   <li>PP positive: dark-brown digits, centered in the laurel.</li>
 *   <li>PP negative: cream digits (including minus sign), centered.</li>
 * </ul>
 *
 * Long values are font-scaled so 3+ char strings ("-100", "150") stay inside
 * the laurel/circle.
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

    // Multiplier on BASE_FONT_RATIO by string length. Index 0 unused;
    // longer than the array clamps to the last value.
    private static final double[] LENGTH_SHRINK = { 1.0, 1.0, 0.88, 0.65, 0.50, 0.40 };

    private Chip() {}

    public static StackPane food(int value, double width) {
        StackPane chip = new StackPane();
        chip.setPrefSize(width, width);
        chip.setMinSize(width, width);
        chip.setMaxSize(width, width);

        ImageView bg = roundImage(ImageCatalog.foodBack(), width);
        Circle clip = new Circle(width / 2.0, width / 2.0, width / 2.0);
        chip.setClip(clip);

        Text num = chalkNumber(String.valueOf(value), width, FOOD_FILL, 1.0);
        num.setStroke(FOOD_STROKE);
        num.setStrokeWidth(Math.max(0.8, width * STROKE_RATIO));
        num.setEffect(new DropShadow(width * 0.08, Color.rgb(0, 0, 0, 0.6)));
        num.setTranslateY(width * FOOD_OFFSET_Y_RATIO);

        chip.getChildren().addAll(bg, num);
        chip.setAlignment(Pos.CENTER);
        return chip;
    }

    public static StackPane pp(int value, double width) {
        boolean positive = value >= 0;
        Image bgImg = positive ? ImageCatalog.ppEarnBack() : ImageCatalog.ppLossBack();
        Color fill  = positive ? PP_POS_FILL : PP_NEG_FILL;
        double extra = PP_BASE_MUL * (positive ? 1.0 : PP_NEG_SHRINK);

        StackPane chip = new StackPane();
        chip.setPrefSize(width, width);
        chip.setMinSize(width, width);
        chip.setMaxSize(width, width);

        ImageView bg = roundImage(bgImg, width);
        Text num = chalkNumber(String.valueOf(value), width, fill, extra);
        chip.getChildren().addAll(bg, num);
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
