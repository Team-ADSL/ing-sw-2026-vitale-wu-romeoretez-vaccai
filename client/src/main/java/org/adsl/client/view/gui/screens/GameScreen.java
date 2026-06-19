package org.adsl.client.view.gui.screens;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
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
import org.adsl.client.view.gui.ChalkBadge;
import org.adsl.client.view.gui.Chip;
import org.adsl.client.view.gui.FloatingLog;
import org.adsl.client.view.gui.ImageCatalog;
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
 * Cards float without containers, rounded corners; hover scales up; selected
 * cards stay scaled with a white glow. Offer-track tiles are flush (no gap)
 * so they compose a continuous image. Card rows scale dynamically so every
 * row stays single-line at any window size.
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
    private static final double CHIP_WIDTH   = 50;
    private static final double CHIP_WIDTH_SM = 40;
    private static final double SELF_TOTEM   = 36;
    private static final double OPP_TOTEM    = 24;
    private static final double LR_PANEL_W     = 170;
    private static final double IDENTITY_COL_W = 100;
    /** Fixed space reserved for opponent panels in applyBoardScale (kept constant
     *  so the board never rescales when panel content changes). */
    private static final double TOP_PANEL_H_RESERVE  = 132.0;
    private static final double SIDE_PANEL_W_RESERVE = LR_PANEL_W + 40;
    /** Top inset pushing the LEFT/RIGHT panels down so their top lines up with the
     *  central "MESOS — …" header instead of the screen edge. */
    private static final double SIDE_PANEL_TOP       = 178.0;

    // ── Deck-summary icons (player panels) ────────────────────────────────────
    private static final double DECK_ICON_W       = 65.0;          // opponent icon width
    private static final double DECK_ICON_SELF_W  = 92.0;          // self icon width (larger)
    private static final double DECK_ICON_AR      = 147.0 / 174.0; // native icon height / width
    private static final double DECK_ICON_GAP     = 6.0;
    private static final int    DECK_SIDE_COLS    = 2;             // grid columns in narrow L/R panels
    private static final double BADGE_W           = 29.0;          // extra-info badge width (corner)
    private static final double BADGE_FONT        = 12.5;
    private static final double DISCOUNT_FONT     = 10.0;          // smaller, fits "-NN"
    private static final double DECK_COUNT_FONT   = 14.5;          // xN counts + inventor number
    private static final double DECK_COUNT_STROKE = 1.1;
    // Drill-down card widths shown when an icon toggle is open.
    private static final double DECK_CARD_W_SELF  = 100.0;        // self (large)
    private static final double DECK_CARD_W_OPP   = 62.0;         // opponents (smaller)
    private static final Color  BUILDER_INK  = Color.web("#541620");
    private static final Color  GATHERER_INK = Color.web("#f57a13");
    private static final Color  SHAMAN_INK   = Color.web("#9a445d");
    // Extra-info badge native pixel spaces + the centred coord of the dynamic number.
    private static final double PP_W = 161, PP_H = 137, PP_CX = 82, PP_CY = 50;
    private static final double FL_W = 188, FL_H = 127, FL_CX = 41, FL_CY = 73;
    private static final double SS_W = 188, SS_H = 127, SS_CX = 53, SS_CY = 62;

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
    @FXML private Label      headerLabel;
    @FXML private Label      phaseLabel;
    @FXML private HBox       topRow;
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
    /** The single card width shared by every card row and hand, set each render
     *  by {@link #renderBoard()} from the most-constrained board row. */
    private double sharedCardW = CARD_MIN_W;
    // Fixed seat order, captured from the first game snapshot. The server
    // reorders players() by turn order each phase; rendering against this stable
    // list keeps each player's panel in the same slot for the whole game.
    private List<String> playerOrder;
    /** Player name -> the card type whose cards are currently drilled into (icon
     *  toggle open). Persisted here so the open deck survives a board re-render. */
    private final java.util.Map<String, CardType> openDeck = new java.util.HashMap<>();
    private final FloatingLog floatingLog;
    /** Scaled board group; stored as a field so applyBoardScale can translate it. */
    private Group boardGroup;

    private List<Image> rulesPages;
    private int rulesPageIndex = 0;
    private StackPane rulesOverlay;
    private Node summaryCardOverlay;

    private StackPane overlayPane;
    private Label overlayTitle;
    private VBox overlayPlayerList;

    /**
     * Shared debounce timer for resize-driven re-renders. Each resize listener
     * resets it; the actual {@link #renderBoard()} fires only once the window
     * has been still for {@code RESIZE_DEBOUNCE_MS}, avoiding hundreds of full
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
        // Side panels start TOP-aligned, pushed down by SIDE_PANEL_TOP so their top
        // lines up with the central header (only the TOP panels keep the screen-edge
        // margin). The boxes fill the height; their content top-aligns inside.
        StackPane.setAlignment(leftPlayersBox, Pos.TOP_LEFT);
        leftPlayersBox.setPickOnBounds(false);
        leftPlayersBox.setMaxWidth(LR_PANEL_W + 40);
        leftPlayersBox.setPadding(new Insets(SIDE_PANEL_TOP, 6, 8, 14));
        StackPane.setAlignment(rightPlayersBox, Pos.TOP_RIGHT);
        rightPlayersBox.setPickOnBounds(false);
        rightPlayersBox.setMaxWidth(LR_PANEL_W + 40);
        rightPlayersBox.setPadding(new Insets(SIDE_PANEL_TOP, 14, 8, 6));
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

        buildEventsOverlay();
        rootStack.getChildren().add(overlayPane);
        StackPane.setAlignment(overlayPane, Pos.CENTER);

        floatingLog = new FloatingLog();

        if (logBox != null) {
            logBox.getChildren().setAll(floatingLog.getFloatingNode());
            logBox.setPickOnBounds(false);
        } else {
            rootStack.getChildren().add(floatingLog.getFloatingNode());
        }

        setupRulesButton();
        setupSummaryCardButton();
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
        rootStack.getChildren().addFirst(bgLayer);

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

    /**
     * Handles the "Confirm" button click (or its keyboard-nav equivalent).
     * Sends the currently selected card picks to the server as a move
     * request during {@code ACTION_EXECUTION} or {@code EXTRA_MOVE}. Shows
     * an error in {@link #errorLabel} if it isn't the player's turn, the
     * phase doesn't allow confirming, or the request fails.
     */
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
        errorLabel.setText("");
        renderHintAndConfirm();
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

        renderSelfPanel();
        renderOpponentPanels();

        renderHintAndConfirm();

        // Defer until the new content has been laid out so prefWidth/prefHeight
        // reflect it, then scale the whole board to fit the window and rebuild
        // the keyboard-navigation index against the freshly-built nodes.
        Platform.runLater(this::applyBoardScale);
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

        VBox identity = buildSelfIdentity(me);

        if (openDeck.containsKey(me.name())) {
            // Drill-down open: keep the self totem + name + chips unchanged; show the
            // cards in a row with the back button on the RIGHT.
            HBox cardsRow = new HBox(DECK_ICON_GAP);
            cardsRow.setAlignment(Pos.CENTER_LEFT);
            cardsRow.getChildren().addAll(deckCardNodes(me, openDeck.get(me.name()), DECK_CARD_W_SELF));
            selfPanelBox.getChildren().addAll(identity, cardsRow, buildDeckBackButton(me.name(), 18));
            return;
        }

        // Default: totem + name + chips on the left, the seven deck icons on the right.
        selfPanelBox.getChildren().addAll(identity, buildDeckIcons(me, false, DECK_ICON_SELF_W));
    }

    /** Self identity column: totem on top, name + food/pp chips below. */
    private VBox buildSelfIdentity(PlayerDTO me) {
        VBox left = new VBox(4);
        left.setAlignment(Pos.CENTER);
        left.setPrefWidth(IDENTITY_COL_W);
        left.setMinWidth(IDENTITY_COL_W);

        if (me.totem() != null) {
            ImageView totem = safeImageView(() -> ImageCatalog.totem2D(me.totem()));
            if (totem != null) {
                totem.setFitWidth(SELF_TOTEM);
                totem.setPreserveRatio(true);
                left.getChildren().add(totem);
            }
        }

        Label name = new Label("★ " + me.name());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #f5deb3;");
        HBox stats = new HBox(8, Chip.food(me.food(), CHIP_WIDTH), Chip.pp(me.pp(), CHIP_WIDTH));
        stats.setAlignment(Pos.CENTER);
        left.getChildren().addAll(name, stats);
        return left;
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

    private void renderOpponentPanels() {
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
            PlayerDTO opp = opponents.get(i);
            // When a side opponent's deck is open the panel mirrors the (wide) self
            // layout, so the narrow LR cap is lifted for that render to avoid clipping.
            boolean open = openDeck.containsKey(opp.name());
            Node panel = buildOpponentPanel(opp, slot);
            switch (slot) {
                case LEFT  -> { leftPlayersBox.setMaxWidth(open ? Region.USE_COMPUTED_SIZE : LR_PANEL_W + 40); leftPlayersBox.getChildren().add(panel); }
                case RIGHT -> { rightPlayersBox.setMaxWidth(open ? Region.USE_COMPUTED_SIZE : LR_PANEL_W + 40); rightPlayersBox.getChildren().add(panel); }
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
        boolean topSlot = (slot == Slot.TOP);
        boolean open = openDeck.containsKey(p.name());

        VBox panel = new VBox(6);
        panel.getStyleClass().add("panel");
        panel.setPadding(new Insets(8, 10, 8, 10));
        panel.setAlignment(Pos.TOP_CENTER);

        if (open) {
            // Drill-down: identity (totem + name + food/pp) on the LEFT in opponent
            // sizing, that type's cards (smaller than the self panel) in the middle,
            // back button on the right — all inside the usual panel background.
            HBox cardsRow = new HBox(DECK_ICON_GAP);
            cardsRow.setAlignment(Pos.CENTER_LEFT);
            cardsRow.getChildren().addAll(deckCardNodes(p, openDeck.get(p.name()), DECK_CARD_W_OPP));
            HBox content = new HBox(8, buildOpponentHeader(p, false), cardsRow, buildDeckBackButton(p.name(), 13));
            content.setAlignment(Pos.CENTER_LEFT);
            panel.getChildren().add(content);
        } else {
            // Default: full header (totem + chips) + the seven icons.
            panel.getChildren().addAll(buildOpponentHeader(p, topSlot),
                    buildDeckIcons(p, !topSlot, DECK_ICON_W));
        }

        // Narrow cap only for the closed icon view; the open row lays out
        // horizontally and would be clipped by LR_PANEL_W.
        if (!topSlot && !open) {
            panel.setPrefWidth(LR_PANEL_W);
            panel.setMaxWidth(LR_PANEL_W);
        }
        return panel;
    }

    // ── Deck-summary icons (replaces the old hand view) ───────────────────────

    /** The seven hand card types, in the order their icons are laid out. */
    private static final List<CardType> DECK_ORDER = List.of(
            CardType.HUNTER, CardType.GATHERER, CardType.BUILDER, CardType.SHAMAN,
            CardType.INVENTOR, CardType.ARTIST, CardType.BUILDINGS);

    /**
     * Builds the seven deck-summary icons for a player. {@code grid=false} lays
     * them in a single row (wide self/TOP panels); {@code grid=true} uses a
     * {@link #DECK_SIDE_COLS}-column grid for the narrow LEFT/RIGHT panels.
     */
    private Pane buildDeckIcons(PlayerDTO p, boolean grid, double iconW) {
        if (!grid) {
            HBox row = new HBox(DECK_ICON_GAP);
            row.setAlignment(Pos.CENTER);
            for (CardType t : DECK_ORDER) row.getChildren().add(buildDeckIconCell(p, t, iconW));
            return row;
        }
        GridPane g = new GridPane();
        g.setHgap(DECK_ICON_GAP);
        g.setVgap(DECK_ICON_GAP);
        g.setAlignment(Pos.CENTER);
        for (int i = 0; i < DECK_ORDER.size(); i++) {
            g.add(buildDeckIconCell(p, DECK_ORDER.get(i), iconW), i % DECK_SIDE_COLS, i / DECK_SIDE_COLS);
        }
        return g;
    }

    /** One deck icon with its {@code xN} count and any type-specific extra-info badge(s). */
    private StackPane buildDeckIconCell(PlayerDTO p, CardType type, double iconW) {
        double w = iconW;
        double h = w * DECK_ICON_AR;
        StackPane cell = new StackPane();
        cell.setMinSize(w, h);
        cell.setPrefSize(w, h);
        cell.setMaxSize(w, h);

        ImageView icon = safeImageView(() -> ImageCatalog.deckIcon(type));
        if (icon != null) {
            icon.setFitWidth(w);
            icon.setPreserveRatio(true);
            cell.getChildren().add(icon);
        }

        switch (type) {
            case BUILDER -> {
                // Bottom-left: total builder PP on the laurel. Top-right: discount
                // (shown as a negative, with a smaller font so "-NN" fits).
                StackPane pp = buildBadge(safeImage(ImageCatalog::ppCard), PP_W, PP_H, BADGE_W,
                        String.valueOf(p.builderPP()), BUILDER_INK, BADGE_FONT, PP_CX, PP_CY);
                StackPane.setAlignment(pp, Pos.BOTTOM_LEFT);
                StackPane disc = buildBadge(safeImage(ImageCatalog::foodLoss), FL_W, FL_H, BADGE_W,
                        "-" + p.builderDiscount(), BUILDER_INK, DISCOUNT_FONT, FL_CX, FL_CY);
                StackPane.setAlignment(disc, Pos.TOP_RIGHT);
                cell.getChildren().addAll(pp, disc);
            }
            case GATHERER -> {
                // Top-right: discount (negative) on the drumstick — same drawing as
                // the builder but with the maroon strokes repainted orange.
                StackPane disc = buildBadge(safeImage(ImageCatalog::foodLossOrange), FL_W, FL_H, BADGE_W,
                        "-" + p.gathererDiscount(), GATHERER_INK, DISCOUNT_FONT, FL_CX, FL_CY);
                StackPane.setAlignment(disc, Pos.TOP_RIGHT);
                cell.getChildren().add(disc);
            }
            case SHAMAN -> {
                // Top-right: total shaman stars.
                StackPane stars = buildBadge(safeImage(ImageCatalog::shamanStars), SS_W, SS_H, BADGE_W,
                        String.valueOf(p.shamanStars()), SHAMAN_INK, BADGE_FONT, SS_CX, SS_CY);
                StackPane.setAlignment(stars, Pos.TOP_RIGHT);
                cell.getChildren().add(stars);
            }
            case INVENTOR -> {
                // Top-right: number of unique inventor icons (explained on hover).
                ImageView uniq = ChalkBadge.number(String.valueOf(p.inventorUniqueIcons()),
                        DECK_COUNT_FONT, Color.WHITE, Color.BLACK, DECK_COUNT_STROKE);
                StackPane.setAlignment(uniq, Pos.TOP_RIGHT);
                Tooltip.install(uniq, new Tooltip("Number of unique icons"));
                cell.getChildren().add(uniq);
            }
            default -> { }
        }

        // Bottom-right of every icon: how many cards of this type the player holds.
        int count = (p.cards() == null || p.cards().get(type) == null) ? 0 : p.cards().get(type).size();
        ImageView xn = ChalkBadge.number("x" + count, DECK_COUNT_FONT, Color.WHITE, Color.BLACK, DECK_COUNT_STROKE);
        StackPane.setAlignment(xn, Pos.BOTTOM_RIGHT);
        cell.getChildren().add(xn);

        // Clicking an icon drills into this player's cards of that type (toggle);
        // hovering lights it up white like a selected board card.
        cell.setCursor(Cursor.HAND);
        cell.setOnMouseEntered(_ -> cell.setEffect(selectedGlow()));
        cell.setOnMouseExited(_ -> cell.setEffect(null));
        cell.setOnMouseClicked(_ -> { openDeck.put(p.name(), type); renderBoard(); });

        return cell;
    }

    /**
     * A small corner extra-info badge: the {@code img} with {@code text} drawn
     * centred on the image's {@code (cx,cy)} reference pixel. Any per-type
     * recolouring is baked into {@code img} by the caller (see
     * {@link ImageCatalog#foodLossOrange()}).
     */
    private StackPane buildBadge(Image img, double nativeW, double nativeH, double dispW,
                                 String text, Color ink, double fontSize, double cx, double cy) {
        double dispH = dispW * nativeH / nativeW;
        StackPane sp = new StackPane();
        sp.setMinSize(dispW, dispH);
        sp.setPrefSize(dispW, dispH);
        sp.setMaxSize(dispW, dispH);
        sp.setMouseTransparent(true);
        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(dispW);
            iv.setFitHeight(dispH);
            iv.setPreserveRatio(false);
            sp.getChildren().add(iv);
        }
        ImageView num = ChalkBadge.number(text, fontSize, ink, null, 0);
        StackPane.setAlignment(num, Pos.CENTER);
        num.setTranslateX((cx - nativeW / 2.0) * (dispW / nativeW));
        num.setTranslateY((cy - nativeH / 2.0) * (dispH / nativeH));
        sp.getChildren().add(num);
        return sp;
    }

    // ── Deck drill-down (icon toggle → that type's cards) ─────────────────────

    /** The card images for {@code p}'s cards of {@code type} (sorted), or a single
     *  "—" placeholder when the player holds none of that type. */
    private List<Node> deckCardNodes(PlayerDTO p, CardType type, double cw) {
        List<CardDTO> cards = new ArrayList<>();
        if (p.cards() != null && p.cards().get(type) != null) cards.addAll(p.cards().get(type));
        cards.sort(Comparator.comparing(CardDTO::id));
        List<Node> nodes = new ArrayList<>();
        if (cards.isEmpty()) {
            Label none = new Label("—");
            none.setFont(ImageCatalog.chalkFont(16));
            none.setStyle("-fx-text-fill: #f5deb3; -fx-padding: 6;");
            nodes.add(none);
        } else {
            for (CardDTO c : cards) nodes.add(buildDeckCard(c, cw));
        }
        return nodes;
    }

    /** Gold back button that closes the drill-down for {@code playerName}, at the
     *  given font size (the self panel uses a larger one). */
    private Button buildDeckBackButton(String playerName, int fontSize) {
        Button b = new Button("↩");
        b.setFocusTraversable(false);
        int padV = Math.round(fontSize * 0.3f);
        int padH = Math.round(fontSize * 0.7f);
        b.setStyle("-fx-font-size: " + fontSize + "px; -fx-padding: "
                + padV + " " + padH + " " + padV + " " + padH + "; -fx-background-radius: 8;");
        b.setOnAction(_ -> { openDeck.remove(playerName); renderBoard(); });
        return b;
    }

    /** A single rounded card image used in the drill-down view. */
    private Node buildDeckCard(CardDTO card, double w) {
        double h = w * CARD_ASPECT;
        StackPane cell = new StackPane();
        cell.setMinSize(w, h);
        cell.setPrefSize(w, h);
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
            Label fb = new Label(card.id());
            fb.setWrapText(true);
            fb.setStyle("-fx-text-fill: #f5deb3; -fx-background-color: #3a2410;"
                    + " -fx-background-radius: 6; -fx-padding: 4; -fx-font-size: 10px;");
            cell.getChildren().add(fb);
        }
        return cell;
    }

    /** Loads an image, swallowing failures (mirrors {@link #safeImageView}). */
    private static Image safeImage(java.util.function.Supplier<Image> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            System.err.println("[GUI] image load failed: " + e.getMessage());
            return null;
        }
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
                if (!selectedMoves.contains(new Move(idx, row))) cell.setEffect(selectedGlow());
            });
            cell.setOnMouseExited(_ -> {
                if (!selectedMoves.contains(new Move(idx, row))) cell.setEffect(null);
            });
            cell.setOnMouseClicked(_ -> onCardClicked(row, idx, card, cell));
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

            cell.setCursor(Cursor.HAND);
            cell.setOnMouseClicked(_ -> onOfferTileClicked(idx, tile));
            cell.setOnMouseEntered(_ -> hoverRect.setVisible(true));
            cell.setOnMouseExited(_ -> hoverRect.setVisible(false));
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

        // Collect the colored rectangles (not the background ImageView) so hover
        // can fade them out to reveal the order-tile content underneath.
        List<javafx.scene.shape.Rectangle> totemRects = orderTilePane.getChildren().stream()
                .filter(c -> c instanceof javafx.scene.shape.Rectangle)
                .map(c -> (javafx.scene.shape.Rectangle) c)
                .toList();
        if (!totemRects.isEmpty()) {
            orderTilePane.setOnMouseEntered(_ -> totemRects.forEach(r -> r.setOpacity(0.15)));
            orderTilePane.setOnMouseExited(_  -> totemRects.forEach(r -> r.setOpacity(1.0)));
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
        buildingDecks.getChildren().clear();
        if (game == null || game.board() == null || tileH <= 0) return;

        List<Boolean> remaining = game.board().remainingBuildings();
        if (remaining == null || remaining.isEmpty()) return;

        // Keep the first deck clearly detached from the offer track.
        HBox.setMargin(buildingDecks, new Insets(0, 0, 0, 18));

        int size = remaining.size();
        double deckW = tileH / CARD_ASPECT;
        for (int i = 0; i < size; i++) {
            if (!Boolean.TRUE.equals(remaining.get(i))) continue;
            final int era = (4 - size) + i;
            if (era < 2 || era > 3) continue;
            ImageView back = safeImageView(() -> ImageCatalog.cardBack(CardType.BUILDINGS, era, false));
            if (back == null) continue;
            back.setFitWidth(deckW);
            back.setFitHeight(tileH);
            back.setPreserveRatio(false);
            // Round the corners like the board cards (arc = 12% of width).
            Rectangle clip = new Rectangle(deckW, tileH);
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
        double deckW = tileH / CARD_ASPECT;
        deckPane.setMinSize(deckW, tileH);
        deckPane.setPrefSize(deckW, tileH);
        deckPane.setMaxSize(deckW, tileH);
        ImageView back = safeImageView(() -> ImageCatalog.deckCardBack(era));
        if (back == null) return;
        back.setFitWidth(deckW);
        back.setFitHeight(tileH);
        back.setPreserveRatio(false);
        Rectangle clip = new Rectangle(deckW, tileH);
        clip.setArcWidth(deckW * 0.12);
        clip.setArcHeight(deckW * 0.12);
        back.setClip(clip);
        StackPane deckCell = new StackPane(back);
        deckCell.setEffect(deckShadow());
        deckPane.getChildren().add(deckCell);
    }

    /** Soft offset shadow that makes a single card-back read as a small pile. */
    private static DropShadow deckShadow() {
        return new DropShadow(6, 3, 3, Color.rgb(0, 0, 0, 0.55));
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
            return;
        }
        if (!isMyTurn()) {
            setHint(waitingHint());
            confirmButton.setDisable(true);
            return;
        }
        if (phase == Phase.TOTEM_PLACEMENT) {
            setHint("Click an offer tile to place your totem.");
            confirmButton.setDisable(true);
        } else if (phase == Phase.ACTION_EXECUTION || phase == Phase.EXTRA_MOVE) {
            long pickedUpper = selectedMoves.stream().filter(m -> m.row() == Row.UPPER).count();
            long pickedLower = selectedMoves.stream().filter(m -> m.row() == Row.LOWER).count();
            setMoveHint(upperCount, lowerCount, (int) pickedUpper, (int) pickedLower);
            confirmButton.setDisable(false);
        } else {
            setHint(waitingHint());
            confirmButton.setDisable(true);
        }
    }

    private static final String MOVE_HINT_ID = "__moveHintRow";

    private void setHint(String text) {
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

    private void setMoveHint(int upper, int lower, int pickedUpper, int pickedLower) {
        int upLeft   = upper - pickedUpper;
        int downLeft = lower - pickedLower;
        boolean upGrey   = upLeft <= 0;
        boolean downGrey = downLeft <= 0;

        ImageView upArrow   = arrowIcon("/assets/general/arrow_up.png",   upGrey);
        ImageView upCount   = countGlyph(upLeft,   "up",   upGrey);
        ImageView downArrow = arrowIcon("/assets/general/arrow_down.png", downGrey);
        ImageView downCount = countGlyph(downLeft, "down", downGrey);

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
            HBox.setHgrow(combined, javafx.scene.layout.Priority.ALWAYS);
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
        int clamped = Math.max(0, Math.min(2, value));
        Image img = ImageCatalog.load("/assets/move_counts/" + variant + "/" + clamped + ".png");
        ImageView iv = new ImageView(img);
        iv.setFitHeight(24);   // fixed — matches arrowIcon height so the row never shifts
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

    // ── Summary card overlay ──────────────────────────────────────────────────

    private void setupSummaryCardButton() {
        Button btn = new Button("SC");
        btn.setStyle(
            "-fx-font-size: 13px; -fx-font-weight: bold;" +
            "-fx-min-width: 40px; -fx-min-height: 40px;" +
            "-fx-max-width: 40px; -fx-max-height: 40px;" +
            "-fx-background-radius: 20; -fx-padding: 0;"
        );
        btn.setOnAction(_ -> toggleSummaryCard());
        StackPane.setAlignment(btn, Pos.BOTTOM_LEFT);
        StackPane.setMargin(btn, new Insets(0, 0, 14, 62));
        rootStack.getChildren().add(btn);
    }

    private void toggleSummaryCard() {
        if (summaryCardOverlay != null && rootStack.getChildren().contains(summaryCardOverlay)) {
            // Toggle off
            rootStack.getChildren().remove(summaryCardOverlay);
            summaryCardOverlay = null;
            return;
        }

        final int[] index = {0};
        final String[] cardPaths = {
                "/assets/cards_front_cropped/summary_card.png",
                "/assets/cards_back_cropped/summary_card.png"
        };

        // Small size: visible but compact, sitting just above the SC button
        double cardW = 130.0;
        double cardH = cardW * CARD_ASPECT;

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
            Image img = safeImage(() -> new Image(getClass().getResourceAsStream(cardPaths[index[0]])));
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
            index[0] = (index[0] + 1) % cardPaths.length;
            updateImage.run();
        });

        HBox container = new HBox(8, cardPane, nextBtn);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPickOnBounds(false);
        container.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        summaryCardOverlay = container;

        // Anchor bottom-left, just above the SC button (button bottom=14 + height=40 + gap=8 = 62)
        StackPane.setAlignment(summaryCardOverlay, Pos.BOTTOM_LEFT);
        StackPane.setMargin(summaryCardOverlay, new Insets(0, 0, 62, 14));

        rootStack.getChildren().add(summaryCardOverlay);
    }

}
