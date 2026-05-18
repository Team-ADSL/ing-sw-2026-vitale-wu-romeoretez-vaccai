package org.adsl.client.view.gui.screens;

import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.adsl.client.AppCoordinator;
import org.adsl.client.serverEvents.ErrorEvent;
import org.adsl.client.serverEvents.TotemAvailableEvent;
import org.adsl.client.view.gui.ImageCatalog;
import org.adsl.shared.enums.Totem;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI screen for the totem-picking phase. Displays totem images in a row;
 * greyed-out totems are already taken. The player clicks to select and confirms
 * with the button or ENTER key. Updates on {@code TotemAvailableEvent}.
 */
public class TotemPickingScreen extends GUIScreen {

    private static final Totem[] ALL_TOTEMS = Totem.values();
    private static final double TOTEM_SIZE   = 140.0;
    private static final double HOVER_SCALE  = 1.18;
    private static final Duration ANIM       = Duration.millis(150);

    private List<Totem> availableTotems;
    private Totem selectedTotem = null;
    private boolean pendingPick = false;
    private boolean hasPicked   = false;

    private HBox   totemRow;
    private Label  statusLabel;
    private Label  errorLabel;
    private Button confirmButton;

    public TotemPickingScreen(AppCoordinator coordinator, String username,
                              List<Totem> availableTotems) {
        super(coordinator, username);
        this.availableTotems = availableTotems != null
                ? new ArrayList<>(availableTotems) : new ArrayList<>();
        this.root = buildUI();
    }

    @Override
    public GUIScreen onEnter() {
        root.requestFocus();
        return null;
    }

    // ── UI construction ────────────────────────────────────────────────────────

    private VBox buildUI() {
        VBox outer = new VBox(28);
        outer.setAlignment(Pos.CENTER);
        outer.setPadding(new Insets(50));
        outer.setStyle("-fx-background-color: #2c1a0e;");
        outer.setFocusTraversable(true);
        outer.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) onConfirm(); });

        Label title = new Label("M E S O S  —  Choose Your Totem");
        title.setFont(ImageCatalog.chalkFont(34));
        title.setStyle("-fx-text-fill: #f5deb3;");

        Label playerLabel = new Label("Player: " + username);
        playerLabel.setFont(ImageCatalog.chalkFont(19));
        playerLabel.setStyle("-fx-text-fill: #b8946c;");

        statusLabel = new Label("Select a totem, then press CONFIRM or ENTER");
        statusLabel.setFont(ImageCatalog.chalkFont(17));
        statusLabel.setStyle("-fx-text-fill: #c8b080;");

        totemRow = new HBox(40);
        totemRow.setAlignment(Pos.CENTER);
        populateTotemRow();

        confirmButton = new Button("C O N F I R M");
        confirmButton.setFont(ImageCatalog.chalkFont(19));
        applyConfirmStyle(false);
        confirmButton.setOnAction(e -> onConfirm());
        confirmButton.setOnMouseEntered(e -> { if (!confirmButton.isDisabled()) applyConfirmHover(); });
        confirmButton.setOnMouseExited(e ->  { if (!confirmButton.isDisabled()) applyConfirmStyle(false); });

        errorLabel = new Label("");
        errorLabel.setFont(ImageCatalog.chalkFont(15));
        errorLabel.setStyle("-fx-text-fill: #e53935;");
        errorLabel.setWrapText(true);

        outer.getChildren().addAll(title, playerLabel, statusLabel, totemRow, confirmButton, errorLabel);
        return outer;
    }

    private void populateTotemRow() {
        totemRow.getChildren().clear();
        for (Totem t : ALL_TOTEMS) {
            totemRow.getChildren().add(buildCell(t));
        }
    }

    private StackPane buildCell(Totem t) {
        boolean available  = availableTotems.contains(t);
        boolean selected   = (t == selectedTotem);
        boolean ourPick    = hasPicked && selected;

        ImageView iv = new ImageView(ImageCatalog.totem2D(t));
        iv.setFitWidth(TOTEM_SIZE);
        iv.setFitHeight(TOTEM_SIZE);
        iv.setPreserveRatio(true);

        Label name = new Label(t.name());
        name.setFont(ImageCatalog.chalkFont(14));

        VBox inner = new VBox(8, iv, name);
        inner.setAlignment(Pos.CENTER);

        StackPane container = new StackPane(inner);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(12));

        applyVisuals(container, iv, name, available, selected, ourPick, t);

        if (selected) {
            container.setScaleX(HOVER_SCALE);
            container.setScaleY(HOVER_SCALE);
        }

        if (!hasPicked && available) {
            container.setStyle("-fx-cursor: hand;");
            container.setOnMouseEntered(e -> { if (!isSelected(t)) scale(container, HOVER_SCALE); });
            container.setOnMouseExited(e ->  { if (!isSelected(t)) scale(container, 1.0); });
            container.setOnMouseClicked(e -> onTotemClicked(t));
        }

        return container;
    }

    private void applyVisuals(StackPane container, ImageView iv, Label name,
                               boolean available, boolean selected, boolean ourPick, Totem t) {
        if (ourPick || (selected && available)) {
            iv.setEffect(null);
            name.setStyle("-fx-text-fill: #ffffff;");
            DropShadow glow = new DropShadow(28, glowColor(t));
            glow.setSpread(0.35);
            container.setEffect(glow);
        } else if (!available) {
            ColorAdjust gray = new ColorAdjust();
            gray.setSaturation(-1.0);
            gray.setBrightness(-0.2);
            iv.setEffect(gray);
            name.setStyle("-fx-text-fill: #555555;");
            container.setEffect(null);
        } else {
            iv.setEffect(null);
            name.setStyle("-fx-text-fill: #c8b080;");
            container.setEffect(null);
        }
    }

    // ── Interaction ────────────────────────────────────────────────────────────

    private void onTotemClicked(Totem t) {
        if (hasPicked || pendingPick) return;
        selectedTotem = (selectedTotem == t) ? null : t;
        errorLabel.setText("");
        populateTotemRow();
    }

    private void onConfirm() {
        if (hasPicked || pendingPick) return;
        if (selectedTotem == null) {
            errorLabel.setText("Select a totem first.");
            return;
        }
        try {
            appCoordinator.createTotemPickingRequest(selectedTotem);
            pendingPick = true;
            confirmButton.setDisable(true);
            statusLabel.setText("Waiting for server confirmation...");
            statusLabel.setStyle("-fx-text-fill: #f5a623;");
            errorLabel.setText("");
        } catch (Exception ex) {
            errorLabel.setText("Request failed: " + ex.getMessage());
        }
    }

    // ── Server events ──────────────────────────────────────────────────────────

    @Override
    public GUIScreen visit(TotemAvailableEvent event) {
        List<Totem> newList = event.totemList() != null
                ? new ArrayList<>(event.totemList()) : new ArrayList<>();
        if (pendingPick && selectedTotem != null && !newList.contains(selectedTotem)) {
            hasPicked = true;
            pendingPick = false;
            availableTotems = newList;
            showPickedState();
        } else {
            availableTotems = newList;
            if (!hasPicked) populateTotemRow();
        }
        return this;
    }

    @Override
    public GUIScreen visit(ErrorEvent e) {
        pendingPick = false;
        errorLabel.setText(e.message());
        statusLabel.setText("Select a totem, then press CONFIRM or ENTER");
        statusLabel.setStyle("-fx-text-fill: #c8b080;");
        confirmButton.setDisable(false);
        applyConfirmStyle(false);
        populateTotemRow();
        return this;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void showPickedState() {
        statusLabel.setText("Waiting for other players to choose...");
        statusLabel.setStyle("-fx-text-fill: #87ceeb;");
        confirmButton.setVisible(false);
        populateTotemRow();
    }

    private boolean isSelected(Totem t) { return t == selectedTotem; }

    private void scale(StackPane node, double to) {
        ScaleTransition st = new ScaleTransition(ANIM, node);
        st.setToX(to);
        st.setToY(to);
        st.play();
    }

    private void applyConfirmStyle(boolean hover) {
        String bg = hover ? "#a5714e" : "#8b5e3c";
        confirmButton.setStyle(
            "-fx-background-color: " + bg + "; -fx-text-fill: #f5deb3; " +
            "-fx-background-radius: 8; -fx-padding: 12 44 12 44; -fx-cursor: hand;"
        );
    }

    private void applyConfirmHover() { applyConfirmStyle(true); }

    private Color glowColor(Totem t) {
        return switch (t) {
            case RED    -> Color.rgb(220, 50,  50);
            case WHITE  -> Color.rgb(240, 240, 240);
            case BLACK  -> Color.rgb(80, 0, 120);
            case BLUE   -> Color.rgb(70,  130, 220);
            case YELLOW -> Color.rgb(230, 200, 50);
        };
    }
}
