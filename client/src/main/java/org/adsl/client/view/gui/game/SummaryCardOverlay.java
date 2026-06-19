package org.adsl.client.view.gui.game;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import org.adsl.client.view.gui.FxUtil;

/**
 * The "SC" summary-card button (bottom-left, next to the rules button) and the
 * small flippable summary-card popup it toggles. Self-installs its trigger
 * button into the supplied root stack; the popup is added/removed on demand and
 * can be flipped between its front and back faces.
 */
public final class SummaryCardOverlay {

    private static final String[] CARD_PATHS = {
            "/assets/cards_front_cropped/summary_card.png",
            "/assets/cards_back_cropped/summary_card.png"
    };

    private final StackPane rootStack;
    private Node overlay;

    public SummaryCardOverlay(StackPane rootStack) {
        this.rootStack = rootStack;
        installButton();
    }

    private void installButton() {
        Button btn = new Button("SC");
        btn.setStyle(
            "-fx-font-size: 13px; -fx-font-weight: bold;" +
            "-fx-min-width: 40px; -fx-min-height: 40px;" +
            "-fx-max-width: 40px; -fx-max-height: 40px;" +
            "-fx-background-radius: 20; -fx-padding: 0;"
        );
        btn.setOnAction(_ -> toggle());
        StackPane.setAlignment(btn, Pos.BOTTOM_LEFT);
        StackPane.setMargin(btn, new Insets(0, 0, 14, 62));
        rootStack.getChildren().add(btn);
    }

    private void toggle() {
        if (overlay != null && rootStack.getChildren().contains(overlay)) {
            // Toggle off
            rootStack.getChildren().remove(overlay);
            overlay = null;
            return;
        }

        final int[] index = {0};

        // Small size: visible but compact, sitting just above the SC button
        double cardW = 130.0;
        double cardH = cardW * BoardRenderer.CARD_ASPECT;

        ImageView iv = new ImageView();
        iv.setFitWidth(cardW);
        iv.setFitHeight(cardH);
        iv.setPreserveRatio(false);
        Rectangle clip = new Rectangle(cardW, cardH);
        clip.setArcWidth(cardW * 0.12);
        clip.setArcHeight(cardW * 0.12);
        iv.setClip(clip);

        Label fb = new Label("Summary Card");
        fb.setStyle("-fx-text-fill: #f5deb3; -fx-background-color: #3a2410;"
                + " -fx-background-radius: 8; -fx-padding: 8; -fx-font-size: 13px;");
        fb.setVisible(false);

        StackPane cardPane = new StackPane(iv, fb);
        cardPane.setMinSize(cardW, cardH);
        cardPane.setMaxSize(cardW, cardH);

        Runnable updateImage = () -> {
            Image img = FxUtil.image(() -> new Image(getClass().getResourceAsStream(CARD_PATHS[index[0]])));
            if (img != null) {
                iv.setImage(img);
                iv.setVisible(true);
                fb.setVisible(false);
            } else {
                iv.setVisible(false);
                fb.setVisible(true);
            }
        };
        updateImage.run();

        Button nextBtn = new Button("▶");
        nextBtn.setFocusTraversable(false);
        nextBtn.setStyle("-fx-font-size: 14px; -fx-padding: 6 10; -fx-background-radius: 15;");
        nextBtn.setOnAction(_ -> {
            index[0] = (index[0] + 1) % CARD_PATHS.length;
            updateImage.run();
        });

        HBox container = new HBox(8, cardPane, nextBtn);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPickOnBounds(false);
        container.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        overlay = container;

        // Anchor bottom-left, just above the SC button (button bottom=14 + height=40 + gap=8 = 62)
        StackPane.setAlignment(overlay, Pos.BOTTOM_LEFT);
        StackPane.setMargin(overlay, new Insets(0, 0, 62, 14));

        rootStack.getChildren().add(overlay);
    }
}
