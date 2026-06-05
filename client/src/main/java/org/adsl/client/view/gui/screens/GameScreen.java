package org.adsl.client.view.gui.screens;

import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.adsl.shared.enums.CardType;
import org.adsl.client.AppCoordinator;
import org.adsl.client.serverEvents.ErrorEvent;
import org.adsl.client.serverEvents.EventsTriggeredEvent;
import org.adsl.client.serverEvents.GameUpdateEvent;
import org.adsl.client.view.gui.Chip;
import org.adsl.client.view.gui.FloatingLog;
import org.adsl.client.view.gui.ImageCatalog;
import org.adsl.client.view.gui.LayoutMath;
import org.adsl.shared.enums.Phase;
import org.adsl.shared.enums.Row;
import org.adsl.shared.enums.Totem;
import org.adsl.shared.model.CardDTO;
import org.adsl.shared.model.GameDTO;
import org.adsl.shared.model.OfferTileDTO;
import org.adsl.shared.model.OrderCellDTO;
import org.adsl.shared.model.OrderTileDTO;
import org.adsl.shared.model.PlayerDTO;
import org.adsl.shared.utils.Move;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
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
    private static final double CARD_MAX_W   = 95.0;
    private static final double CARD_MIN_W   = 48.0;
    private static final double CARD_GAP     = 6.0;
    /** Margin (px) kept between the uniformly-scaled board and the window edges. */
    private static final double BOARD_MARGIN = 24.0;
    /** Estimated natural board height (px) at reference size, used only to size
     *  hand pagination before the exact scale is known (applyBoardScale computes
     *  the real scale from measured prefHeight). Tune if hand card count looks off. */
    private static final double REF_BOARD_H = 820.0;
    private static final double TILE_ASPECT  = 1.65;
    private static final double TILE_MAX_W   = 80.0;
    private static final double TILE_MIN_W   = 42.0;
    private static final Duration ANIM       = Duration.millis(140);
    private static final double CHIP_WIDTH   = 50;
    private static final double CHIP_WIDTH_SM = 40;
    private static final double SELF_TOTEM   = 36;
    private static final double OPP_TOTEM    = 24;
    private static final double OPP_HAND_W   = 66;
    private static final double SIDE_PANEL_W   = 220;
    private static final double LR_PANEL_W     = 170;
    private static final double IDENTITY_COL_W = 100;
    private static final double HAND_GAP       = 6;
    private static final double NAME_H_EST     = 22;
    private static final int    OPP_HAND_PER_PAGE  = 8;
    private static final double NAV_BTN_W      = 44;
    private static final double OPP_NAV_BTN_W  = 32;
    /** Fixed space reserved for opponent panels in applyBoardScale.
     *  Using constants (not live measurements) means expanded panels never
     *  cause the board to rescale — expansion only shows a semi-transparent overlay. */
    private static final double TOP_PANEL_H_RESERVE  = 95.0;
    private static final double SIDE_PANEL_W_RESERVE = LR_PANEL_W + 40;
    // Max fraction of window height an expanded TOP-slot panel may occupy
    // before it is scaled down to fit (the TOP region itself grows unbounded).
    private static final double TOP_EXPAND_BUDGET = 0.42;

    // ── Order tile + 3D totem geometry ───────────────────────────────────────
    // Reference pixel space of the order-tile PNGs (≈624×965) and the 3D totem
    // PNGs (220×384). Cell centers and the totem support point were measured by
    // hand on those originals, then normalised to fractions so they scale with
    // the rendered tile.
    private static final double ORDER_TILE_W   = 624.0;
    private static final double ORDER_TILE_H   = 965.0;
    private static final double TOTEM3D_W      = 220.0;
    private static final double TOTEM3D_H      = 384.0;
    // Support point inside the 3D totem sprite (the pixel that "rests" on a cell).
    private static final double TOTEM_ANCHOR_X = 113.0 / TOTEM3D_W;
    private static final double TOTEM_ANCHOR_Y = 339.0 / TOTEM3D_H;
    // All cell centers share one horizontal line (centered on the tile).
    private static final double ORDER_CELL_X   = 0.5;
    // Totem width relative to tile width (≈ the measured cell rect width 201px).
    private static final double ORDER_TOTEM_W_FRAC = 201.0 / ORDER_TILE_W;
    private static final double OFFER_ROW_SPACING  = 14.0;
    // Per-player-count vertical cell-center fractions (index = players − 2).
    private static final double[][] ORDER_CELL_Y = {
        { 298.0 / ORDER_TILE_H, 462.0 / ORDER_TILE_H },
        { 257.0 / ORDER_TILE_H, 422.0 / ORDER_TILE_H, 587.0 / ORDER_TILE_H },
        { 213.0 / ORDER_TILE_H, 379.0 / ORDER_TILE_H, 545.0 / ORDER_TILE_H, 711.0 / ORDER_TILE_H },
        { 148.0 / ORDER_TILE_H, 314.0 / ORDER_TILE_H, 479.0 / ORDER_TILE_H, 646.0 / ORDER_TILE_H, 811.0 / ORDER_TILE_H },
    };

    // ── Offer tile + 3D totem geometry ───────────────────────────────────────
    // Reference pixel space of the offer-tile PNGs (602×1004). Each tile has a
    // single totem slot whose center was measured by hand (the white frame),
    // then normalised so it scales with the rendered tile. The 3D totem rests
    // its support point (TOTEM_ANCHOR_*) on that slot center, same as the order
    // tile.
    private static final double OFFER_TILE_W = 602.0;
    private static final double OFFER_TILE_H = 1004.0;
    // Totem width chosen so its on-screen size matches the order-track totems
    // (both are sized off the shared rendered tile height).
    private static final double OFFER_TOTEM_W_FRAC = (201.0 / 965.0) * (OFFER_TILE_H / OFFER_TILE_W);
    // Slot center fractions keyed by offer-tile id.
    private static final Map<String, double[]> OFFER_SLOT = Map.of(
        "offer_tile_a", new double[]{325.0 / OFFER_TILE_W, 280.0 / OFFER_TILE_H},
        "offer_tile_b", new double[]{325.0 / OFFER_TILE_W, 280.0 / OFFER_TILE_H},
        "offer_tile_c", new double[]{309.0 / OFFER_TILE_W, 281.0 / OFFER_TILE_H},
        "offer_tile_d", new double[]{302.0 / OFFER_TILE_W, 281.0 / OFFER_TILE_H},
        "offer_tile_e", new double[]{291.0 / OFFER_TILE_W, 280.0 / OFFER_TILE_H},
        "offer_tile_f", new double[]{281.0 / OFFER_TILE_W, 281.0 / OFFER_TILE_H},
        "offer_tile_g", new double[]{277.0 / OFFER_TILE_W, 281.0 / OFFER_TILE_H}
    );

    @FXML private StackPane  rootStack;
    @FXML private BorderPane rootPane;
    @FXML private VBox       centerBox;
    @FXML private Label      headerLabel;
    @FXML private Label      phaseLabel;
    @FXML private HBox       topRow;
    @FXML private HBox       offerRow;
    @FXML private Pane       deckPane;
    @FXML private Pane       orderTilePane;
    @FXML private HBox       offerTrack;
    @FXML private HBox       buildingDecks;
    @FXML private HBox       bottomRow;
    @FXML private HBox       topPlayersBox;
    @FXML private VBox       leftPlayersBox;
    @FXML private VBox       rightPlayersBox;
    @FXML private HBox       selfPanelBox;
    @FXML private TextFlow   hintLabel;
    @FXML private Button     confirmButton;
    @FXML private Label      errorLabel;
    @FXML private VBox       logBox;

    private GameDTO game;
    private final Set<Move> selectedMoves = new LinkedHashSet<>();
    private int upperCount = 0;
    private int lowerCount = 0;
    private boolean waitingServer = false;
    /** Identity of the picking turn the current selection belongs to; when it
     *  changes (new player or phase) the pending selection is dropped so a past
     *  turn's picks never bleed into a new ACTION_EXECUTION / EXTRA_MOVE turn. */
    private String selectionTurnToken = "none";
    private int selfHandPage = 0;
    /** The single card width shared by every card row and hand, set each render
     *  by {@link #renderBoard()} from the most-constrained board row. */
    private double sharedCardW = CARD_MIN_W;
    /** Height-driven estimate of the board scale, used to size hand pagination in
     *  reference space so the hand fills the row at whatever size the board scales to. */
    private double boardScale = 1.0;
    /** Opponent names whose card hand is currently expanded. Persisted here (not
     *  on the panel node) so the expanded state survives a full board re-render. */
    private final Set<String> expandedOpponents = new HashSet<>();
    private final java.util.Map<String, Integer> opponentHandPages = new java.util.HashMap<>();
    // Fixed seat order, captured from the first game snapshot. The server
    // reorders players() by turn order each phase; rendering against this stable
    // list keeps each player's panel in the same slot for the whole game.
    private List<String> playerOrder;
    private final FloatingLog floatingLog;
    /** Scaled board group; stored as a field so applyBoardScale can translate it. */
    private Group boardGroup;
    /** Hand nodes (HBox/VBox with cards only) of all currently-expanded panels.
     *  Rebuilt each renderOpponentPanels. Opacity is applied here, not on the
     *  whole panel, so buttons and headers stay fully opaque. */
    private final java.util.List<Pane> expandedHandNodes = new java.util.ArrayList<>();
    /** Shared fade timer: fires after hover leaves ALL expanded panels. */
    private final PauseTransition expandedFadeTimer =
            new PauseTransition(Duration.millis(4000));

    private List<Image> rulesPages;
    private int rulesPageIndex = 0;
    private StackPane rulesOverlay;

    // ── Keyboard navigation (spatial) ────────────────────────────────────────
    // Rebuilt each render: stable key → the active node it points to. Only
    // elements that are actually actionable in the current phase are added, so
    // the arrows never land on inert cards/tiles. navKey survives a re-render so
    // the cursor stays put across board updates.
    private final java.util.Map<String, Node> navIndex = new java.util.LinkedHashMap<>();
    private String navKey = null;
    private Node navFocusedNode = null;

    private StackPane overlayPane;
    private Label overlayTitle;
    private VBox overlayPlayerList;

    /**
     * Shared debounce timer for resize-driven re-renders. Each resize listener
     * resets it; the actual {@link #renderBoard()} fires only once the window
     * has been still for {@link #RESIZE_DEBOUNCE_MS}, avoiding hundreds of full
     * board rebuilds per second while dragging the window edge.
     */
    private static final double RESIZE_DEBOUNCE_MS = 90;
    private final PauseTransition resizeDebounce =
            new PauseTransition(Duration.millis(RESIZE_DEBOUNCE_MS));

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
        // Lay the board out at its natural size inside a Group (which sizes its
        // child to preferred and ignores the parent's sizing), centered in the
        // root stack. applyBoardScale() then scales that whole block to fit the
        // window; the aerial background fills the window behind it (not scaled).
        int boardIdx = rootStack.getChildren().indexOf(rootPane);
        rootStack.getChildren().remove(rootPane);
        boardGroup = new Group(rootPane);
        StackPane.setAlignment(boardGroup, Pos.CENTER);
        rootStack.getChildren().add(boardIdx, boardGroup);
        installBackground();

        // Opponent panels are lifted out of the BorderPane (rootPane) into rootStack
        // so they float as screen-fixed overlays independent of board scale. This
        // prevents their size from contributing to rootPane.prefWidth/prefHeight, which
        // would shift the board every time a panel expands or its content changes.
        rootPane.setTop(null);
        rootPane.setLeft(null);
        rootPane.setRight(null);
        StackPane.setAlignment(topPlayersBox, Pos.TOP_CENTER);
        topPlayersBox.setPickOnBounds(false);
        StackPane.setAlignment(leftPlayersBox, Pos.CENTER_LEFT);
        leftPlayersBox.setPickOnBounds(false);
        leftPlayersBox.setMaxWidth(LR_PANEL_W + 40);
        StackPane.setAlignment(rightPlayersBox, Pos.CENTER_RIGHT);
        rightPlayersBox.setPickOnBounds(false);
        rightPlayersBox.setMaxWidth(LR_PANEL_W + 40);
        int panelInsertIdx = rootStack.getChildren().indexOf(boardGroup) + 1;
        rootStack.getChildren().add(panelInsertIdx,     topPlayersBox);
        rootStack.getChildren().add(panelInsertIdx + 1, leftPlayersBox);
        rootStack.getChildren().add(panelInsertIdx + 2, rightPlayersBox);

        // Fix the hint-row height so changes to its content (arrows vs plain text)
        // never alter rootPane.prefHeight and thus never cause the board to rescale.
        if (hintLabel != null && hintLabel.getParent() instanceof HBox hintRow) {
            hintRow.setMinHeight(40);
            hintRow.setPrefHeight(40);
            hintRow.setMaxHeight(40);
        }

        // Header labels stay inside centerBox (part of the scaled board block).
        // applyBoardScale reserves space for opponent panels via TOP_PANEL_H_RESERVE /
        // SIDE_PANEL_W_RESERVE so the board is never hidden behind them.

        // Only the window (rootStack) drives re-renders. We deliberately do NOT
        // listen on centerBox size: its height/width change as a *result* of
        // renderBoard (and of expanding an opponent panel), so listening there
        // created a feedback loop that re-rendered the board and collapsed any
        // expanded panel. Board zoom-out is handled by the global ResponsiveScaler.
        resizeDebounce.setOnFinished(_ -> renderBoard());
        rootStack.widthProperty().addListener((_, _, _) -> resizeDebounce.playFromStart());
        rootStack.heightProperty().addListener((_, _, _) -> resizeDebounce.playFromStart());
        rootStack.setFocusTraversable(true);
        rootStack.setOnKeyPressed(e -> { if (handleNavKey(e.getCode())) e.consume(); });

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

        setupRulesButton();
        resolveMoveCounts();
        renderBoard();
    }

    /**
     * Installs the aerial-view image as a cover-scaled background behind the
     * board. A {@link Region} added as the first (bottom) child of the root
     * stack auto-resizes to fill the window; {@code BackgroundSize} cover scales
     * the image to always fill the area (cropping overflow) so no borders show.
     */
    private void installBackground() {
        Image bg = safeImage("/assets/aerial_views/v2.png");
        if (bg == null) return;
        BackgroundSize cover = new BackgroundSize(
                BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, false, true);
        BackgroundImage bgImage = new BackgroundImage(bg,
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER, cover);
        Region bgLayer = new Region();
        bgLayer.setBackground(new Background(bgImage));
        bgLayer.setMouseTransparent(true);
        rootStack.getChildren().add(0, bgLayer);

        // Dark tint above the image to improve contrast/readability of the board.
        Region tint = new Region();
        tint.setStyle("-fx-background-color: rgba(0,0,0,0.45);");
        tint.setMouseTransparent(true);
        rootStack.getChildren().add(1, tint);
    }

    private Image safeImage(String path) {
        try {
            var in = getClass().getResourceAsStream(path);
            return in != null ? new Image(in) : null;
        } catch (Exception e) {
            return null;
        }
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
        syncSelectionToTurn();
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
        if (game.phase() == Phase.EXTRA_MOVE) {
            if (row != Row.UPPER) {
                errorLabel.setText("Extra move: pick from the top row only.");
                return;
            }
            boolean wasSelected = selectedMoves.contains(m);
            selectedMoves.clear();
            if (!wasSelected) selectedMoves.add(m);
        } else if (!selectedMoves.add(m)) {
            selectedMoves.remove(m);
        }
        pane.setEffect(selectedMoves.contains(m) ? selectedGlow() : null);
        pane.getProperties().put("navSelected", selectedMoves.contains(m));
        errorLabel.setText("");
        renderHintAndConfirm();
        Platform.runLater(this::refreshNav);
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

    /** During EXTRA_MOVE only the top row is pickable; otherwise any row when picking. */
    private boolean canPickRow(Row row) {
        if (!canPickCards()) return false;
        return game.phase() != Phase.EXTRA_MOVE || row == Row.UPPER;
    }

    /** Drops the pending selection when the picking turn changes (see {@link #selectionTurnToken}). */
    private void syncSelectionToTurn() {
        String token = pickingTurnToken();
        if (!token.equals(selectionTurnToken)) {
            selectedMoves.clear();
            selectionTurnToken = token;
        }
    }

    /** A stable id for the current picking turn ({@code phase:totem}), or {@code "none"}
     *  outside the card-picking phases. */
    private String pickingTurnToken() {
        if (game == null) return "none";
        Phase ph = game.phase();
        if (ph != Phase.ACTION_EXECUTION && ph != Phase.EXTRA_MOVE) return "none";
        Totem cur = game.currentPlayerTotem();
        return ph.name() + ":" + (cur == null ? "?" : cur.name());
    }

    private void resolveMoveCounts() {
        upperCount = 0;
        lowerCount = 0;
        if (game == null) return;
        if (game.phase() == Phase.EXTRA_MOVE) {
            // Extra move grants one optional pick from the top row only.
            upperCount = 1;
            lowerCount = 0;
            return;
        }
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

    private static String formatPhase(Phase phase) {
        if (phase == null) return "—";
        StringBuilder sb = new StringBuilder();
        for (String word : phase.name().split("_")) {
            if (sb.length() > 0) sb.append(' ');
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                sb.append(word.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    private void renderBoard() {
        if (game == null) return;
        headerLabel.setText(String.format("MESOS — Round %d/10  ·  Era %d  ·  Current: %s",
                game.round(), game.era(), nameFor(game.currentPlayerTotem())));
        phaseLabel.setText("Phase: " + formatPhase(game.phase()));

        List<CardDTO> top = game.board().topRow();
        List<CardDTO> bot = game.board().lowRow();
        List<OfferTileDTO> off = game.board().offerTrack();

        int topN = (int) top.stream().filter(java.util.Objects::nonNull).count();
        int botN = (int) bot.stream().filter(java.util.Objects::nonNull).count();
        int offN = off.size();

        // One card size for EVERY card (top row, bottom row, hands): size the
        // most-constrained single-line card row, so cards never differ in size.
        // Every card/tile is drawn at a fixed reference size; the whole board is
        // then scaled as one block (applyBoardScale) to fit the window, so all
        // elements resize together and nothing is ever clipped.
        sharedCardW = CARD_MAX_W;
        double offW = TILE_MAX_W;

        renderRow(topRow, top, Row.UPPER, sharedCardW);
        renderRow(bottomRow, bot, Row.LOWER, sharedCardW);
        renderOfferTrack(offerTrack, off, offW);
        renderDeckCard(offW * TILE_ASPECT);
        renderOrderTile(offW * TILE_ASPECT);
        renderBuildingDecks(offW * TILE_ASPECT);

        // Height-driven scale estimate: lets the hand show as many reference-sized
        // cards as fit once the board is scaled up to fill the window height.
        double bh = rootStack.getHeight();
        boardScale = (bh > 0) ? Math.max(0.1, (bh - 2 * BOARD_MARGIN) / REF_BOARD_H) : 1.0;

        renderSelfPanel();
        renderOpponentPanels();

        renderHintAndConfirm();

        // Defer until the new content has been laid out so prefWidth/prefHeight
        // reflect it, then scale the whole board to fit the window and rebuild
        // the keyboard-navigation index against the freshly-built nodes.
        Platform.runLater(() -> { applyBoardScale(); refreshNav(); });
    }

    /**
     * Scales the whole board ({@link #rootPane}) as one block so it fits the
     * window minus {@link #BOARD_MARGIN}, keeping its aspect ratio. Scales both
     * down AND up (to fill large screens), driven by the tighter of the
     * width/height fits — so every element resizes together and the decks pinned
     * top and bottom are never pushed off-screen. The background fills the window
     * behind it and is not scaled.
     */
    private void applyBoardScale() {
        if (rootStack == null || rootPane == null || boardGroup == null) return;

        // Reserve fixed space for opponent panels regardless of their expanded/collapsed
        // state — this keeps the board scale stable when panels open or close.
        int n      = (game != null && game.players() != null) ? game.players().size() : 0;
        double topH  = n >= 2 ? TOP_PANEL_H_RESERVE  : 0;
        double sideW = n >= 4 ? SIDE_PANEL_W_RESERVE : 0;

        double availW = rootStack.getWidth()  - 2 * BOARD_MARGIN - 2 * sideW;
        double availH = rootStack.getHeight() - 2 * BOARD_MARGIN - topH;
        if (availW <= 0 || availH <= 0) return;
        double prefW = rootPane.prefWidth(-1);
        double prefH = rootPane.prefHeight(-1);
        if (prefW <= 0 || prefH <= 0) return;
        double s = Math.min(availW / prefW, availH / prefH);
        rootPane.setScaleX(s);
        rootPane.setScaleY(s);
        // Center the board in the area that remains after panel reservations.
        // sideW is symmetric so no horizontal shift; topH pushes the vertical
        // center down by half the reserved top space.
        boardGroup.setTranslateX(0);
        boardGroup.setTranslateY(topH / 2.0);
    }

    // ── Self panel (bottom) ──────────────────────────────────────────────────

    private void renderSelfPanel() {
        selfPanelBox.getChildren().clear();
        PlayerDTO me = findMe();
        if (me == null) return;

        // Bottom spans the full window width in BorderPane, so the hand can
        // use everything minus the identity column, side paddings, and (if
        // pagination is needed) two nav-button slots.
        List<CardDTO> allCards = collectHand(me);
        int total = allCards.size();
        double stack = rootStack.getWidth();
        if (stack <= 0) stack = 1280;
        double cardW = sharedCardW;            // identical to the board card size
        double cardH = cardW * CARD_ASPECT;

        // Fit as many board-sized cards as the hand area allows; only then paginate.
        // Recompute once with the nav-button reservation if a page overflows.
        // Pagination works in reference space (window width ÷ board scale) so the
        // hand fills the row at whatever size the board ends up scaled to.
        double usable = stack / boardScale;
        int perPage = LayoutMath.perPage(usable - IDENTITY_COL_W - 40, cardW, HAND_GAP);
        boolean paginated = total > perPage;
        if (paginated) {
            perPage = LayoutMath.perPage(usable - IDENTITY_COL_W - 40 - (NAV_BTN_W * 2 + 16), cardW, HAND_GAP);
            paginated = total > perPage;
        }
        int pageCount = paginated ? (int) Math.ceil(total / (double) perPage) : 1;
        if (selfHandPage >= pageCount) selfHandPage = pageCount - 1;
        if (selfHandPage < 0) selfHandPage = 0;

        int from = selfHandPage * perPage;
        int to = Math.min(from + perPage, total);
        List<CardDTO> pageCards = allCards.subList(from, to);

        // Identity column: totem pinned to top, name + chips pinned to bottom,
        // overall height = card height so the boundaries line up. The totem
        // is clamped to whatever vertical space is left after name + chip.
        VBox left = new VBox(4);
        left.setAlignment(Pos.TOP_CENTER);
        left.setPrefWidth(IDENTITY_COL_W);
        left.setMinWidth(IDENTITY_COL_W);
        left.setPrefHeight(cardH);
        left.setMinHeight(cardH);
        left.setMaxHeight(cardH);

        double totemMaxH = Math.max(16, cardH - NAME_H_EST - CHIP_WIDTH - 12);

        if (me.totem() != null) {
            ImageView totem = safeImageView(() -> ImageCatalog.totem2D(me.totem()));
            if (totem != null) {
                totem.setFitWidth(SELF_TOTEM);
                totem.setFitHeight(totemMaxH);
                totem.setPreserveRatio(true);
                left.getChildren().add(totem);
            }
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        left.getChildren().add(spacer);

        Label name = new Label("★ " + me.name());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #f5deb3;");
        left.getChildren().add(name);

        HBox stats = new HBox(8, Chip.food(me.food(), CHIP_WIDTH), Chip.pp(me.pp(), CHIP_WIDTH));
        stats.setAlignment(Pos.CENTER);
        left.getChildren().add(stats);

        HBox cards = buildHandCards(pageCards, cardW);

        HBox handRow = new HBox(8);
        handRow.setAlignment(Pos.CENTER_LEFT);
        if (selfHandPage > 0) {
            handRow.getChildren().add(buildHandNavButton("◀", -1));
        }
        handRow.getChildren().add(cards);
        if (selfHandPage < pageCount - 1) {
            handRow.getChildren().add(buildHandNavButton("▶", +1));
        }

        selfPanelBox.getChildren().addAll(left, handRow);
        HBox.setHgrow(handRow, javafx.scene.layout.Priority.ALWAYS);
    }

    private Button buildHandNavButton(String glyph, int delta) {
        Button b = new Button(glyph);
        b.setFocusTraversable(false);
        b.setStyle("-fx-font-size: 16px; -fx-padding: 4 10 4 10; -fx-background-radius: 8;");
        b.setOnAction(_ -> {
            selfHandPage += delta;
            renderSelfPanel();
        });
        markNav(b, "SELF_NAV:" + (delta < 0 ? "PREV" : "NEXT"), "BUTTON", b::fire);
        return b;
    }

    // ── Opponent panels (sides) ──────────────────────────────────────────────

    private enum Slot { LEFT, TOP, RIGHT }

    /**
     * Slot assignment for opponents based on total player count. Returned in
     * seat order starting from self+1 going clockwise.
     */
    private List<Slot> slotsFor(int playerCount) {
        return switch (playerCount) {
            case 2 -> List.of(Slot.TOP);
            case 3 -> List.of(Slot.TOP, Slot.TOP);
            case 4 -> List.of(Slot.LEFT, Slot.TOP, Slot.RIGHT);
            case 5 -> List.of(Slot.LEFT, Slot.TOP, Slot.TOP, Slot.RIGHT);
            default -> List.of();
        };
    }

    /** Called when mouse enters or exits any expanded opponent panel.
     *  On enter: cancel fade timer, show ALL expanded panels at full opacity.
     *  On exit: start shared timer; when it fires fade all back to semi-transparent. */
    private void onExpandedPanelHover(boolean entering) {
        expandedFadeTimer.stop();
        if (entering) {
            for (Pane h : expandedHandNodes) h.setOpacity(1.0);
        } else {
            expandedFadeTimer.setOnFinished(_ -> {
                for (Pane h : expandedHandNodes) h.setOpacity(0.45);
            });
            expandedFadeTimer.playFromStart();
        }
    }

    private void renderOpponentPanels() {
        expandedHandNodes.clear();
        topPlayersBox.getChildren().clear();
        leftPlayersBox.getChildren().clear();
        rightPlayersBox.getChildren().clear();

        if (game.players() == null || game.players().isEmpty()) return;

        // Capture the seat order once, then always render against it so panels
        // don't shuffle when the server reorders players() between phases.
        if (playerOrder == null) {
            playerOrder = new ArrayList<>();
            for (PlayerDTO p : game.players()) playerOrder.add(p.name());
        }
        List<PlayerDTO> all = new ArrayList<>(game.players());
        all.sort(Comparator.comparingInt(p -> {
            int i = playerOrder.indexOf(p.name());
            return i < 0 ? Integer.MAX_VALUE : i;
        }));

        int n = all.size();
        int selfIdx = findMyIndex(all);

        // Build clockwise seat order starting from self+1.
        List<PlayerDTO> opponents = new ArrayList<>();
        for (int k = 1; k < n; k++) {
            int idx = (selfIdx >= 0 ? (selfIdx + k) % n : k - 1);
            opponents.add(all.get(idx));
        }

        List<Slot> slots = slotsFor(n);
        int count = Math.min(slots.size(), opponents.size());
        for (int i = 0; i < count; i++) {
            Slot slot = slots.get(i);
            Node panel = buildOpponentPanel(opponents.get(i), slot);
            switch (slot) {
                case LEFT  -> leftPlayersBox.getChildren().add(panel);
                case RIGHT -> rightPlayersBox.getChildren().add(panel);
                case TOP   -> topPlayersBox.getChildren().add(panel);
            }
        }
    }

    private int findMyIndex(List<PlayerDTO> players) {
        for (int i = 0; i < players.size(); i++) {
            if (username != null && username.equals(players.get(i).name())) return i;
        }
        return -1;
    }

    /**
     * Header for the opponent panel.
     * <p>{@code horizontal=true} (TOP slot): row of [totem, name, spacer, chips].
     * Wide layout, fills horizontal space.
     * <p>{@code horizontal=false} (LEFT/RIGHT): column of [totem, name, chips]
     * stacked, like the self panel — keeps the panel narrow so the center
     * board has more room.
     */
    private Pane buildOpponentHeader(PlayerDTO p, boolean horizontal) {
        ImageView totem = null;
        if (p.totem() != null) {
            totem = safeImageView(() -> ImageCatalog.totem2D(p.totem()));
            if (totem != null) {
                totem.setFitWidth(OPP_TOTEM);
                totem.setPreserveRatio(true);
            }
        }
        Label name = new Label(p.name());
        name.setStyle("-fx-font-weight: bold; -fx-text-fill: #f5deb3; -fx-font-size: 13px;");

        if (horizontal) {
            HBox h = new HBox(8);
            h.setAlignment(Pos.CENTER_LEFT);
            if (totem != null) h.getChildren().add(totem);
            h.getChildren().add(name);
            Region grow = new Region();
            HBox.setHgrow(grow, javafx.scene.layout.Priority.ALWAYS);
            h.getChildren().add(grow);
            h.getChildren().addAll(
                Chip.food(p.food(), CHIP_WIDTH_SM),
                Chip.pp(p.pp(),     CHIP_WIDTH_SM));
            return h;
        } else {
            VBox v = new VBox(4);
            v.setAlignment(Pos.CENTER);
            if (totem != null) v.getChildren().add(totem);
            v.getChildren().add(name);
            HBox chips = new HBox(6,
                Chip.food(p.food(), CHIP_WIDTH_SM),
                Chip.pp(p.pp(),     CHIP_WIDTH_SM));
            chips.setAlignment(Pos.CENTER);
            v.getChildren().add(chips);
            return v;
        }
    }

    private Node buildOpponentPanel(PlayerDTO p, Slot slot) {
        // TOP slot: cards lay out horizontally (single row), arrows on the
        // sides. LEFT/RIGHT: 4×2 vertical grid (paired columns) with arrows on
        // top/bottom. Both branches share the same pagination model.
        boolean topSlot = (slot == Slot.TOP);
        List<CardDTO> allCards = collectHand(p);
        int total = allCards.size();
        boolean paginated = total > OPP_HAND_PER_PAGE;
        int sizingN = Math.min(total, OPP_HAND_PER_PAGE);
        double cardsWidth = (sizingN == 0) ? 0 : sizingN * OPP_HAND_W + (sizingN - 1) * HAND_GAP;
        double navReserved = paginated ? (OPP_NAV_BTN_W * 2 + 16) : 0;
        final double basePanelW = topSlot ? SIDE_PANEL_W : LR_PANEL_W;
        double expandedW = topSlot
                ? Math.max(basePanelW, cardsWidth + navReserved + 28)
                : basePanelW;
        double cardAvail = topSlot
                ? expandedW - 28 - navReserved
                : basePanelW - 28;

        VBox panel = new VBox(6);
        panel.getStyleClass().add("panel");
        panel.setPadding(new Insets(8, 10, 8, 10));
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPrefWidth(basePanelW);
        panel.setMaxWidth(basePanelW);

        Pane header = buildOpponentHeader(p, topSlot);

        Button toggle = new Button("▾ Cards");
        toggle.setFocusTraversable(false);
        toggle.setStyle("-fx-font-size: 11px; -fx-padding: 4 8 4 8;");

        final double cardW = sharedCardW;      // all cards share one size

        final javafx.scene.layout.Pane hand;
        if (topSlot) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setVisible(false);
            row.setManaged(false);
            populateOpponentHandRow(row, panel, p, cardW, allCards);
            hand = row;
        } else {
            VBox col = new VBox(6);
            col.setAlignment(Pos.CENTER);
            col.setVisible(false);
            col.setManaged(false);
            populateOpponentHandColumn(col, panel, p, cardW, allCards);
            hand = col;
        }

        // Shown only in expanded state: player name in bottom-left while the
        // toggle stays centered.
        Label expandedName = new Label(p.name());
        expandedName.setStyle("-fx-font-weight: bold; -fx-text-fill: #f5deb3; -fx-font-size: 13px;");
        expandedName.setVisible(false);
        expandedName.setManaged(false);

        StackPane bottomBar = new StackPane();
        StackPane.setAlignment(expandedName, Pos.BOTTOM_LEFT);
        // TOP slot has plenty of horizontal room → toggle centered; side
        // slots are narrow so we tuck the toggle in the bottom-right corner.
        StackPane.setAlignment(toggle, topSlot ? Pos.CENTER : Pos.BOTTOM_RIGHT);
        bottomBar.getChildren().addAll(expandedName, toggle);

        // Applies the expanded/collapsed look and records it in expandedOpponents
        // so a later re-render can restore it. Used by the toggle and by the
        // initial state below.
        java.util.function.Consumer<Boolean> setExpanded = showCards -> {
            hand.setVisible(showCards);
            hand.setManaged(showCards);
            header.setVisible(!showCards);
            header.setManaged(!showCards);
            expandedName.setVisible(showCards);
            expandedName.setManaged(showCards);
            toggle.setText(showCards ? "▴ Player" : "▾ Cards");
            if (showCards) {
                expandedOpponents.add(p.name());
                if (topSlot) {
                    populateOpponentHandRow((HBox) hand, panel, p, cardW, allCards);
                } else {
                    populateOpponentHandColumn((VBox) hand, panel, p, cardW, allCards);
                }
                Platform.runLater(() -> fitExpandedPanel(panel, topSlot));
                // Only the card hand fades — toggle, name, chips stay fully opaque.
                expandedHandNodes.add(hand);
                // Start at full opacity (user just clicked the toggle, mouse is on
                // the panel). The fade timer runs immediately; if the mouse stays
                // inside, onMouseEntered cancels it and keeps everything at 1.0.
                hand.setOpacity(1.0);
                panel.setOnMouseEntered(_ -> onExpandedPanelHover(true));
                panel.setOnMouseExited(_ -> onExpandedPanelHover(false));
                onExpandedPanelHover(false);
            } else {
                expandedOpponents.remove(p.name());
                expandedHandNodes.remove(hand);
                hand.setOpacity(1.0);
                panel.setPrefWidth(basePanelW);
                panel.setMaxWidth(basePanelW);
                panel.setScaleX(1);
                panel.setScaleY(1);
                panel.setTranslateY(0);
                panel.setOnMouseEntered(null);
                panel.setOnMouseExited(null);
            }
        };
        toggle.setOnAction(_ -> setExpanded.accept(!hand.isVisible()));
        // The "Cards" toggle is always navigable so the player can browse any
        // opponent's hand, regardless of phase.
        markNav(toggle, "TOG:" + p.name(), "BUTTON", toggle::fire);

        panel.getChildren().addAll(header, hand, bottomBar);
        // Restore expanded state across re-renders.
        if (expandedOpponents.contains(p.name())) setExpanded.accept(true);
        return panel;
    }

    /**
     * Scales a single expanded opponent panel down so its content fits the
     * available vertical space, anchoring the top so it never grows upward off
     * the window. Acts per-panel: expanding one opponent leaves siblings
     * untouched. Collapsed panels reset scale to 1 in the toggle handler.
     *
     * <p>Side (LEFT/RIGHT) panels are bounded by their BorderPane region height;
     * TOP panels live in a region that grows with content, so they are bounded
     * by a fraction of the window height instead.
     */
    private void fitExpandedPanel(VBox panel, boolean topSlot) {
        panel.setScaleX(1);
        panel.setScaleY(1);
        panel.setTranslateY(0);

        double availH;
        if (topSlot) {
            double winH = rootStack.getHeight();
            if (winH <= 0) winH = 720;
            availH = winH * TOP_EXPAND_BUDGET;
        } else if (panel.getParent() instanceof Region region) {
            Insets pad = region.getInsets();
            availH = region.getHeight() - pad.getTop() - pad.getBottom();
        } else {
            return;
        }
        if (availH <= 0) return;

        double prefH = panel.prefHeight(panel.getWidth());
        if (prefH <= availH) return;

        double scale = availH / prefH;
        panel.setScaleX(scale);
        panel.setScaleY(scale);
        // Scaling pivots on the panel center; shift up so the visual top stays
        // anchored at the region top instead of drifting off-screen.
        panel.setTranslateY(-(prefH - availH) / 2.0);
    }

    /**
     * Fills {@code handRow} with optional prev/next nav buttons around the
     * current page of cards for {@code p}. The row mutates in-place so toggling
     * the page doesn't force a full board re-render (which would close the
     * expanded panel).
     */
    private void populateOpponentHandRow(HBox handRow, VBox panel, PlayerDTO p,
                                          double cardW, List<CardDTO> allCards) {
        int total = allCards.size();
        boolean paginated = total > OPP_HAND_PER_PAGE;
        int pageCount = paginated ? (int) Math.ceil(total / (double) OPP_HAND_PER_PAGE) : 1;
        int cur = opponentHandPages.getOrDefault(p.name(), 0);
        if (cur >= pageCount) cur = pageCount - 1;
        if (cur < 0) cur = 0;
        opponentHandPages.put(p.name(), cur);
        final int page = cur;

        int from = page * OPP_HAND_PER_PAGE;
        int to   = Math.min(from + OPP_HAND_PER_PAGE, total);
        List<CardDTO> pageCards = allCards.subList(from, to);

        handRow.getChildren().clear();
        boolean hasPrev = page > 0;
        boolean hasNext = page < pageCount - 1;
        if (hasPrev) {
            handRow.getChildren().add(buildOppNavButton("◀", () -> {
                opponentHandPages.put(p.name(), page - 1);
                populateOpponentHandRow(handRow, panel, p, cardW, allCards);
            }, "OPP_NAV:" + p.name() + ":PREV"));
        }
        if (total == 0) {
            handRow.setAlignment(Pos.CENTER);
            handRow.getChildren().add(buildNoCardsLabel());
        } else {
            handRow.setAlignment(Pos.CENTER_LEFT);
            handRow.getChildren().add(buildHandCards(pageCards, cardW));
        }
        if (hasNext) {
            handRow.getChildren().add(buildOppNavButton("▶", () -> {
                opponentHandPages.put(p.name(), page + 1);
                populateOpponentHandRow(handRow, panel, p, cardW, allCards);
            }, "OPP_NAV:" + p.name() + ":NEXT"));
        }

        // When expanded, size the panel to the actual content (cards + the
        // arrows currently visible). When collapsed the toggle handler keeps
        // the panel at SIDE_PANEL_W.
        if (handRow.isVisible()) {
            int sizingN = Math.min(total, OPP_HAND_PER_PAGE);
            double cardsW = sizingN * cardW + Math.max(0, sizingN - 1) * HAND_GAP;
            int navCount = (hasPrev ? 1 : 0) + (hasNext ? 1 : 0);
            double w = Math.max(SIDE_PANEL_W, cardsW + navCount * (OPP_NAV_BTN_W + 8) + 28);
            panel.setPrefWidth(w);
            panel.setMaxWidth(w);
        }
    }

    /**
     * Vertical counterpart of {@link #populateOpponentHandRow}: lays the page
     * as a 4×2 grid (4 rows × 2 columns), with ▲/▼ arrows above/below when
     * pagination is active. Used for LEFT/RIGHT slot opponents.
     */
    private void populateOpponentHandColumn(VBox container, VBox panel, PlayerDTO p,
                                             double cardW, List<CardDTO> allCards) {
        int total = allCards.size();
        boolean paginated = total > OPP_HAND_PER_PAGE;
        int pageCount = paginated ? (int) Math.ceil(total / (double) OPP_HAND_PER_PAGE) : 1;
        int cur = opponentHandPages.getOrDefault(p.name(), 0);
        if (cur >= pageCount) cur = pageCount - 1;
        if (cur < 0) cur = 0;
        opponentHandPages.put(p.name(), cur);
        final int page = cur;

        int from = page * OPP_HAND_PER_PAGE;
        int to   = Math.min(from + OPP_HAND_PER_PAGE, total);
        List<CardDTO> pageCards = allCards.subList(from, to);

        container.getChildren().clear();
        boolean hasPrev = page > 0;
        boolean hasNext = page < pageCount - 1;
        if (hasPrev) {
            container.getChildren().add(buildOppNavButton("▲", () -> {
                opponentHandPages.put(p.name(), page - 1);
                populateOpponentHandColumn(container, panel, p, cardW, allCards);
            }, "OPP_NAV:" + p.name() + ":PREV"));
        }
        if (total == 0) {
            container.getChildren().add(buildNoCardsLabel());
        } else {
            container.getChildren().add(buildOpponentGridPage(pageCards, cardW));
        }
        if (hasNext) {
            container.getChildren().add(buildOppNavButton("▼", () -> {
                opponentHandPages.put(p.name(), page + 1);
                populateOpponentHandColumn(container, panel, p, cardW, allCards);
            }, "OPP_NAV:" + p.name() + ":NEXT"));
        }
        // Side panels keep SIDE_PANEL_W; height grows automatically with content.
    }

    /** Lays {@code pageCards} as a 4-row × 2-column grid (left-to-right, top-to-bottom). */
    private VBox buildOpponentGridPage(List<CardDTO> pageCards, double cardW) {
        VBox grid = new VBox(HAND_GAP);
        grid.setAlignment(Pos.CENTER);
        HBox row = null;
        for (int i = 0; i < pageCards.size(); i++) {
            if (i % 2 == 0) {
                row = new HBox(HAND_GAP);
                row.setAlignment(Pos.CENTER);
                grid.getChildren().add(row);
            }
            row.getChildren().add(buildMiniCard(pageCards.get(i), cardW));
        }
        return grid;
    }

    private Button buildOppNavButton(String glyph, Runnable onClick, String navKey) {
        Button b = new Button(glyph);
        b.setFocusTraversable(false);
        b.setStyle("-fx-font-size: 13px; -fx-padding: 3 8 3 8; -fx-background-radius: 6;");
        b.setOnAction(_ -> onClick.run());
        markNav(b, navKey, "BUTTON", b::fire);
        return b;
    }

    // ── Hand cards view (used by both self and opponents) ────────────────────

    private int countHandCards(PlayerDTO p) {
        if (p.cards() == null) return 0;
        int n = 0;
        for (Set<CardDTO> set : p.cards().values()) {
            if (set != null) n += set.size();
        }
        return n;
    }

    /** Flattens the player's hand to a single list in CardType enum order. */
    private List<CardDTO> collectHand(PlayerDTO p) {
        List<CardDTO> all = new ArrayList<>();
        if (p.cards() == null) return all;
        for (CardType type : CardType.values()) {
            Set<CardDTO> set = p.cards().get(type);
            if (set != null) all.addAll(set);
        }
        return all;
    }

    /** Centered placeholder shown in an expanded panel when the player holds no cards. */
    private Label buildNoCardsLabel() {
        Label l = new Label("No cards yet");
        l.setFont(ImageCatalog.chalkFont(18));
        l.setStyle("-fx-text-fill: #f5deb3;");
        l.setAlignment(Pos.CENTER);
        l.setPadding(new Insets(6, 4, 6, 4));
        return l;
    }

    /** Builds a single-row HBox of mini-cards using a pre-computed card width. */
    private HBox buildHandCards(List<CardDTO> cards, double cardW) {
        HBox row = new HBox(HAND_GAP);
        row.setAlignment(Pos.CENTER_LEFT);
        for (CardDTO c : cards) {
            row.getChildren().add(buildMiniCard(c, cardW));
        }
        return row;
    }

    private Node buildMiniCard(CardDTO card, double w) {
        double h = w * CARD_ASPECT;
        StackPane cell = new StackPane();
        cell.setPrefSize(w, h);
        cell.setMinSize(w, h);
        cell.setMaxSize(w, h);
        ImageView img = safeImageView(() -> ImageCatalog.cardFront(card.id()));
        if (img != null) {
            img.setFitWidth(w);
            img.setFitHeight(h);
            img.setPreserveRatio(false);
            Rectangle clip = new Rectangle(w, h);
            clip.setArcWidth(w * 0.12);
            clip.setArcHeight(w * 0.12);
            img.setClip(clip);
            cell.getChildren().add(img);
        } else {
            Label fallback = new Label(card.id());
            fallback.setWrapText(true);
            fallback.setStyle("-fx-text-fill: #f5deb3; -fx-background-color: #3a2410;"
                    + " -fx-background-radius: 6; -fx-padding: 4; -fx-font-size: 10px;");
            cell.getChildren().add(fallback);
        }
        return cell;
    }

    private void renderRow(HBox container, List<CardDTO> cards, Row row, double cardW) {
        container.getChildren().clear();
        container.setSpacing(CARD_GAP);
        if (cards == null) return;
        boolean clickable = canPickRow(row);
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
        }

        if (clickable) {
            cell.setCursor(Cursor.HAND);
            cell.setOnMouseEntered(_ -> {
                if (!selectedMoves.contains(new Move(idx, row))) cell.setEffect(selectedGlow());;
            });
            cell.setOnMouseExited(_ -> {
                if (!selectedMoves.contains(new Move(idx, row))) cell.setEffect(null);;
            });
            cell.setOnMouseClicked(_ -> onCardClicked(row, idx, card, cell));
            markNav(cell, "CARD:" + row.name() + ":" + idx, "CARD", () -> onCardClicked(row, idx, card, cell));
            cell.getProperties().put("navSelected", selected);
        } else {
            cell.setEffect(darken());
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
        Pane cell = new Pane();
        cell.setPrefSize(tileW, tileH);
        cell.setMinSize(tileW, tileH);
        cell.setMaxSize(tileW, tileH);

        ImageView img = safeImageView(() -> ImageCatalog.offerTile(tile.id()));
        if (img != null) {
            img.setFitWidth(tileW);
            img.setFitHeight(tileH);
            img.setPreserveRatio(false);
            cell.getChildren().add(img);
        } else {
            Label fallback = new Label(tile.id());
            fallback.setStyle("-fx-text-fill: #f5deb3;");
            fallback.setLayoutX(4);
            fallback.setLayoutY(4);
            cell.getChildren().add(fallback);
        }

        if (tile.totem() != null) {
            ImageView totem = safeImageView(() -> ImageCatalog.totem3D(tile.totem()));
            if (totem != null) {
                double totemW = tileW * OFFER_TOTEM_W_FRAC;
                double totemH = totemW * (TOTEM3D_H / TOTEM3D_W);
                double[] slot = OFFER_SLOT.getOrDefault(tile.id(), new double[]{0.5, 0.28});
                double cx = slot[0] * tileW;
                double cy = slot[1] * tileH;
                totem.setFitWidth(totemW);
                totem.setPreserveRatio(true);
                totem.setLayoutX(cx - TOTEM_ANCHOR_X * totemW);
                totem.setLayoutY(cy - TOTEM_ANCHOR_Y * totemH);
                cell.getChildren().add(totem);
            }
        }

        boolean occupied = tile.totem() != null;
        if (clickable && !occupied) {
            double rectW = tileW * (217.0 / OFFER_TILE_W);
            double rectH = tileH * (121.0 / OFFER_TILE_H);
            double[] slot = OFFER_SLOT.getOrDefault(tile.id(), new double[]{0.5, 0.28});
            double cx = slot[0] * tileW;
            double cy = slot[1] * tileH;
            
            Rectangle hoverRect = new Rectangle(rectW, rectH);
            hoverRect.setX(cx - rectW / 2);
            hoverRect.setY(cy - rectH / 2);
            hoverRect.setFill(Color.web("#F2B035", 0.45));
            hoverRect.setStroke(Color.web("#F2B035"));
            hoverRect.setStrokeWidth(2.5);
            hoverRect.setArcWidth(8);
            hoverRect.setArcHeight(8);
            hoverRect.setVisible(false);
            hoverRect.setMouseTransparent(true);
            
            cell.getChildren().add(hoverRect);
            cell.getProperties().put("hoverRect", hoverRect);

            cell.setCursor(Cursor.HAND);
            cell.setOnMouseClicked(_ -> onOfferTileClicked(idx, tile));
            cell.setOnMouseEntered(_ -> hoverRect.setVisible(true));
            cell.setOnMouseExited(_ -> {
                if (cell != navFocusedNode) hoverRect.setVisible(false);
            });
            markNav(cell, "TILE:" + idx, "TILE", () -> onOfferTileClicked(idx, tile));
        } else if (!clickable) {
            cell.setEffect(darken());
        }
        return cell;
    }

    /**
     * Renders the turn-order tile to the left of the offer track. Picks the
     * {@code order_tile_<n>p} sprite for the current player count, sizes it to
     * the offer-tile height ({@code tileH}) so it sits in line with the rest of
     * the board, then drops each occupied cell's 3D totem so its support point
     * rests on the cell center. A right-side spacer of equal width keeps the
     * offer track centered with the card rows.
     */
    private void renderOrderTile(double tileH) {
        if (orderTilePane == null) return;
        orderTilePane.getChildren().clear();

        OrderTileDTO ot = (game != null && game.board() != null) ? game.board().orderTile() : null;
        int n = (game != null && game.players() != null) ? game.players().size() : 0;
        if (ot == null || n < 2 || n > 5 || tileH <= 0) {
            sizeOrderTile(0, 0);
            return;
        }

        double tileW = tileH * (ORDER_TILE_W / ORDER_TILE_H);
        sizeOrderTile(tileW, tileH);

        ImageView img = safeImageView(() -> ImageCatalog.orderTile(n));
        if (img != null) {
            img.setFitWidth(tileW);
            img.setFitHeight(tileH);
            img.setPreserveRatio(false);
            orderTilePane.getChildren().add(img);
        }

        List<OrderCellDTO> cells = ot.cells();
        if (cells == null) return;
        double[] ys = ORDER_CELL_Y[n - 2];
        double totemW = tileW * ORDER_TOTEM_W_FRAC;
        double totemH = totemW * (TOTEM3D_H / TOTEM3D_W);

        double rectW = tileW * (186.0 / ORDER_TILE_W);
        double rectH = tileH * (96.0 / ORDER_TILE_H);
        double arc   = rectW * 0.10;

        for (int i = 0; i < cells.size() && i < ys.length; i++) {
            Totem totem = cells.get(i).totem();
            if (totem == null) continue;
            double cx = ORDER_CELL_X * tileW;
            double cy = ys[i] * tileH;
            Rectangle rect = new Rectangle(rectW, rectH);
            rect.setArcWidth(arc);
            rect.setArcHeight(arc);
            rect.setFill(totemFill(totem));
            rect.setStroke(Color.web("#000000", 0.75));
            rect.setStrokeWidth(1.0);
            rect.setEffect(new DropShadow(5, 0, 2, Color.web("#000000", 0.55)));
            rect.setLayoutX(cx - rectW / 2.0);
            rect.setLayoutY(cy - rectH / 2.0);
            orderTilePane.getChildren().add(rect);
        }
    }

    private void sizeOrderTile(double w, double h) {
        orderTilePane.setMinSize(w, h);
        orderTilePane.setPrefSize(w, h);
        orderTilePane.setMaxSize(w, h);
    }

    /**
     * Renders the face-down future-era building decks to the right of the offer
     * track, separated from it and from each other. {@code remainingBuildings}
     * is a shrinking queue (server does {@code removeFirst()} on each era
     * advance), so index 0 is always the <em>next</em> era to enter and the
     * list size tells which eras are left: size 2 → eras [2, 3], size 1 → [3].
     * The era for a slot is therefore {@code (4 - size) + index}, not a fixed
     * index. The innermost deck (next to the offer track) is the next to be
     * consumed; once it is drawn onto the board the outer deck takes its place.
     * Only deck presence is known client-side (not the card count), so a single
     * card-back with a stacked shadow stands in for the pile, sized to the
     * offer-tile height so it sits in line.
     */
    private void renderBuildingDecks(double tileH) {
        if (buildingDecks == null) return;
        buildingDecks.getChildren().clear();
        if (game == null || game.board() == null || tileH <= 0) return;

        List<Boolean> remaining = game.board().remainingBuildings();
        if (remaining == null || remaining.isEmpty()) return;

        // Keep the first deck clearly detached from the offer track.
        HBox.setMargin(buildingDecks, new Insets(0, 0, 0, 18));

        int size = remaining.size();
        double deckH = tileH;
        double deckW = deckH / CARD_ASPECT;
        for (int i = 0; i < size; i++) {
            if (!Boolean.TRUE.equals(remaining.get(i))) continue;
            final int era = (4 - size) + i;
            if (era < 2 || era > 3) continue;
            ImageView back = safeImageView(() -> ImageCatalog.cardBack(CardType.BUILDINGS, era, false));
            if (back == null) continue;
            back.setFitWidth(deckW);
            back.setFitHeight(deckH);
            back.setPreserveRatio(false);
            // Round the corners like the board cards (arc = 12% of width).
            Rectangle clip = new Rectangle(deckW, deckH);
            clip.setArcWidth(deckW * 0.12);
            clip.setArcHeight(deckW * 0.12);
            back.setClip(clip);
            // Wrap so the pile shadow survives the clip (a clip on the image
            // itself would crop the shadow away).
            StackPane deckCell = new StackPane(back);
            deckCell.setEffect(deckShadow());
            buildingDecks.getChildren().add(deckCell);
        }
    }

    private void renderDeckCard(double tileH) {
        if (deckPane == null) return;
        deckPane.getChildren().clear();
        boolean empty = game == null || game.board() == null || game.board().isDeckEmpty();
        int era = (game != null) ? game.era() : 0;
        if (empty || era < 1 || era > 3 || tileH <= 0) {
            deckPane.setMinSize(0, 0);
            deckPane.setPrefSize(0, 0);
            deckPane.setMaxSize(0, 0);
            return;
        }
        double deckH = tileH;
        double deckW = deckH / CARD_ASPECT;
        deckPane.setMinSize(deckW, deckH);
        deckPane.setPrefSize(deckW, deckH);
        deckPane.setMaxSize(deckW, deckH);
        ImageView back = safeImageView(() -> ImageCatalog.deckCardBack(era));
        if (back == null) return;
        back.setFitWidth(deckW);
        back.setFitHeight(deckH);
        back.setPreserveRatio(false);
        Rectangle clip = new Rectangle(deckW, deckH);
        clip.setArcWidth(deckW * 0.12);
        clip.setArcHeight(deckW * 0.12);
        back.setClip(clip);
        StackPane deckCell = new StackPane(back);
        deckCell.setEffect(deckShadow());
        deckPane.getChildren().add(deckCell);
    }

    /** Soft offset shadow that makes a single card-back read as a small pile. */
    private static DropShadow deckShadow() {
        DropShadow s = new DropShadow(6, 3, 3, Color.rgb(0, 0, 0, 0.55));
        return s;
    }

    private static DropShadow selectedGlow() {
        DropShadow glow = new DropShadow(10, Color.WHITE);
        glow.setSpread(0.30);
        return glow;
    }

    /**
     * Darkens a node while keeping it fully opaque. Used for non-interactable
     * cards/tiles so they read as disabled without going transparent over the
     * background image.
     */
    private static ColorAdjust darken() {
        ColorAdjust ca = new ColorAdjust();
        ca.setBrightness(-0.45);
        return ca;
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
            setHint("Waiting for server...");
            confirmButton.setDisable(true);
            unmarkNav(confirmButton);
            return;
        }
        if (!isMyTurn()) {
            setHint(waitingHint());
            confirmButton.setDisable(true);
            unmarkNav(confirmButton);
            return;
        }
        if (phase == Phase.TOTEM_PLACEMENT) {
            setHint("Click an offer tile to place your totem.");
            confirmButton.setDisable(true);
            unmarkNav(confirmButton);
        } else if (phase == Phase.ACTION_EXECUTION || phase == Phase.EXTRA_MOVE) {
            int required = upperCount + lowerCount;
            long pickedUpper = selectedMoves.stream().filter(m -> m.row() == Row.UPPER).count();
            long pickedLower = selectedMoves.stream().filter(m -> m.row() == Row.LOWER).count();
            setMoveHint(upperCount, lowerCount, (int) pickedUpper, (int) pickedLower);
            confirmButton.setDisable(false);
            markNav(confirmButton, "CONFIRM", "BUTTON", this::onSendMove);
        } else {
            setHint(waitingHint());
            confirmButton.setDisable(true);
            unmarkNav(confirmButton);
        }
    }

    private void setHint(String text) {
        Text t = new Text(text);
        t.setFill(Color.web("#FDF3D3"));
        t.setFont(Font.font(13));
        hintLabel.getChildren().setAll(t);
    }

    private void setMoveHint(int upper, int lower, int pickedUpper, int pickedLower, int selected, int required) {
        Color white = Color.web("#FDF3D3");

        int upLeft   = upper - pickedUpper;
        int downLeft = lower - pickedLower;
        boolean upGrey   = upLeft <= 0;
        boolean downGrey = downLeft <= 0;

        ImageView upArrow = arrowIcon("/assets/general/arrow_up.png", upGrey);
        ImageView upCount = countGlyph(upLeft, "up", upGrey);

        ImageView downArrow = arrowIcon("/assets/general/arrow_down.png", downGrey);
        ImageView downCount = countGlyph(downLeft, "down", downGrey);

        Text sep = new Text("│");
        sep.setFont(Font.font(13));
        sep.setFill(Color.web("#FDF3D3", 0.45));

        Text selText = new Text(selected + " / " + required + " selected");
        selText.setFont(Font.font(13));
        selText.setFill(white);

        // HBox (not the raw TextFlow flow) so arrows and the number glyphs share
        // a common vertical center instead of being aligned on the text baseline,
        // which dropped the arrows below the digits.
        HBox row = new HBox(4, upArrow, upCount, gap(12), downArrow, downCount, gap(12), sep, gap(6), selText);
        row.setAlignment(Pos.CENTER);

        hintLabel.getChildren().setAll(row);
        hintLabel.setTextAlignment(TextAlignment.CENTER);
    }

    /** Fixed-width invisible spacer for the move-hint row. */
    private Node gap(double w) {
        Region r = new Region();
        r.setMinWidth(w);
        r.setPrefWidth(w);
        r.setMaxWidth(w);
        return r;
    }

    /** PNGs are baked at this oversampling; the ImageView downscales by it. */
    private static final double COUNT_GLYPH_SCALE = 2.0;

    /**
     * Move-count label as a pre-baked PNG from
     * {@code /assets/move_counts/<variant>/<n>.png} (variant: up | down).
     * The numbers are rendered offline by {@code tools/CountGlyphGenerator}
     * (ChristmasChalk font, white outer outline, round joins) so there is zero
     * runtime rasterisation — the earlier live {@link Node#snapshot} froze the
     * window while warming the cache. {@link ImageCatalog#load} caches the decode.
     * If {@code grey} is true, we apply a greyscale filter in the GUI.
     */
    private ImageView countGlyph(int value, String variant, boolean grey) {
        String filename;
        if (value < 0) {
            int clamped = Math.max(-20, value);
            filename = String.valueOf(clamped);
        } else {
            int clamped = Math.min(2, value);
            filename = String.valueOf(clamped);
        }
        Image img = ImageCatalog.load("/assets/move_counts/" + variant + "/" + filename + ".png");
        ImageView iv = new ImageView(img);
        iv.setFitHeight(img.getHeight() / COUNT_GLYPH_SCALE);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        if (grey) {
            ColorAdjust desaturate = new ColorAdjust();
            desaturate.setSaturation(-1);
            desaturate.setBrightness(-0.15);
            iv.setEffect(desaturate);
            iv.setOpacity(0.5);
        }
        return iv;
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

    private static Color totemFill(Totem totem) {
        return switch (totem) {
            case RED    -> Color.web("#ec6645");
            case WHITE  -> Color.web("#f7f1f0");
            case BLACK  -> Color.web("#421528");
            case BLUE   -> Color.web("#2391ae");
            case YELLOW -> Color.web("#f6c81f");
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
    // ── Keyboard Navigation (Spatial) ────────────────────────────────────────

    private void markNav(Node node, String key, String kind, Runnable action) {
        node.getProperties().put("navKey", key);
        node.getProperties().put("navKind", kind);
        node.getProperties().put("navAction", action);
    }

    private void unmarkNav(Node node) {
        node.getProperties().remove("navKey");
        node.getProperties().remove("navKind");
        node.getProperties().remove("navAction");
        if (node.getProperties().containsKey("navBaseStyle")) {
            node.setStyle((String) node.getProperties().get("navBaseStyle"));
        }
    }

    private void walkNav(Node n) {
        if (n.getProperties().containsKey("navKey")) {
            navIndex.put((String) n.getProperties().get("navKey"), n);
        }
        if (n instanceof Parent p) {
            for (Node child : p.getChildrenUnmodifiable()) {
                walkNav(child);
            }
        }
    }

    private void refreshNav() {
        navIndex.clear();
        walkNav(rootStack);

        if (navKey != null && !navIndex.containsKey(navKey)) {
            navKey = null;
            navFocusedNode = null;
        }

        for (Node n : navIndex.values()) {
            setNavHighlight(n, false);
        }
        if (navKey != null) {
            navFocusedNode = navIndex.get(navKey);
            setNavHighlight(navFocusedNode, true);
        }
    }

    private void setNavHighlight(Node node, boolean focused) {
        if (node == null) return;
        String kind = (String) node.getProperties().get("navKind");
        if (kind == null) return;

        if (focused) {
            if (kind.equals("CARD")) {
                node.setEffect(selectedGlow());
            } else if (kind.equals("TILE")) {
                Rectangle r = (Rectangle) node.getProperties().get("hoverRect");
                if (r != null) r.setVisible(true);
            } else if (kind.equals("BUTTON")) {
                if (!node.getProperties().containsKey("navBaseStyle")) {
                    node.getProperties().put("navBaseStyle", node.getStyle());
                }
                String baseStyle = (String) node.getProperties().get("navBaseStyle");
                if (baseStyle == null) baseStyle = "";
                node.setStyle(baseStyle + (baseStyle.endsWith(";") || baseStyle.isEmpty() ? "" : ";") + " -fx-border-color: white; -fx-border-width: 2; -fx-border-radius: 4;");
            }
        } else {
            if (kind.equals("CARD")) {
                Boolean sel = (Boolean) node.getProperties().get("navSelected");
                if (Boolean.TRUE.equals(sel)) {
                    node.setEffect(selectedGlow());
                } else {
                    node.setEffect(null);
                }
            } else if (kind.equals("TILE")) {
                Rectangle r = (Rectangle) node.getProperties().get("hoverRect");
                if (r != null) r.setVisible(false);
            } else if (kind.equals("BUTTON")) {
                if (node.getProperties().containsKey("navBaseStyle")) {
                    node.setStyle((String) node.getProperties().get("navBaseStyle"));
                }
            }
        }
    }

    private boolean handleNavKey(KeyCode code) {
        if (navIndex.isEmpty()) return false;

        // SPACE selects/deselects the focused card (or activates the focused tile /
        // page button) — mirrors the TUI's select key.
        if (code == KeyCode.SPACE) {
            return runFocusedAction();
        }

        // ENTER confirms the whole selection when a confirm action is available;
        // otherwise it activates the focused node (e.g. placing a totem on a tile).
        if (code == KeyCode.ENTER) {
            Node confirm = navIndex.get("CONFIRM");
            if (confirm != null && !confirm.isDisabled()) {
                onSendMove();
                return true;
            }
            return runFocusedAction();
        }

        double dx = 0; double dy = 0;
        if (code == KeyCode.UP) dy = -1;
        else if (code == KeyCode.DOWN) dy = 1;
        else if (code == KeyCode.LEFT) dx = -1;
        else if (code == KeyCode.RIGHT) dx = 1;
        else return false;

        if (navFocusedNode == null) {
            navFocusedNode = getLeftmostNode();
            if (navFocusedNode != null) {
                navKey = (String) navFocusedNode.getProperties().get("navKey");
                setNavHighlight(navFocusedNode, true);
            }
            return true;
        }

        Node next = geometricNext(navFocusedNode, dx, dy);
        if (next != null && next != navFocusedNode) {
            setNavHighlight(navFocusedNode, false);
            navFocusedNode = next;
            navKey = (String) navFocusedNode.getProperties().get("navKey");
            setNavHighlight(navFocusedNode, true);
        }
        return true;
    }

    /** Runs the navAction of the currently focused node, if any. */
    private boolean runFocusedAction() {
        if (navFocusedNode == null) return false;
        Runnable action = (Runnable) navFocusedNode.getProperties().get("navAction");
        if (action != null) {
            action.run();
            return true;
        }
        return false;
    }

    private Node getLeftmostNode() {
        Node best = null;
        double minScore = Double.MAX_VALUE;
        for (Node n : navIndex.values()) {
            if (!n.isVisible() || !n.isManaged()) continue;
            Bounds b = n.localToScene(n.getBoundsInLocal());
            if (b == null) continue;
            // score prioritizes top-left items.
            double score = b.getCenterX() + b.getCenterY();
            if (score < minScore) {
                minScore = score;
                best = n;
            }
        }
        return best;
    }

    private Node geometricNext(Node current, double dx, double dy) {
        Bounds curB = current.localToScene(current.getBoundsInLocal());
        if (curB == null) return null;
        double cx = curB.getCenterX();
        double cy = curB.getCenterY();

        Node best = null;
        double bestDist = Double.MAX_VALUE;

        for (Node n : navIndex.values()) {
            if (n == current) continue;
            if (!n.isVisible() || !n.isManaged()) continue;

            Bounds b = n.localToScene(n.getBoundsInLocal());
            if (b == null) continue;
            double nx = b.getCenterX();
            double ny = b.getCenterY();

            double dirX = nx - cx;
            double dirY = ny - cy;

            boolean valid = false;
            if (dx > 0 && dirX > 10) valid = true;
            if (dx < 0 && dirX < -10) valid = true;
            if (dy > 0 && dirY > 10) valid = true;
            if (dy < 0 && dirY < -10) valid = true;

            if (valid) {
                double dist;
                if (dx != 0) {
                    dist = Math.abs(dirX) + 4 * Math.abs(dirY);
                } else {
                    dist = Math.abs(dirY) + 4 * Math.abs(dirX);
                }

                if (dist < bestDist) {
                    bestDist = dist;
                    best = n;
                }
            }
        }
        return best;
    }

    // ── Rules overlay ────────────────────────────────────────────────────────

    private void setupRulesButton() {
        Button btn = new Button("?");
        btn.setStyle(
            "-fx-font-size: 18px; -fx-font-weight: bold;" +
            "-fx-min-width: 40px; -fx-min-height: 40px;" +
            "-fx-max-width: 40px; -fx-max-height: 40px;" +
            "-fx-background-radius: 20; -fx-padding: 0;"
        );
        btn.setOnAction(_ -> openRulesOverlay());
        StackPane.setAlignment(btn, Pos.BOTTOM_LEFT);
        StackPane.setMargin(btn, new Insets(0, 0, 14, 14));
        rootStack.getChildren().add(btn);
    }

    private void openRulesOverlay() {
        if (rulesPages == null) rulesPages = ImageCatalog.rulesPages();
        if (rulesPages.isEmpty()) return;

        rulesPageIndex = 0;
        rulesOverlay = new StackPane();
        rulesOverlay.setStyle("-fx-background-color: rgba(0,0,0,0.88);");

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(rulesOverlay.widthProperty());
        clip.heightProperty().bind(rulesOverlay.heightProperty());
        rulesOverlay.setClip(clip);

        ImageView iv = new ImageView(rulesPages.get(0));
        iv.setPreserveRatio(true);
        iv.fitWidthProperty().bind(rulesOverlay.widthProperty().multiply(0.88));
        iv.fitHeightProperty().bind(rulesOverlay.heightProperty().multiply(0.88));

        Button prev = new Button("◀");
        Button next = new Button("▶");
        Button close = new Button("✕");
        close.setStyle("-fx-font-size: 14px; -fx-min-width: 34px; -fx-min-height: 34px;" +
                       "-fx-max-width: 34px; -fx-max-height: 34px; -fx-background-radius: 17; -fx-padding: 0;");

        Label counter = new Label("1 / " + rulesPages.size());
        counter.setStyle("-fx-text-fill: #FDF3D3; -fx-font-size: 14px;");

        prev.setOnAction(_ -> {
            if (rulesPageIndex > 0) {
                rulesPageIndex--;
                iv.setImage(rulesPages.get(rulesPageIndex));
                counter.setText((rulesPageIndex + 1) + " / " + rulesPages.size());
            }
        });
        next.setOnAction(_ -> {
            if (rulesPageIndex < rulesPages.size() - 1) {
                rulesPageIndex++;
                iv.setImage(rulesPages.get(rulesPageIndex));
                counter.setText((rulesPageIndex + 1) + " / " + rulesPages.size());
            }
        });
        close.setOnAction(_ -> rootStack.getChildren().remove(rulesOverlay));
        rulesOverlay.setOnMouseClicked(e -> {
            if (e.getTarget() == rulesOverlay) rootStack.getChildren().remove(rulesOverlay);
        });

        HBox nav = new HBox(16, prev, counter, next);
        nav.setAlignment(Pos.CENTER);

        VBox content = new VBox(12, iv, nav);
        content.setAlignment(Pos.CENTER);
        content.setPickOnBounds(false);

        StackPane.setAlignment(close, Pos.TOP_RIGHT);
        StackPane.setMargin(close, new Insets(12, 12, 0, 0));

        rulesOverlay.getChildren().addAll(content, close);
        rootStack.getChildren().add(rulesOverlay);
    }

}
