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
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
import org.adsl.client.view.tui.CardCatalog;
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
import java.util.EnumSet;
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

    private static final double BG_VP_LEFT   = 485;
    private static final double BG_VP_TOP    = 80;
    private static final double BG_VP_W      = 2260 - 485;   // 1775
    private static final double BG_VP_H      = 1470 - 80;    // 1390

    // Card-row footprints in v4.png pixel space (both rows normalised to the
    // same width/height; only the Y differs).
    private static final double ROW_X     = 940;
    private static final double ROW_W     = 860;   // 1800 − 940
    private static final double ROW_H     = 170;   // fits within both rects, clear of offer y≈692
    private static final double TOP_ROW_Y = 500;
    private static final double LOW_ROW_Y = 870;

    private static final double CARD_ASPECT  = 1.484;
    private static final double CARD_MAX_W   = 130.0;
    private static final double CARD_GAP     = 10.0;
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
    private ImageView bgView;
    private Pane tribesLayer;
    private Pane offerLayer;
    private Pane cardsLayer;

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
        setupBackground();
        setupTribesLayer();
        setupOfferLayer();
        setupCardsLayer();
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

    // ── Background + aerial overlays ─────────────────────────────────────────

    /**
     * Click-rectangle in v4.png pixel space, in DTO-list order (i.e. matching
     * {@code game.board().offerTrack()} indices for the active player count).
     */
    private record TileRect(double x, double y, double w, double h) {
        double cx() { return x + w / 2; }
        double cy() { return y + h / 2; }
    }

    private static final TileRect[] RECTS_2P = {
            new TileRect(1485, 698, 106, 147),
            new TileRect(1368, 696, 115, 157),
            new TileRect(1260, 694, 105, 165),
            new TileRect(1158, 692, 100, 155),
    };
    private static final TileRect[] RECTS_3P = {
            new TileRect(1537, 701, 107, 140),
            new TileRect(1423, 700, 109, 142),
            new TileRect(1310, 699, 109, 141),
            new TileRect(1207, 697, 100, 139),
            new TileRect(1102, 697, 102, 141),
    };
    private static final TileRect[] RECTS_4P = {
            new TileRect(1576, 704, 126, 132),
            new TileRect(1474, 702,  95, 134),
            new TileRect(1368, 700,  98, 136),
            new TileRect(1248, 699, 114, 134),
            new TileRect(1145, 697,  97, 145),
            new TileRect(1033, 695, 107, 141),
    };
    private static final TileRect[] RECTS_5P = {
            new TileRect(1634, 706, 120, 122),
            new TileRect(1530, 705,  98, 127),
            new TileRect(1423, 705,  96, 124),
            new TileRect(1309, 706,  99, 125),
            new TileRect(1206, 703,  94, 128),
            new TileRect(1086, 701, 111, 129),
            new TileRect( 987, 700,  91, 126),
    };

    private static TileRect[] rectsFor(int numPlayers) {
        return switch (numPlayers) {
            case 2 -> RECTS_2P;
            case 3 -> RECTS_3P;
            case 4 -> RECTS_4P;
            case 5 -> RECTS_5P;
            default -> null;
        };
    }

    private void setupBackground() {
        try {
            bgView = new ImageView(ImageCatalog.aerialBackground());
            bgView.setPreserveRatio(false);
            bgView.setSmooth(true);
            bgView.setMouseTransparent(true);
            rootStack.setStyle("");
            rootStack.getChildren().add(0, bgView);
        } catch (Exception e) {
            System.err.println("[GUI] background load failed: " + e.getMessage());
        }
    }

    private void setupTribesLayer() {
        tribesLayer = new Pane();
        tribesLayer.setPickOnBounds(false);
        rootStack.getChildren().add(1, tribesLayer);
    }

    private void setupOfferLayer() {
        offerLayer = new Pane();
        offerLayer.setPickOnBounds(false);
        rootStack.getChildren().add(2, offerLayer);
    }

    /**
     * Pulls topRow/bottomRow out of the FXML contentBox VBox so they can be
     * positioned absolutely over the aerial background. Inserts a Region spacer
     * in contentBox so the remaining items (tribe info, hint, confirm, error)
     * hug the bottom of the screen instead of collapsing into the header.
     */
    private void setupCardsLayer() {
        cardsLayer = new Pane();
        cardsLayer.setPickOnBounds(false);
        // Cards must sit ABOVE the contentBox labels so they're visible and clickable.
        rootStack.getChildren().add(cardsLayer);

        contentBox.setPickOnBounds(false);
        contentBox.getChildren().remove(topRow);
        contentBox.getChildren().remove(bottomRow);
        contentBox.getChildren().remove(offerTrack);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        int afterPhase = contentBox.getChildren().indexOf(phaseLabel) + 1;
        contentBox.getChildren().add(afterPhase, spacer);

        topRow.setPickOnBounds(false);
        bottomRow.setPickOnBounds(false);
        cardsLayer.getChildren().addAll(topRow, bottomRow);
    }

    private void positionCardRows(AerialTransform t) {
        if (topRow == null || bottomRow == null) return;
        double rowX = t.sx(ROW_X);
        double rowW = t.ss(ROW_W);
        double rowH = t.ss(ROW_H);
        double topY = t.sy(TOP_ROW_Y);
        double botY = t.sy(LOW_ROW_Y);

        topRow.setLayoutX(rowX);
        topRow.setLayoutY(topY);
        topRow.setMinSize(rowW, rowH);
        topRow.setPrefSize(rowW, rowH);
        topRow.setMaxSize(rowW, rowH);

        bottomRow.setLayoutX(rowX);
        bottomRow.setLayoutY(botY);
        bottomRow.setMinSize(rowW, rowH);
        bottomRow.setPrefSize(rowW, rowH);
        bottomRow.setMaxSize(rowW, rowH);
    }

    /** Card width that fits N cards in the given box, height-aware. Never overflows. */
    private double computeCardWidthInBox(int n, double availW, double availH, double gap) {
        if (n <= 0 || availW <= 0 || availH <= 0) return CARD_MAX_W;
        double byWidth  = (availW - gap * Math.max(0, n - 1)) / n;
        double byHeight = availH / CARD_ASPECT;
        return Math.max(1, Math.min(byWidth, byHeight));
    }

    /** Computed once per resize from the bg image: scale + top-left on screen. */
    private record AerialTransform(double scale, double leftX, double topY, double imgW, double imgH) {
        double sx(double v4x) { return leftX + v4x * scale; }
        double sy(double v4y) { return topY  + v4y * scale; }
        double ss(double v4) { return v4 * scale; }
    }

    private AerialTransform aerialTransform() {
        if (bgView == null || bgView.getImage() == null) return null;
        double screenW = rootStack.getWidth();
        double screenH = rootStack.getHeight();
        if (screenW <= 0 || screenH <= 0) return null;
        Image img = bgView.getImage();
        double scale = Math.min(screenW / BG_VP_W, screenH / BG_VP_H);
        double imgW = img.getWidth() * scale;
        double imgH = img.getHeight() * scale;
        double leftX = screenW / 2 - (BG_VP_LEFT + BG_VP_W / 2) * scale;
        double topY  = screenH / 2 - (BG_VP_TOP  + BG_VP_H / 2) * scale;
        return new AerialTransform(scale, leftX, topY, imgW, imgH);
    }

    private void updateOverlays() {
        AerialTransform t = aerialTransform();
        if (t == null) return;

        // Background uses StackPane centering, so we set translate relative to center.
        bgView.setFitWidth(t.imgW());
        bgView.setFitHeight(t.imgH());
        bgView.setTranslateX(t.imgW() / 2 - (BG_VP_LEFT + BG_VP_W / 2) * t.scale());
        bgView.setTranslateY(t.imgH() / 2 - (BG_VP_TOP  + BG_VP_H / 2) * t.scale());

        renderTribesLayer(t);
        renderOfferLayer(t);
        positionCardRows(t);
    }

    private void renderTribesLayer(AerialTransform t) {
        if (tribesLayer == null) return;
        tribesLayer.getChildren().clear();
        if (game == null) return;

        Set<Totem> active = EnumSet.noneOf(Totem.class);
        for (PlayerDTO p : game.players()) if (p.totem() != null) active.add(p.totem());

        for (Totem totem : Totem.values()) {
            boolean isActive = active.contains(totem);
            try {
                Image overlay = isActive
                        ? ImageCatalog.tribeOverlay(totem)
                        : ImageCatalog.nofireOverlay(totem);
                ImageView iv = new ImageView(overlay);
                iv.setPreserveRatio(false);
                iv.setSmooth(true);
                iv.setFitWidth(t.imgW());
                iv.setFitHeight(t.imgH());
                iv.setLayoutX(t.leftX());
                iv.setLayoutY(t.topY());

                if (isActive) {
                    iv.setPickOnBounds(false); // only opaque tribe-tent pixels are clickable
                    iv.setCursor(Cursor.HAND);
                    DropShadow halo = new DropShadow(28, Color.WHITE);
                    halo.setSpread(0.6);
                    iv.setOnMouseEntered(_ -> iv.setEffect(halo));
                    iv.setOnMouseExited(_ -> iv.setEffect(null));
                    iv.setOnMouseClicked(_ -> onTribeClicked(totem));
                } else {
                    iv.setMouseTransparent(true);
                }
                tribesLayer.getChildren().add(iv);
            } catch (Exception ex) {
                System.err.println("[GUI] tribe overlay load failed for " + totem + ": " + ex.getMessage());
            }
        }
    }

    private void renderOfferLayer(AerialTransform t) {
        if (offerLayer == null) return;
        offerLayer.getChildren().clear();
        if (game == null || game.board() == null) return;

        List<OfferTileDTO> tiles = game.board().offerTrack();
        if (tiles == null || tiles.isEmpty()) return;

        int numPlayers = game.players() != null ? game.players().size() : 0;
        TileRect[] rects = rectsFor(numPlayers);
        if (rects == null || rects.length != tiles.size()) {
            System.err.println("[GUI] offer-rect count " + (rects == null ? "null" : rects.length)
                    + " != tiles " + tiles.size() + " for " + numPlayers + " players");
            return;
        }

        // Pre-positioned whole-row image (same scale & origin as bg).
        try {
            ImageView rowView = new ImageView(ImageCatalog.offerTileRow(numPlayers));
            rowView.setPreserveRatio(false);
            rowView.setSmooth(true);
            rowView.setFitWidth(t.imgW());
            rowView.setFitHeight(t.imgH());
            rowView.setLayoutX(t.leftX());
            rowView.setLayoutY(t.topY());
            rowView.setMouseTransparent(true);
            offerLayer.getChildren().add(rowView);
        } catch (Exception ex) {
            System.err.println("[GUI] offer-row image load failed: " + ex.getMessage());
        }

        boolean canPlace = canPlaceTotem();
        for (int i = 0; i < tiles.size(); i++) {
            OfferTileDTO tile = tiles.get(i);
            TileRect r = rects[i];

            // Transparent click rectangle.
            Rectangle hit = new Rectangle(t.ss(r.w()), t.ss(r.h()));
            hit.setFill(Color.TRANSPARENT);
            hit.setLayoutX(t.sx(r.x()));
            hit.setLayoutY(t.sy(r.y()));
            boolean occupied = tile.totem() != null;
            if (canPlace && !occupied) {
                hit.setCursor(Cursor.HAND);
                final int idx = i;
                hit.setOnMouseClicked(_ -> onOfferTileClicked(idx, tile));
            }
            offerLayer.getChildren().add(hit);

            // 3D totem on placed tile, centered in the rectangle.
            if (occupied) {
                try {
                    ImageView totem = new ImageView(ImageCatalog.totem3D(tile.totem()));
                    totem.setPreserveRatio(true);
                    totem.setSmooth(true);
                    double size = t.ss(Math.min(r.w(), r.h())) * 1.1;
                    totem.setFitHeight(size);
                    totem.setLayoutX(t.sx(r.cx()) - size / 2);
                    totem.setLayoutY(t.sy(r.cy()) - size / 2);
                    totem.setMouseTransparent(true);
                    offerLayer.getChildren().add(totem);
                } catch (Exception ex) {
                    System.err.println("[GUI] 3D totem load failed: " + ex.getMessage());
                }
            }
        }
    }

    private void onTribeClicked(Totem totem) {
        System.out.println("[GUI] tribe clicked: " + totem);
        // TODO: open the tribe's deck panel
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
        if (CardCatalog.isEvent(CardCatalog.typeFromId(card.id()))) return;
        int maxForRow = (row == Row.UPPER) ? upperCount : lowerCount;
        if (maxForRow == 0) return;
        Move m = new Move(idx, row);
        if (selectedMoves.contains(m)) {
            selectedMoves.remove(m);
        } else {
            long selectedInRow = selectedMoves.stream().filter(mv -> mv.row() == row).count();
            if (selectedInRow >= maxForRow) {
                Move toRemove = selectedMoves.stream().filter(mv -> mv.row() == row).findFirst().orElse(null);
                if (toRemove != null) selectedMoves.remove(toRemove);
            }
            selectedMoves.add(m);
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
        return Math.max(200, w - 80);
    }

    private void applyContentScale() {
        double availH = rootStack.getHeight();
        if (availH <= 0 || contentBox == null) return;
        double prefH = contentBox.prefHeight(availableWidth() + 80);
        if (prefH <= 0) return;
        double scale = Math.min(1.0, availH / prefH);
        contentBox.setScaleX(scale);
        contentBox.setScaleY(scale);
        // Compensate for scale-from-center so top edge stays pinned to top of pane.
        contentBox.setTranslateY(-prefH / 2.0 * (1.0 - scale));
        updateOverlays();
    }


    private void renderBoard() {
        if (game == null) return;
        headerLabel.setText(String.format("MESOS — Round %d/10  ·  Era %d  ·  Current: %s",
                game.round(), game.era(), nameFor(game.currentPlayerTotem())));
        phaseLabel.setText("Phase: " + (game.phase() != null ? game.phase().name() : "—"));

        List<CardDTO> top = game.board().topRow();
        List<CardDTO> bot = game.board().lowRow();

        int topN = (int) top.stream().filter(java.util.Objects::nonNull).count();
        int botN = (int) bot.stream().filter(java.util.Objects::nonNull).count();

        // Card size scales with the actual row footprint in screen space so all
        // cards always fit in the v4-defined ROW_W × ROW_H box.
        AerialTransform t = aerialTransform();
        double rowW = t != null ? t.ss(ROW_W) : 800;
        double rowH = t != null ? t.ss(ROW_H) : 150;
        double topW = computeCardWidthInBox(topN, rowW, rowH, CARD_GAP);
        double botW = computeCardWidthInBox(botN, rowW, rowH, CARD_GAP);

        renderRow(topRow, top, Row.UPPER, topW);
        renderRow(bottomRow, bot, Row.LOWER, botW);

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
        Platform.runLater(this::applyContentScale);
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
        boolean canPick = canPickCards();
        int maxForRow = (row == Row.UPPER) ? upperCount : lowerCount;
        long selectedInRow = selectedMoves.stream().filter(mv -> mv.row() == row).count();
        boolean rowFull = maxForRow > 0 && selectedInRow >= maxForRow;

        int idx = 0;
        for (CardDTO c : cards) {
            if (c == null) { idx++; continue; }
            boolean isEvent = CardCatalog.isEvent(CardCatalog.typeFromId(c.id()));
            boolean rowDisabled = maxForRow == 0;
            boolean selected = selectedMoves.contains(new Move(idx, row));
            boolean cardClickable = canPick && !isEvent && !rowDisabled;
            boolean dimmed = !canPick || rowDisabled || (rowFull && !selected);
            container.getChildren().add(buildCardCell(c, row, idx, cardClickable, dimmed, cardW));
            idx++;
        }
    }

    private Node buildCardCell(CardDTO card, Row row, int idx, boolean clickable, boolean dimmed, double cardW) {
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
        } else if (dimmed) {
            ColorAdjust darken = new ColorAdjust();
            darken.setBrightness(-0.45);
            cell.setEffect(darken);
        }

        if (clickable) {
            cell.setCursor(Cursor.HAND);
            if (!dimmed) {
                cell.setOnMouseEntered(_ -> {
                    if (!selectedMoves.contains(new Move(idx, row))) scale(cell, HOVER_SCALE);
                });
                cell.setOnMouseExited(_ -> {
                    if (!selectedMoves.contains(new Move(idx, row))) scale(cell, 1.0);
                });
            }
            cell.setOnMouseClicked(_ -> onCardClicked(row, idx, card, cell));
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
