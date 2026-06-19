package org.adsl.client.view.gui.game;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import org.adsl.client.view.gui.ImageCatalog;

import java.util.List;

/**
 * The "?" rules button (bottom-left of the board) and the full-screen rules
 * viewer it opens: a paged image browser over the rulebook PNGs with prev/next
 * and a close chip. Self-installs its trigger button into the supplied root
 * stack; the overlay is added/removed on demand.
 */
public final class RulesOverlay {

    private final StackPane rootStack;
    private List<Image> pages;
    private int pageIndex = 0;
    private StackPane overlay;

    public RulesOverlay(StackPane rootStack) {
        this.rootStack = rootStack;
        installButton();
    }

    private void installButton() {
        Button btn = new Button("?");
        btn.setStyle(
            "-fx-font-size: 18px; -fx-font-weight: bold;" +
            "-fx-min-width: 40px; -fx-min-height: 40px;" +
            "-fx-max-width: 40px; -fx-max-height: 40px;" +
            "-fx-background-radius: 20; -fx-padding: 0;"
        );
        btn.setOnAction(_ -> open());
        StackPane.setAlignment(btn, Pos.BOTTOM_LEFT);
        StackPane.setMargin(btn, new Insets(0, 0, 14, 14));
        rootStack.getChildren().add(btn);
    }

    private void open() {
        if (pages == null) pages = ImageCatalog.rulesPages();
        if (pages.isEmpty()) return;

        pageIndex = 0;
        overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.88);");

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(overlay.widthProperty());
        clip.heightProperty().bind(overlay.heightProperty());
        overlay.setClip(clip);

        ImageView iv = new ImageView(pages.get(0));
        iv.setPreserveRatio(true);
        iv.fitWidthProperty().bind(overlay.widthProperty().multiply(0.88));
        iv.fitHeightProperty().bind(overlay.heightProperty().multiply(0.88));

        Button prev = new Button("◀");
        Button next = new Button("▶");
        Button close = new Button("✕");
        close.setStyle("-fx-font-size: 14px; -fx-min-width: 34px; -fx-min-height: 34px;" +
                       "-fx-max-width: 34px; -fx-max-height: 34px; -fx-background-radius: 17; -fx-padding: 0;");

        Label counter = new Label("1 / " + pages.size());
        counter.setStyle("-fx-text-fill: #FDF3D3; -fx-font-size: 14px;");

        prev.setOnAction(_ -> {
            if (pageIndex > 0) {
                pageIndex--;
                iv.setImage(pages.get(pageIndex));
                counter.setText((pageIndex + 1) + " / " + pages.size());
            }
        });
        next.setOnAction(_ -> {
            if (pageIndex < pages.size() - 1) {
                pageIndex++;
                iv.setImage(pages.get(pageIndex));
                counter.setText((pageIndex + 1) + " / " + pages.size());
            }
        });
        close.setOnAction(_ -> rootStack.getChildren().remove(overlay));
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) rootStack.getChildren().remove(overlay);
        });

        HBox nav = new HBox(16, prev, counter, next);
        nav.setAlignment(Pos.CENTER);

        VBox content = new VBox(12, iv, nav);
        content.setAlignment(Pos.CENTER);
        content.setPickOnBounds(false);

        StackPane.setAlignment(close, Pos.TOP_RIGHT);
        StackPane.setMargin(close, new Insets(12, 12, 0, 0));

        overlay.getChildren().addAll(content, close);
        rootStack.getChildren().add(overlay);
    }
}
