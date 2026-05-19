package org.adsl.client.view.gui.screens;

import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.adsl.client.AppCoordinator;
import org.adsl.client.serverEvents.ErrorEvent;
import org.adsl.client.serverEvents.EventsTriggeredEvent;
import org.adsl.client.serverEvents.GameUpdateEvent;
import org.adsl.client.view.gui.Chip;
import org.adsl.client.view.gui.FloatingLog;
import org.adsl.client.view.gui.ImageCatalog;
import org.adsl.shared.enums.Phase;
import org.adsl.shared.enums.Row;
import org.adsl.shared.enums.Totem;
import org.adsl.shared.model.CardDTO;
import org.adsl.shared.model.GameDTO;
import org.adsl.shared.model.OfferTileDTO;
import org.adsl.shared.model.PlayerDTO;
import org.adsl.shared.utils.Move;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Game screen — minimal, centered, responsive.
 *
 * Cards float without containers, rounded corners; hover scales up; selected
 * cards stay scaled with a white glow. Offer-track tiles are flush (no gap)
 * so they compose a continuous image. Card rows scale dynamically so every
 * row stays single-line at any window size.
 *
 * The game log sits bottom-right, showing only the most recent messages with
 * a fade-up gradient; clicking it opens a full-chat panel.
 */
public class GameScreen extends GUIScreen {

    private static final double CARD_ASPECT  = 1.484;
    private static final double CARD_MAX_W   = 130.0;
    private static final double CARD_MIN_W   = 55.0;
    private static final double CARD_GAP     = 10.0;
    private static final double TILE_ASPECT  = 1.65;
    private static final double TILE_MAX_W   = 110.0;
    private static final double TILE_MIN_W   = 50.0;
    private static final double HOVER_SCALE  = 1.15;
    private static final Duration ANIM       = Duration.millis(140);
    private static final double CHIP_WIDTH   = 40;
    private static final double TOTEM_BADGE  = 22;

    @FXML private StackPane rootStack;
    @FXML private VBox      contentBox;
    @FXML private Label     headerLabel;
    @FXML private Label     phaseLabel;
    @FXML private HBox      topRow;
    @FXML private HBox      offerTrack;
    @FXML private HBox      bottomRow;
    @FXML private Label     tribeLabel;
    @FXML private Label     tribeContents;
    @FXML private VBox      othersBox;
    @FXML private Label     hintLabel;
    @FXML private Button    confirmButton;
    @FXML private Label     errorLabel;
    @FXML private VBox      logBox;

    private GameDTO game;
    private final Set<Move> selectedMoves = new LinkedHashSet<>();
    private int upperCount = 0;
    private int lowerCount = 0;
    private boolean waitingServer = false;
    private final FloatingLog floatingLog;

    private StackPane overlayPane;
    private Label overlayTitle;
    private VBox overlayPlayerList;

    public GameScreen(AppCoordinator coordinator, String username, GameDTO game) {
        super(coordinator, username);
        this.game = game;
        Parent fxmlRoot;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/game.fxml"));
            loader.setController(this);
            fxmlRoot = loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load game.fxml", e);
        }
        applyTheme(fxmlRoot, false);

        this.root = fxmlRoot;
        rootStack.widthProperty().addListener((_, _, _) -> Platform.runLater(this::renderBoard));
        rootStack.heightProperty().addListener((_, _, _) -> Platform.runLater(this::renderBoard));
        rootStack.setFocusTraversable(true);
        rootStack.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER && confirmButton != null && !confirmButton.isDisabled()) {
                onSendMove();
            }
        });

        buildEventsOverlay();
        rootStack.getChildren().add(overlayPane);
        StackPane.setAlignment(overlayPane, Pos.CENTER);

        floatingLog = new FloatingLog("Game log");
        if (logBox != null) {
            logBox.getChildren().setAll(floatingLog.getFloatingNode());
            logBox.setPickOnBounds(false);
        } else {
            rootStack.getChildren().add(floatingLog.getFloatingNode());
        }
        rootStack.getChildren().add(floatingLog.getFullPanel());

        resolveMoveCounts();
        renderBoard();
    }

    @Override
    public GUIScreen onEnter() {
        rootStack.requestFocus();
        return null;
    }

    // ── Events overlay ───────────────────────────────────────────────────────

    private void buildEventsOverlay() {
        overlayPane = new StackPane();
        overlayPane.setStyle("-fx-background-color: rgba(239, 108, 0, 0.55);");
        overlayPane.setMouseTransparent(true);
        overlayPane.setVisible(false);

        overlayTitle = new Label("");
        overlayTitle.setStyle("-fx-text-fill: white; -fx-font-size: 64px; -fx-font-weight: bold;"
                + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.85), 8, 0.4, 0, 0);");
        overlayTitle.setAlignment(Pos.CENTER);

        overlayPlayerList = new VBox(6);
        overlayPlayerList.setAlignment(Pos.CENTER);

        VBox overlayContent = new VBox(24, overlayTitle, overlayPlayerList);
        overlayContent.setAlignment(Pos.CENTER);
        overlayPane.getChildren().add(overlayContent);
        StackPane.setAlignment(overlayContent, Pos.CENTER);
    }

    @Override
    public GUIScreen visit(EventsTriggeredEvent e) {
        String title = e.eventTitle();
        if (title == null || title.isBlank()) {
            return this;
        }
        String log = e.logMessage();
        Platform.runLater(() -> {
            showEventOverlay(title, log);
            if (log != null && !log.isBlank()) appendChat(log);
        });
        return this;
    }

    /**
     * Shows a single event title plus per-player delta lines parsed from the
     * server-side log message. Stays on screen until the next response
     * (another EventsTriggered or a GameUpdate) replaces or clears it.
     * AppCoordinator's pacer ensures a minimum gap between consecutive
     * arrivals.
     */
    private void showEventOverlay(String title, String logMessage) {
        if (overlayPane == null) return;
        overlayTitle.setText(title.toUpperCase());
        populateOverlayPlayerList(logMessage);
        overlayPane.setVisible(true);
    }

    private void populateOverlayPlayerList(String logMessage) {
        overlayPlayerList.getChildren().clear();
        if (logMessage == null || logMessage.isBlank()) return;
        int sep = logMessage.indexOf(" — ");
        if (sep < 0) return;
        String body = logMessage.substring(sep + " — ".length());
        for (String seg : body.split("\\s\\|\\s")) {
            String text = seg.trim();
            if (text.isEmpty()) continue;
            Label line = new Label(text);
            line.setStyle("-fx-text-fill: white; -fx-font-size: 26px; -fx-font-weight: 600;"
                    + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.75), 4, 0.4, 0, 0);");
            overlayPlayerList.getChildren().add(line);
        }
    }

    private void hideEventOverlay() {
        if (overlayPane != null) overlayPane.setVisible(false);
    }

    // ── Server events ────────────────────────────────────────────────────────

    @Override
    public GUIScreen visit(GameUpdateEvent e) {
        hideEventOverlay();
        this.game = e.game();
        waitingServer = false;
        if (game.phase() != Phase.ACTION_EXECUTION && game.phase() != Phase.EXTRA_MOVE) {
            selectedMoves.clear();
        }
        resolveMoveCounts();
        renderBoard();
        if (e.message() != null && !e.message().isBlank()) appendChat(e.message());
        return this;
    }

    @Override
    public GUIScreen visit(ErrorEvent e) {
        if (errorLabel != null) errorLabel.setText(e.message());
        if (waitingServer) {
            waitingServer = false;
            renderBoard();
        }
        return this;
    }

    private void appendChat(String text) {
        if (floatingLog != null) floatingLog.append(text);
    }

    // ── Input ────────────────────────────────────────────────────────────────

    @FXML
    private void onSendMove() {
        if (!isMyTurn()) {
            errorLabel.setText("Not your turn.");
            return;
        }
        Phase phase = game.phase();
        if (phase != Phase.ACTION_EXECUTION && phase != Phase.EXTRA_MOVE) {
            errorLabel.setText("Cannot confirm selection in this phase.");
            return;
        }
        Set<Move> toSend = new LinkedHashSet<>(selectedMoves);
        try {
            appCoordinator.makeMoveRequest(toSend);
            waitingServer = true;
            errorLabel.setText("");
            renderBoard();
        } catch (Exception ex) {
            errorLabel.setText("Move failed: " + ex.getMessage());
        }
    }

    private void onCardClicked(Row row, int idx, CardDTO card, StackPane pane) {
        if (!canPickCards()) return;
        if (card == null) {
            errorLabel.setText("That slot is empty.");
            return;
        }
        Move m = new Move(idx, row);
        if (!selectedMoves.add(m)) {
            selectedMoves.remove(m);
        }
        errorLabel.setText("");
        renderBoard();
    }

    private void onOfferTileClicked(int idx, OfferTileDTO tile) {
        if (!canPlaceTotem()) return;
        if (tile.totem() != null) {
            errorLabel.setText("That tile is already occupied!");
            return;
        }
        try {
            appCoordinator.makeMoveRequest(Set.of(new Move(idx, Row.OFFER)));
            waitingServer = true;
            errorLabel.setText("");
            renderBoard();
        } catch (Exception ex) {
            errorLabel.setText("Move failed: " + ex.getMessage());
        }
    }

    // ── Phase / turn helpers ─────────────────────────────────────────────────

    private boolean isMyTurn() {
        if (game == null || username == null) return false;
        PlayerDTO me = findMe();
        return me != null && me.totem() == game.currentPlayerTotem();
    }

    private boolean canPlaceTotem() {
        return !waitingServer && isMyTurn() && game.phase() == Phase.TOTEM_PLACEMENT;
    }

    private boolean canPickCards() {
        return !waitingServer && isMyTurn()
                && (game.phase() == Phase.ACTION_EXECUTION || game.phase() == Phase.EXTRA_MOVE);
    }

    private void resolveMoveCounts() {
        upperCount = 0;
        lowerCount = 0;
        if (game == null) return;
        Totem current = game.currentPlayerTotem();
        if (current == null || game.board() == null) return;
        for (OfferTileDTO tile : game.board().offerTrack()) {
            if (tile.totem() == current) {
                Map<Row, Integer> moves = tile.moves();
                if (moves != null) {
                    upperCount = moves.getOrDefault(Row.UPPER, 0);
                    lowerCount = moves.getOrDefault(Row.LOWER, 0);
                }
                return;
            }
        }
    }

    // ── Rendering ────────────────────────────────────────────────────────────

    private double availableWidth() {
        double w = rootStack.getWidth();
        if (w <= 0) w = 1280;
        // contentBox padding 24+24, plus a safety margin
        return Math.max(200, w - 80);
    }

    private double computeCardWidth(int n, double gap, double max, double min) {
        if (n <= 0) return max;
        double avail = availableWidth();
        double w = (avail - gap * Math.max(0, n - 1)) / n;
        if (w > max) w = max;
        if (w < min) w = min;
        return w;
    }

    private void renderBoard() {
        if (game == null) return;
        headerLabel.setText(String.format("MESOS — Round %d/10  ·  Era %d  ·  Current: %s",
                game.round(), game.era(), nameFor(game.currentPlayerTotem())));
        phaseLabel.setText("Phase: " + (game.phase() != null ? game.phase().name() : "—"));

        List<CardDTO> top = game.board().topRow();
        List<CardDTO> bot = game.board().lowRow();
        List<OfferTileDTO> off = game.board().offerTrack();

        int topN = (int) top.stream().filter(java.util.Objects::nonNull).count();
        int botN = (int) bot.stream().filter(java.util.Objects::nonNull).count();
        int offN = off.size();

        double topW = computeCardWidth(topN, CARD_GAP, CARD_MAX_W, CARD_MIN_W);
        double botW = computeCardWidth(botN, CARD_GAP, CARD_MAX_W, CARD_MIN_W);
        double offW = computeCardWidth(offN, 0,         TILE_MAX_W, TILE_MIN_W);

        renderRow(topRow, top, Row.UPPER, topW);
        renderRow(bottomRow, bot, Row.LOWER, botW);
        renderOfferTrack(offerTrack, off, offW);

        PlayerDTO me = findMe();
        if (me != null) {
            tribeLabel.setText("YOUR TRIBE — " + me.name());
            tribeContents.setText(summariseCards(me));
        } else {
            tribeLabel.setText("YOUR TRIBE");
            tribeContents.setText("");
        }

        othersBox.getChildren().clear();
        if (me != null) othersBox.getChildren().add(buildPlayerRow(me, true));
        for (PlayerDTO p : game.players()) {
            if (me != null && p.totem() == me.totem()) continue;
            othersBox.getChildren().add(buildPlayerRow(p, false));
        }

        renderHintAndConfirm();
    }

    private Node buildPlayerRow(PlayerDTO p, boolean self) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER);

        if (p.totem() != null) {
            ImageView totem = safeImageView(() -> ImageCatalog.totem2D(p.totem()));
            if (totem != null) {
                totem.setFitWidth(TOTEM_BADGE);
                totem.setPreserveRatio(true);
                row.getChildren().add(totem);
            }
        }

        Label name = new Label((self ? "★ " : "") + p.name());
        name.setStyle("-fx-font-weight: bold; -fx-text-fill: #f5deb3;");
        row.getChildren().add(name);

        row.getChildren().add(Chip.food(p.food(), CHIP_WIDTH));
        row.getChildren().add(Chip.pp(p.pp(), CHIP_WIDTH));

        if (self) {
            Label tag = new Label("(you)");
            tag.setStyle("-fx-text-fill: #888;");
            row.getChildren().add(tag);
        }
        return row;
    }

    private void renderRow(HBox container, List<CardDTO> cards, Row row, double cardW) {
        container.getChildren().clear();
        if (cards == null) return;
        boolean clickable = canPickCards();
        int idx = 0;
        for (CardDTO c : cards) {
            if (c == null) { idx++; continue; }
            container.getChildren().add(buildCardCell(c, row, idx, clickable, cardW));
            idx++;
        }
    }

    private Node buildCardCell(CardDTO card, Row row, int idx, boolean clickable, double cardW) {
        double cardH = cardW * CARD_ASPECT;
        StackPane cell = new StackPane();
        cell.setPrefSize(cardW, cardH);
        cell.setMinSize(cardW, cardH);
        cell.setMaxSize(cardW, cardH);
        cell.setAlignment(Pos.CENTER);

        ImageView img = safeImageView(() -> ImageCatalog.cardFront(card.id()));
        if (img != null) {
            img.setFitWidth(cardW);
            img.setFitHeight(cardH);
            img.setPreserveRatio(false);
            Rectangle clip = new Rectangle(cardW, cardH);
            clip.setArcWidth(cardW * 0.12);
            clip.setArcHeight(cardW * 0.12);
            img.setClip(clip);
            cell.getChildren().add(img);
        } else {
            Label fallback = new Label(card.id());
            fallback.setWrapText(true);
            fallback.setStyle("-fx-text-fill: #f5deb3; -fx-background-color: #3a2410;"
                    + " -fx-background-radius: 8; -fx-padding: 6;");
            cell.getChildren().add(fallback);
        }

        boolean selected = selectedMoves.contains(new Move(idx, row));
        if (selected) {
            cell.setEffect(selectedGlow());
            cell.setScaleX(HOVER_SCALE);
            cell.setScaleY(HOVER_SCALE);
        }

        if (clickable) {
            cell.setCursor(Cursor.HAND);
            cell.setOnMouseEntered(_ -> {
                if (!selectedMoves.contains(new Move(idx, row))) scale(cell, HOVER_SCALE);
            });
            cell.setOnMouseExited(_ -> {
                if (!selectedMoves.contains(new Move(idx, row))) scale(cell, 1.0);
            });
            cell.setOnMouseClicked(_ -> onCardClicked(row, idx, card, cell));
        } else {
            cell.setOpacity(0.65);
        }
        return cell;
    }

    private void renderOfferTrack(HBox container, List<OfferTileDTO> tiles, double tileW) {
        container.getChildren().clear();
        if (tiles == null) return;
        boolean clickable = canPlaceTotem();
        int idx = 0;
        for (OfferTileDTO t : tiles) {
            container.getChildren().add(buildOfferTileCell(t, idx, clickable, tileW));
            idx++;
        }
    }

    private Node buildOfferTileCell(OfferTileDTO tile, int idx, boolean clickable, double tileW) {
        double tileH = tileW * TILE_ASPECT;
        StackPane cell = new StackPane();
        cell.setPrefSize(tileW, tileH);
        cell.setMinSize(tileW, tileH);
        cell.setMaxSize(tileW, tileH);
        cell.setAlignment(Pos.CENTER);

        ImageView img = safeImageView(() -> ImageCatalog.offerTile(tile.id()));
        if (img != null) {
            img.setFitWidth(tileW);
            img.setFitHeight(tileH);
            img.setPreserveRatio(false);
            cell.getChildren().add(img);
        } else {
            Label fallback = new Label(tile.id());
            fallback.setStyle("-fx-text-fill: #f5deb3;");
            cell.getChildren().add(fallback);
        }

        if (tile.totem() != null) {
            ImageView totem = safeImageView(() -> ImageCatalog.totem2D(tile.totem()));
            if (totem != null) {
                totem.setFitWidth(tileW * 0.5);
                totem.setPreserveRatio(true);
                cell.getChildren().add(totem);
            }
        }

        boolean occupied = tile.totem() != null;
        if (clickable && !occupied) {
            cell.setCursor(Cursor.HAND);
            cell.setOnMouseClicked(_ -> onOfferTileClicked(idx, tile));
        } else if (!clickable) {
            cell.setOpacity(0.7);
        }
        return cell;
    }

    private static DropShadow selectedGlow() {
        DropShadow glow = new DropShadow(24, Color.WHITE);
        glow.setSpread(0.45);
        return glow;
    }

    private void scale(StackPane node, double to) {
        ScaleTransition st = new ScaleTransition(ANIM, node);
        st.setToX(to);
        st.setToY(to);
        st.play();
    }

    private static ImageView safeImageView(java.util.function.Supplier<javafx.scene.image.Image> supplier) {
        try {
            return new ImageView(supplier.get());
        } catch (Exception e) {
            System.err.println("[GUI] image load failed: " + e.getMessage());
            return null;
        }
    }

    private void renderHintAndConfirm() {
        Phase phase = game.phase();
        if (waitingServer) {
            hintLabel.setText("Waiting for server...");
            confirmButton.setDisable(true);
            return;
        }
        if (!isMyTurn()) {
            hintLabel.setText(waitingHint());
            confirmButton.setDisable(true);
            return;
        }
        if (phase == Phase.TOTEM_PLACEMENT) {
            hintLabel.setText("Click an offer tile to place your totem.");
            confirmButton.setDisable(true);
        } else if (phase == Phase.ACTION_EXECUTION || phase == Phase.EXTRA_MOVE) {
            int required = upperCount + lowerCount;
            hintLabel.setText(String.format(
                    "Pick cards (top×%d, bottom×%d)  —  Selected: %d / %d",
                    upperCount, lowerCount, selectedMoves.size(), required));
            confirmButton.setDisable(false);
        } else {
            hintLabel.setText(waitingHint());
            confirmButton.setDisable(true);
        }
    }

    private String waitingHint() {
        if (game == null) return "Waiting...";
        Phase phase = game.phase();
        if (game.currentPlayerTotem() != null) {
            return "Waiting for " + nameFor(game.currentPlayerTotem()) + "...";
        }
        return switch (phase) {
            case EVENTS_EXECUTION -> "Resolving events...";
            case END_ROUND -> "Resolving end-of-round...";
            case END_GAME -> "Game over.";
            case null -> "Waiting...";
            default -> "Waiting...";
        };
    }

    private String nameFor(Totem totem) {
        if (game == null || totem == null) return "?";
        for (PlayerDTO p : game.players()) {
            if (p.totem() == totem) return p.name();
        }
        return "?";
    }

    private PlayerDTO findMe() {
        if (game == null || username == null) return null;
        for (PlayerDTO p : game.players()) {
            if (username.equals(p.name())) return p;
        }
        return null;
    }

    private static String summariseCards(PlayerDTO p) {
        StringBuilder sb = new StringBuilder();
        if (p.cards() == null) return "";
        p.cards().forEach((type, set) -> {
            if (set != null && !set.isEmpty()) {
                if (!sb.isEmpty()) sb.append("  ");
                sb.append(type.name()).append(":").append(set.size());
            }
        });
        return sb.toString();
    }
}
