package org.adsl.client.view.gui;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Bottom-right log used across screens. Collapsed it shows the last N messages
 * with an opacity gradient (older = more transparent). Clicking expands it
 * in place: the gradient is removed and every message is shown using the same
 * card style, stacked one above the other in the same spot. The list scrolls
 * only when it would overflow the available vertical space. Closing happens via
 * the ✕ chip or by clicking outside the log area.
 *
 * Wire it by adding {@link #getFloatingNode()} as a child of the log container.
 * {@link #getFullPanel()} is kept for backward compatibility and is a no-op.
 */
public final class FloatingLog {

    private static final int VISIBLE = 5;
    private static final double MAX_W = 320;
    private static final double VERTICAL_MARGIN = 120;

    private static final List<String> history = new ArrayList<>();
    private final VBox floating;
    private final StackPane fullPanel;
    private final VBox expandedBody;
    private final VBox expandedMessages;
    private final ScrollPane expandedScroll;

    private boolean expanded = false;
    private EventHandler<MouseEvent> outsideClickFilter;
    private Scene filterScene;

    public FloatingLog(String title) {
        floating = new VBox(2);
        floating.setAlignment(Pos.BOTTOM_RIGHT);
        floating.setMaxWidth(MAX_W);
        floating.setMaxHeight(220);
        floating.setPickOnBounds(false);
        floating.setMouseTransparent(false);
        floating.setCursor(Cursor.HAND);
        floating.setPadding(new Insets(6, 20, 20, 6));
        StackPane.setAlignment(floating, Pos.BOTTOM_RIGHT);
        floating.setOnMouseClicked(e -> {
            if (!expanded) expand();
        });

        // Backward-compat no-op node (no longer shown).
        fullPanel = new StackPane();
        fullPanel.setVisible(false);
        fullPanel.setMouseTransparent(true);

        // Expanded in-place body: scrollable stack of message cards + close chip.
        expandedMessages = new VBox(2);
        expandedMessages.setAlignment(Pos.BOTTOM_RIGHT);

        expandedScroll = new ScrollPane(expandedMessages);
        expandedScroll.setFitToWidth(true);
        expandedScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        expandedScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        expandedScroll.setMaxWidth(MAX_W);

        Label closeChip = new Label("✕");
        closeChip.setFont(ImageCatalog.robotoFont(9));
        closeChip.setStyle("-fx-text-fill: #1a0808; -fx-font-size: 9px;"
                + " -fx-background-color: #F2B035; -fx-padding: 3 10 3 10;"
                + " -fx-background-radius: 8; -fx-cursor: hand;");
        closeChip.setOnMouseClicked(e -> {
            e.consume();
            collapse();
        });
        VBox closeRow = new VBox(closeChip);
        closeRow.setAlignment(Pos.BOTTOM_RIGHT);

        expandedBody = new VBox(4, expandedScroll, closeRow);
        expandedBody.setAlignment(Pos.BOTTOM_RIGHT);
        expandedBody.setMaxWidth(MAX_W);
        VBox.setVgrow(expandedScroll, Priority.ALWAYS);

        refresh();
    }

    public VBox getFloatingNode() { return floating; }
    public StackPane getFullPanel() { return fullPanel; }

    public void append(String text) {
        if (text == null || text.isBlank()) return;
        history.add(text);
        if (expanded) {
            refillExpanded();
            Platform.runLater(() -> expandedScroll.setVvalue(1.0));
        } else {
            refresh();
        }
    }

    private void expand() {
        expanded = true;
        floating.setCursor(Cursor.DEFAULT);
        floating.setMaxHeight(Double.MAX_VALUE);
        floating.getChildren().setAll(expandedBody);
        refillExpanded();
        applyScrollCap();
        installOutsideClickFilter();
        Platform.runLater(() -> expandedScroll.setVvalue(1.0));
    }

    private void collapse() {
        expanded = false;
        floating.setCursor(Cursor.HAND);
        floating.setMaxHeight(220);
        removeOutsideClickFilter();
        refresh();
    }

    /** Cap scroll viewport to available vertical space so the bar appears only on overflow. */
    private void applyScrollCap() {
        Scene scene = floating.getScene();
        double available = (scene != null ? scene.getHeight() : 600) - VERTICAL_MARGIN;
        expandedScroll.setMaxHeight(Math.max(120, available));
    }

    private void installOutsideClickFilter() {
        Scene scene = floating.getScene();
        if (scene == null) return;
        outsideClickFilter = e -> {
            if (!expanded) return;
            Node n = e.getPickResult().getIntersectedNode();
            while (n != null) {
                if (n == floating) return;
                n = n.getParent();
            }
            collapse();
        };
        filterScene = scene;
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, outsideClickFilter);
        scene.heightProperty().addListener((o, a, b) -> { if (expanded) applyScrollCap(); });
    }

    private void removeOutsideClickFilter() {
        if (filterScene != null && outsideClickFilter != null) {
            filterScene.removeEventFilter(MouseEvent.MOUSE_PRESSED, outsideClickFilter);
        }
        outsideClickFilter = null;
        filterScene = null;
    }

    private void refresh() {
        floating.getChildren().clear();
        int n = history.size();
        int from = Math.max(0, n - VISIBLE);
        List<String> recent = history.subList(from, n);
        int count = recent.size();
        for (int i = 0; i < count; i++) {
            double frac = (i + 1) / (double) count;
            double opacity = 0.15 + frac * 0.85;
            floating.getChildren().add(card(recent.get(i), opacity));
        }
    }

    private void refillExpanded() {
        expandedMessages.getChildren().clear();
        for (String s : history) {
            expandedMessages.getChildren().add(card(s, 1.0));
        }
    }

    private Label card(String text, double opacity) {
        Label entry = new Label(text);
        entry.setWrapText(true);
        entry.setMaxWidth(MAX_W - 20);
        entry.setFont(ImageCatalog.robotoFont(9));
        entry.setStyle("-fx-text-fill: #f5deb3; -fx-font-size: 9px;"
                + " -fx-background-color: rgba(0,0,0,0.40); -fx-padding: 3 10 3 10;"
                + " -fx-background-radius: 8;");
        entry.setOpacity(opacity);
        return entry;
    }
}
