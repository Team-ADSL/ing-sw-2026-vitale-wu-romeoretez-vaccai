package org.adsl.client.view.gui.game;

import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.adsl.client.view.gui.GlyphStrip;
import org.adsl.client.view.gui.ImageCatalog;

/**
 * The hint row at the bottom of the game screen. Renders either a plain text
 * hint (with the confirm button at the far end) or, during card picking, the
 * up/down move-count arrows next to the confirm button. The screen decides
 * <em>which</em> hint to show and the enabled state of the button; this bar only
 * knows how to draw them into the shared {@code hintLabel} / {@code confirmButton}.
 */
public final class MoveHintBar {

    /** Id tagging the injected arrows+confirm container so it can be removed/restored. */
    private static final String MOVE_HINT_ID = "__moveHintRow";
    /** Move-count glyphs are sized to this height so the row matches the arrow icons. */
    private static final double COUNT_GLYPH_HEIGHT = 24.0;

    private final TextFlow hintLabel;
    private final Button confirmButton;

    public MoveHintBar(TextFlow hintLabel, Button confirmButton) {
        this.hintLabel = hintLabel;
        this.confirmButton = confirmButton;
    }

    /** Plain text hint; restores the TextFlow and the confirm button to their row. */
    public void setHint(String text) {
        restoreHintLabel();
        Text t = new Text(text);
        t.setFill(Color.web("#FDF3D3"));
        t.setFont(Font.font(13));
        hintLabel.getChildren().setAll(t);
    }

    /**
     * Restores the TextFlow to visible and removes any injected move-hint HBox
     * from its parent. Called before every plain-text hint.
     */
    private void restoreHintLabel() {
        if (hintLabel == null) return;
        if (hintLabel.getParent() instanceof HBox hintRow) {
            hintRow.getChildren().removeIf(n -> MOVE_HINT_ID.equals(n.getId()));
            // confirmButton may have been inside the combined box — put it back
            if (!hintRow.getChildren().contains(confirmButton)) {
                hintRow.getChildren().add(confirmButton);
            }
        }
        hintLabel.setVisible(true);
        hintLabel.setManaged(true);
    }

    /** Up/down move-count arrows; grey when no picks of that direction remain. */
    public void setMoveHint(int upper, int lower, int pickedUpper, int pickedLower) {
        int upLeft   = upper - pickedUpper;
        int downLeft = lower - pickedLower;
        boolean upGrey   = upLeft <= 0;
        boolean downGrey = downLeft <= 0;

        ImageView upArrow   = arrowIcon("/assets/general/arrow_up.png",   upGrey);
        Node      upCount   = countGlyph(upLeft,   GlyphStrip.Style.MOVE_UP,   upGrey);
        ImageView downArrow = arrowIcon("/assets/general/arrow_down.png", downGrey);
        Node      downCount = countGlyph(downLeft, GlyphStrip.Style.MOVE_DOWN, downGrey);

        HBox arrowRow = new HBox(4, upArrow, upCount, gap(12), downArrow, downCount);
        arrowRow.setAlignment(Pos.CENTER);

        if (hintLabel != null && hintLabel.getParent() instanceof HBox hintRow) {
            hintRow.getChildren().removeIf(n -> MOVE_HINT_ID.equals(n.getId()));
            hintLabel.setVisible(false);
            hintLabel.setManaged(false);
            // Pull confirmButton out of hintRow and place it alongside the arrows
            // inside a single centered container that grows to fill available space.
            // This keeps the two elements together instead of being pushed to
            // opposite ends by HBox layout.
            hintRow.getChildren().remove(confirmButton);
            HBox combined = new HBox(16, arrowRow, confirmButton);
            combined.setAlignment(Pos.CENTER);
            combined.setId(MOVE_HINT_ID);
            HBox.setHgrow(combined, Priority.ALWAYS);
            hintRow.getChildren().add(0, combined);
        } else {
            hintLabel.getChildren().setAll(arrowRow);
        }
    }

    /** Fixed-width invisible spacer for the move-hint row. */
    private Node gap(double w) {
        Region r = new Region();
        r.setMinWidth(w);
        r.setPrefWidth(w);
        r.setMaxWidth(w);
        return r;
    }

    /**
     * Move-count label composed from the {@code move_counts/<up|down>} glyph PNGs
     * by {@link GlyphStrip} (ChristmasChalk, coloured fill + white outline, baked
     * offline by {@code tools/GlyphAtlasGenerator}) so there is zero runtime
     * rasterisation. Any value is supported — single digits are composed side by
     * side, so the count is no longer capped. If {@code grey} is true we apply a
     * greyscale filter in the GUI.
     */
    private Node countGlyph(int value, GlyphStrip.Style style, boolean grey) {
        Node node = GlyphStrip.build(String.valueOf(value), style, null, COUNT_GLYPH_HEIGHT);
        if (node == null) node = new Group(); // assets missing — render nothing rather than crash
        if (grey) {
            ColorAdjust desaturate = new ColorAdjust();
            desaturate.setSaturation(-1);
            desaturate.setBrightness(-0.15);
            node.setEffect(desaturate);
            node.setOpacity(0.5);
        }
        return node;
    }

    /**
     * Arrow icon for the move hint. {@code grey} desaturates it to greyscale
     * (used when its move count is 0, so the inactive arrow reads as dim).
     */
    private ImageView arrowIcon(String path, boolean grey) {
        ImageView iv = new ImageView(ImageCatalog.load(path));
        iv.setFitHeight(24);
        iv.setPreserveRatio(true);
        if (grey) {
            ColorAdjust desaturate = new ColorAdjust();
            desaturate.setSaturation(-1);
            desaturate.setBrightness(-0.15);
            iv.setEffect(desaturate);
            iv.setOpacity(0.5);
        }
        return iv;
    }
}
