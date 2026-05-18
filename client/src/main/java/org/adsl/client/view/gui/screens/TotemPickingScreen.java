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
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
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

    private FlowPane totemRow;
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

    private StackPane buildUI() {
        VBox outer = new VBox(28);
        outer.setAlignment(Pos.CENTER);
        outer.setPadding(new Insets(50));
        outer.setStyle("-fx-background-color: transparent;");

        Label title = new Label("M E S O S  —  Choose Your Totem");
        title.setFont(ImageCatalog.chalkFont(42));
        title.setStyle("-fx-text-fill: #FDF3D3;");

        Label playerLabel = new Label("Player: " + username);
        playerLabel.setFont(ImageCatalog.chalkFont(24));
        playerLabel.setStyle("-fx-text-fill: #F2B035;");

        statusLabel = new Label("Select a totem, then press CONFIRM or ENTER");
        statusLabel.setFont(ImageCatalog.chalkFont(19));
        statusLabel.setStyle("-fx-text-fill: #FDF3D3;");

        totemRow = new FlowPane(24, 24);
        totemRow.setAlignment(Pos.CENTER);
        populateTotemRow();

        confirmButton = new Button("C O N F I R M");
        confirmButton.setFont(ImageCatalog.chalkFont(19));
        applyConfirmStyle(false);
        confirmButton.setOnAction(e -> onConfirm());
        confirmButton.setOnMouseEntered(_ -> { if (!confirmButton.isDisabled()) applyConfirmHover(); });
        confirmButton.setOnMouseExited(_ ->  { if (!confirmButton.isDisabled()) applyConfirmStyle(false); });

        errorLabel = new Label("");
        errorLabel.setFont(ImageCatalog.chalkFont(15));
        errorLabel.setStyle("-fx-text-fill: #D92938;");
        errorLabel.setWrapText(true);

        outer.getChildren().addAll(title, playerLabel, statusLabel, totemRow, confirmButton, errorLabel);

        StackPane rootPane = new StackPane(outer);
        rootPane.setFocusTraversable(true);
        rootPane.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) onConfirm(); });
        applyTheme(rootPane);

        // Dark overlay matching .panel opacity — sits between bg image and content.
        Rectangle overlay = new Rectangle();
        overlay.setFill(Color.rgb(0, 0, 0, 0.28));
        overlay.widthProperty().bind(rootPane.widthProperty());
        overlay.heightProperty().bind(rootPane.heightProperty());
        StackPane.setAlignment(overlay, Pos.TOP_LEFT);
        rootPane.getChildren().add(1, overlay);

        return rootPane;
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
            container.setOnMouseEntered(_ -> { if (!isSelected(t)) scale(container, HOVER_SCALE); });
            container.setOnMouseExited(_ ->  { if (!isSelected(t)) scale(container, 1.0); });
            container.setOnMouseClicked(_ -> onTotemClicked(t));
        }

        return container;
    }

    private void applyVisuals(StackPane container, ImageView iv, Label name,
                               boolean available, boolean selected, boolean ourPick, Totem t) {
        if (ourPick || (selected && available)) {
            iv.setEffect(null);
            name.setStyle("-fx-text-fill: #FDF3D3;");
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
            name.setStyle("-fx-text-fill: #FDF3D3;");
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
            statusLabel.setStyle("-fx-text-fill: #F2B035;");
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
        statusLabel.setStyle("-fx-text-fill: #FDF3D3;");
        confirmButton.setDisable(false);
        applyConfirmStyle(false);
        populateTotemRow();
        return this;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void showPickedState() {
        statusLabel.setText("Waiting for other players to choose...");
        statusLabel.setStyle("-fx-text-fill: #F2B035;");
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
        String bg = hover ? "#F25835" : "#F2B035";
        confirmButton.setStyle(
            "-fx-background-color: " + bg + "; -fx-text-fill: #1a0808; " +
            "-fx-background-radius: 12; -fx-padding: 12 44 12 44; -fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 6, 0.25, 0, 2);"
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
