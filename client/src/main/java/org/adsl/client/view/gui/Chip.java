package org.adsl.client.view.gui;

import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

/**
 * Builds the small token-style chips the board uses for food and PP values.
 * The image sits on the back ImageView; the number is rendered on top with
 * the Christmas Chalk font.
 *
 * <ul>
 *   <li>Food: {@code food_back.png}, white text with a black outline.</li>
 *   <li>PP positive: {@code pp_earn_back.png}, plain white text.</li>
 *   <li>PP negative: {@code pp_loss_back.png}, plain white text.</li>
 * </ul>
 */
public final class Chip {

    private Chip() {}

    public static StackPane food(int value, double width) {
        return build(ImageCatalog.foodBack(), String.valueOf(value), width, true);
    }

    public static StackPane pp(int value, double width) {
        Image bg = value >= 0 ? ImageCatalog.ppEarnBack() : ImageCatalog.ppLossBack();
        return build(bg, String.valueOf(Math.abs(value)), width, false);
    }

    private static StackPane build(Image bg, String text, double width, boolean outline) {
        ImageView bgView = new ImageView(bg);
        bgView.setFitWidth(width);
        bgView.setPreserveRatio(true);
        bgView.setSmooth(true);

        Text label = new Text(text);
        label.setFont(ImageCatalog.chalkFont(width * 0.5));
        label.setFill(Color.WHITE);
        if (outline) {
            label.setStroke(Color.BLACK);
            label.setStrokeWidth(Math.max(1.0, width * 0.04));
        }

        StackPane chip = new StackPane(bgView, label);
        chip.setAlignment(Pos.CENTER);
        return chip;
    }
}
