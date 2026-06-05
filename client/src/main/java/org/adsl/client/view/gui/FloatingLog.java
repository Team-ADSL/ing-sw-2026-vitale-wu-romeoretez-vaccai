package org.adsl.client.view.gui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

import java.util.ArrayList;
import java.util.List;

/**
 * Bottom-right log used across screens. Collapsed it is a single gold "chat"
 * toggle button tucked into the very bottom-right corner; clicking it expands
 * the log in place into a scrollable stack of every message, each shown with
 * the same card style. The list scrolls only when it would overflow the
 * available vertical space. Once open it stays open until the user closes it
 * with the ✕ chip — clicking elsewhere does not collapse it.
 * Wire it by adding {@link #getFloatingNode()} as a child of the log container.
 * {@link #getFullPanel()} is kept for backward compatibility and is a no-op.
 */
public final class FloatingLog {

    private static final double MAX_W = 420;
    private static final double VERTICAL_MARGIN = 120;
    private static final double COLLAPSED_MAX_H = 300;

    /** Tight inset so the collapsed toggle sits as far into the corner as possible. */
    private static final Insets COLLAPSED_PAD = new Insets(6, 10, 10, 6);
    /** Original inset for the expanded panel — kept unchanged. */
    private static final Insets EXPANDED_PAD = new Insets(6, 20, 20, 6);

    private static final List<String> history = new ArrayList<>();
    private static FloatingLog active;
    private final VBox floating;
    private final StackPane fullPanel;
    private final Button toggleButton;
    private final VBox expandedBody;
    private final VBox expandedMessages;
    private final ScrollPane expandedScroll;

    private boolean expanded = false;
    private boolean scrollCapBound = false;
    private Runnable onToggleCallback;

    public void setOnToggle(Runnable callback) {
        this.onToggleCallback = callback;
    }

    public FloatingLog() {
        floating = new VBox(2);
        floating.setAlignment(Pos.BOTTOM_RIGHT);
        floating.setMaxWidth(MAX_W);
        floating.setMaxHeight(COLLAPSED_MAX_H);
        floating.setPickOnBounds(false);
        floating.setMouseTransparent(false);
        floating.setPadding(COLLAPSED_PAD);
        StackPane.setAlignment(floating, Pos.BOTTOM_RIGHT);

        // Collapsed state: a gold chat-style toggle (reuses the shared ".button"
        // style so it matches the other gold toggles).
        toggleButton = new Button();
        toggleButton.setGraphic(chatIcon());
        toggleButton.setFocusTraversable(true);
        toggleButton.setStyle("-fx-padding: 6;");
        toggleButton.setOnAction(_ -> expand());

        // Collapse on ESCAPE key
        floating.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE && expanded) {
                collapse();
                toggleButton.requestFocus();
                e.consume();
            }
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
        closeChip.setFont(ImageCatalog.robotoFont(12));
        closeChip.setStyle("-fx-text-fill: #1a0808; -fx-font-size: 12px;"
                + " -fx-background-color: #F2B035; -fx-padding: 4 12 4 12;"
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

        active = this;
        refresh();
    }

    public VBox getFloatingNode() { return floating; }
    public StackPane getFullPanel() { return fullPanel; }
    public Button getToggleButton() { return toggleButton; }

    /**
     * Replaces the shared transcript with {@code entries} (server-authoritative,
     * used on reconnect) and refreshes the live widget. Replacing rather than
     * appending avoids duplicates on a same-process reconnect. Must run on the
     * JavaFX thread (callers dispatch via {@code Platform.runLater}).
     */
    public static void restore(List<String> entries) {
        history.clear();
        if (entries != null) {
            for (String e : entries) {
                if (e != null && !e.isBlank()) history.add(e);
            }
        }
        if (active != null) {
            if (active.expanded) active.refillExpanded();
            else active.refresh();
        }
    }

    public void append(String text) {
        if (text == null || text.isBlank()) return;
        history.add(text);
        if (expanded) {
            refillExpanded();
            Platform.runLater(() -> expandedScroll.setVvalue(1.0));
        }
        // Collapsed state is a static toggle button — nothing to re-render.
    }

    private void expand() {
        expanded = true;
        floating.setPadding(EXPANDED_PAD);
        floating.setMaxHeight(Double.MAX_VALUE);
        floating.getChildren().setAll(expandedBody);
        refillExpanded();
        applyScrollCap();
        bindScrollCapToHeight();
        Platform.runLater(() -> expandedScroll.setVvalue(1.0));
        if (onToggleCallback != null) onToggleCallback.run();
    }

    /** Closes the log. Only ever called from the ✕ chip — clicking elsewhere keeps it open. */
    private void collapse() {
        expanded = false;
        floating.setMaxHeight(COLLAPSED_MAX_H);
        refresh();
        if (onToggleCallback != null) onToggleCallback.run();
    }

    /** Cap scroll viewport to available vertical space so the bar appears only on overflow. */
    private void applyScrollCap() {
        Scene scene = floating.getScene();
        double available = (scene != null ? scene.getHeight() : 600) - VERTICAL_MARGIN;
        expandedScroll.setMaxHeight(Math.max(120, available));
    }

    /** Re-cap the scroll viewport when the window is resized while open (bound once). */
    private void bindScrollCapToHeight() {
        Scene scene = floating.getScene();
        if (scene == null || scrollCapBound) return;
        scene.heightProperty().addListener((_, _, _) -> { if (expanded) applyScrollCap(); });
        scrollCapBound = true;
    }

    /** Collapsed view: just the gold toggle button in the corner. */
    private void refresh() {
        floating.setPadding(COLLAPSED_PAD);
        floating.getChildren().setAll(toggleButton);
    }

    private void refillExpanded() {
        expandedMessages.getChildren().clear();
        for (String s : history) {
            expandedMessages.getChildren().add(card(s));
        }
    }

    private Label card(String text) {
        Label entry = new Label(text);
        entry.setWrapText(true);
        entry.setMaxWidth(MAX_W - 20);
        entry.setFont(ImageCatalog.robotoFont(13));
        entry.setStyle("-fx-text-fill: #f5deb3; -fx-font-size: 13px;"
                + " -fx-background-color: rgba(0,0,0,0.40); -fx-padding: 5 12 5 12;"
                + " -fx-background-radius: 8;");
        return entry;
    }

    /** Material "chat bubble" glyph (24×24), tinted to match the gold button text. */
    private static SVGPath chatIcon() {
        SVGPath icon = new SVGPath();
        icon.setContent("M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z");
        icon.setFill(Color.web("#1a0808"));
        icon.setScaleX(0.82);
        icon.setScaleY(0.82);
        return icon;
    }
}
