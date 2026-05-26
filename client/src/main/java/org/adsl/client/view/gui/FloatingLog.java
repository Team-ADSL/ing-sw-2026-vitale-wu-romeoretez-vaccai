package org.adsl.client.view.gui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Bottom-right fading log used across screens. Shows last N messages with
 * opacity gradient (older = more transparent). Click opens a full-history
 * panel with a close button.
 *
 * Wire it by adding {@link #getFloatingNode()} and {@link #getFullPanel()} as
 * children of a {@link StackPane} root, both aligned to BOTTOM_RIGHT.
 */
public final class FloatingLog {

    private static final int VISIBLE = 5;
    private static final double MAX_W = 320;

    private final List<String> history = new ArrayList<>();
    private final VBox floating;
    private final StackPane fullPanel;
    private final VBox fullMessages;
    private final ScrollPane fullScroll;
    private final String title;

    public FloatingLog(String title) {
        this.title = title;

        floating = new VBox(2);
        floating.setAlignment(Pos.BOTTOM_RIGHT);
        floating.setMaxWidth(MAX_W);
        floating.setMaxHeight(220);
        floating.setPickOnBounds(false);
        floating.setMouseTransparent(false);
        floating.setCursor(Cursor.HAND);
        floating.setPadding(new Insets(6, 20, 20, 6));
        StackPane.setAlignment(floating, Pos.BOTTOM_RIGHT);
        floating.setOnMouseClicked(e -> openFull());

        fullPanel = new StackPane();
        fullPanel.setPickOnBounds(false);
        fullPanel.setVisible(false);
        StackPane.setAlignment(fullPanel, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(fullPanel, new Insets(0, 20, 20, 0));

        VBox panelBody = new VBox(6);
        panelBody.setStyle("-fx-background-color: rgba(20, 12, 6, 0.95); -fx-background-radius: 12;"
                + " -fx-border-color: #8b5e3c; -fx-border-width: 1; -fx-border-radius: 12;");
        panelBody.setPrefSize(420, 480);
        panelBody.setMaxSize(420, 480);
        panelBody.setPadding(new Insets(10));

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label(title);
        titleLabel.setFont(ImageCatalog.robotoFont(14));
        titleLabel.setStyle("-fx-text-fill: #c8b080; -fx-font-size: 14px; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button close = new Button("✕");
        close.setStyle("-fx-background-color: transparent; -fx-text-fill: #f5deb3;"
                + " -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2 8 2 8;");
        close.setOnAction(e -> fullPanel.setVisible(false));
        header.getChildren().addAll(titleLabel, spacer, close);

        fullScroll = new ScrollPane();
        fullScroll.setFitToWidth(true);
        fullScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(fullScroll, Priority.ALWAYS);
        fullMessages = new VBox(4);
        fullMessages.setPadding(new Insets(4));
        fullScroll.setContent(fullMessages);

        panelBody.getChildren().addAll(header, fullScroll);
        fullPanel.getChildren().add(panelBody);
    }

    public VBox getFloatingNode() { return floating; }
    public StackPane getFullPanel() { return fullPanel; }

    public void append(String text) {
        if (text == null || text.isBlank()) return;
        history.add(text);
        refresh();
        if (fullPanel.isVisible()) refillFull();
    }

    private void refresh() {
        floating.getChildren().clear();
        int n = history.size();
        int from = Math.max(0, n - VISIBLE);
        List<String> recent = history.subList(from, n);
        int count = recent.size();
        for (int i = 0; i < count; i++) {
            String text = recent.get(i);
            double frac = (i + 1) / (double) count;
            double opacity = 0.15 + frac * 0.85;
            Label entry = new Label(text);
            entry.setWrapText(true);
            entry.setMaxWidth(MAX_W - 20);
            entry.setFont(ImageCatalog.robotoFont(9));
            entry.setStyle("-fx-text-fill: #f5deb3; -fx-font-size: 9px;"
                    + " -fx-background-color: rgba(0,0,0,0.40); -fx-padding: 3 10 3 10;"
                    + " -fx-background-radius: 8;");
            entry.setOpacity(opacity);
            floating.getChildren().add(entry);
        }
    }

    private void openFull() {
        if (fullPanel.isVisible()) return;
        refillFull();
        fullPanel.setVisible(true);
        Platform.runLater(() -> fullScroll.setVvalue(1.0));
    }

    private void refillFull() {
        fullMessages.getChildren().clear();
        for (String s : history) {
            Label l = new Label(s);
            l.setWrapText(true);
            l.setFont(ImageCatalog.robotoFont(13));
            l.setStyle("-fx-text-fill: #f5deb3; -fx-font-size: 13px;");
            fullMessages.getChildren().add(l);
        }
    }
}
