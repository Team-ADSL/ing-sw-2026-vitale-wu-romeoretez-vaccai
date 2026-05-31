package org.adsl.client.view.gui.screens;

import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
    // Height-aware sizing: the shared card width is also capped so the two board
    // card rows + the self hand (CARD_VROWS stacked) plus the offer row fit the
    // window height, keeping the decks visible. V_CHROME_EST is the fixed vertical
    // overhead (header, hint, error, panel headers, chips, paddings) — tune if the
    // board leaves too much/too little vertical slack.
    private static final double V_CHROME_EST = 230.0;
    private static final double CARD_VROWS    = 3.0;
    private static final double TILE_ASPECT  = 1.65;
    private static final double TILE_MAX_W   = 80.0;
    private static final double TILE_MIN_W   = 42.0;
    private static final double HOVER_SCALE  = 1.15;
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
    @FXML private Pane       orderTilePane;
    @FXML private HBox       offerTrack;
    @FXML private HBox       buildingDecks;
    @FXML private HBox       bottomRow;
    @FXML private HBox       topPlayersBox;
    @FXML private VBox       leftPlayersBox;
    @FXML private VBox       rightPlayersBox;
    @FXML private HBox       selfPanelBox;
    @FXML private Label      hintLabel;
    @FXML private Button     confirmButton;
    @FXML private Label      errorLabel;
    @FXML private VBox       logBox;

    private GameDTO game;
    private final Set<Move> selectedMoves = new LinkedHashSet<>();
    private int upperCount = 0;
    private int lowerCount = 0;
    private boolean waitingServer = false;
    private int selfHandPage = 0;
    /** The single card width shared by every card row and hand, set each render
     *  by {@link #renderBoard()} from the most-constrained board row. */
    private double sharedCardW = CARD_MIN_W;
    /** Opponent names whose card hand is currently expanded. Persisted here (not
     *  on the panel node) so the expanded state survives a full board re-render. */
    private final Set<String> expandedOpponents = new HashSet<>();
    private final java.util.Map<String, Integer> opponentHandPages = new java.util.HashMap<>();
    // Fixed seat order, captured from the first game snapshot. The server
    // reorders players() by turn order each phase; rendering against this stable
    // list keeps each player's panel in the same slot for the whole game.
    private List<String> playerOrder;
    private final FloatingLog floatingLog;

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
        rootPane.setMinWidth(Region.USE_PREF_SIZE);
        rootPane.setMinHeight(Region.USE_PREF_SIZE);
        installBackground();
        // Only the window (rootStack) drives re-renders. We deliberately do NOT
        // listen on centerBox size: its height/width change as a *result* of
        // renderBoard (and of expanding an opponent panel), so listening there
        // created a feedback loop that re-rendered the board and collapsed any
        // expanded panel. Board zoom-out is handled by the global ResponsiveScaler.
        resizeDebounce.setOnFinished(_ -> renderBoard());
        rootStack.widthProperty().addListener((_, _, _) -> resizeDebounce.playFromStart());
        rootStack.heightProperty().addListener((_, _, _) -> resizeDebounce.playFromStart());
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
        double w = (centerBox != null) ? centerBox.getWidth() : 0;
        if (w <= 0) {
            // Fallback before first layout: estimate after side panels.
            double stack = rootStack.getWidth();
            if (stack <= 0) stack = 1280;
            w = stack - 2 * LR_PANEL_W;
        }
        return Math.max(200, w - 40);
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

        // One card size for EVERY card (top row, bottom row, hands): size the
        // most-constrained single-line card row, so cards never differ in size.
        int maxRowCards = Math.max(topN, botN);
        sharedCardW = LayoutMath.cardWidth(availableWidth(), maxRowCards, CARD_GAP, CARD_MIN_W, CARD_MAX_W);
        double offW = LayoutMath.cardWidth(availableWidth(), offN, 0, TILE_MIN_W, TILE_MAX_W);

        // Cap the shared card size by the available height so the two board card
        // rows + offer row + self hand all fit vertically (decks never clipped).
        // Below CARD_MIN_W the ResponsiveScaler zooms the whole board out.
        double winH = rootStack.getHeight();
        if (winH <= 0) winH = 800;
        double offerH = offW * TILE_ASPECT;
        double cardWByHeight = ((winH - offerH - V_CHROME_EST) / CARD_VROWS) / CARD_ASPECT;
        sharedCardW = Math.max(CARD_MIN_W, Math.min(sharedCardW, cardWByHeight));

        renderRow(topRow, top, Row.UPPER, sharedCardW);
        renderRow(bottomRow, bot, Row.LOWER, sharedCardW);
        renderOfferTrack(offerTrack, off, offW);
        renderOrderTile(offW * TILE_ASPECT);
        renderBuildingDecks(offW * TILE_ASPECT);

        renderSelfPanel();
        renderOpponentPanels();

        renderHintAndConfirm();
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
        int perPage = LayoutMath.perPage(stack - IDENTITY_COL_W - 40, cardW, HAND_GAP);
        boolean paginated = total > perPage;
        if (paginated) {
            perPage = LayoutMath.perPage(stack - IDENTITY_COL_W - 40 - (NAV_BTN_W * 2 + 16), cardW, HAND_GAP);
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
                // Recompute layout. For TOP slot the panel resizes to fit the
                // current page's content; for side slots the panel stays at
                // SIDE_PANEL_W (the column grows vertically only).
                if (topSlot) {
                    populateOpponentHandRow((HBox) hand, panel, p, cardW, allCards);
                } else {
                    populateOpponentHandColumn((VBox) hand, panel, p, cardW, allCards);
                }
                // Shrink only this panel if its expanded content would overflow
                // the window — runLater so prefHeight reflects the new content.
                Platform.runLater(() -> fitExpandedPanel(panel, topSlot));
            } else {
                expandedOpponents.remove(p.name());
                panel.setPrefWidth(basePanelW);
                panel.setMaxWidth(basePanelW);
                panel.setScaleX(1);
                panel.setScaleY(1);
                panel.setTranslateY(0);
            }
        };
        toggle.setOnAction(_ -> setExpanded.accept(!hand.isVisible()));

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
            }));
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
            }));
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
            }));
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
            }));
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

    private Button buildOppNavButton(String glyph, Runnable onClick) {
        Button b = new Button(glyph);
        b.setFocusTraversable(false);
        b.setStyle("-fx-font-size: 13px; -fx-padding: 3 8 3 8; -fx-background-radius: 6;");
        b.setOnAction(_ -> onClick.run());
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
            cell.setCursor(Cursor.HAND);
            cell.setOnMouseClicked(_ -> onOfferTileClicked(idx, tile));
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

        // Draw top-to-bottom so lower (nearer) totems overlap the ones behind.
        for (int i = 0; i < cells.size() && i < ys.length; i++) {
            Totem totem = cells.get(i).totem();
            if (totem == null) continue;
            ImageView t = safeImageView(() -> ImageCatalog.totem3D(totem));
            if (t == null) continue;
            t.setFitWidth(totemW);
            t.setPreserveRatio(true);
            double cx = ORDER_CELL_X * tileW;
            double cy = ys[i] * tileH;
            t.setLayoutX(cx - TOTEM_ANCHOR_X * totemW);
            t.setLayoutY(cy - TOTEM_ANCHOR_Y * totemH);
            orderTilePane.getChildren().add(t);
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

}
