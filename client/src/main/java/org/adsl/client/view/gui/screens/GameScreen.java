package org.adsl.client.view.gui.screens;

import static org.adsl.client.view.gui.GuiConstants.*;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.text.TextFlow;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
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
import javafx.util.Duration;
import org.adsl.client.AppCoordinator;
import org.adsl.client.view.GameViewModel;
import org.adsl.client.serverEvents.ErrorEvent;
import org.adsl.client.serverEvents.EventsTriggeredEvent;
import org.adsl.client.serverEvents.GameUpdateEvent;
import org.adsl.client.view.gui.FxUtil;
import org.adsl.client.view.gui.FloatingLog;
import org.adsl.client.view.gui.game.BoardRenderer;
import org.adsl.client.view.gui.game.EventsOverlay;
import org.adsl.client.view.gui.game.MoveHintBar;
import org.adsl.client.view.gui.game.PlayerPanelsRenderer;
import org.adsl.client.view.gui.game.RulesOverlay;
import org.adsl.client.view.gui.game.SummaryCardOverlay;
import org.adsl.shared.enums.Phase;
import org.adsl.shared.enums.Row;
import org.adsl.shared.enums.Totem;
import org.adsl.shared.model.CardDTO;
import org.adsl.shared.model.GameDTO;
import org.adsl.shared.model.OfferTileDTO;
import org.adsl.shared.utils.Move;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
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

    // Layout constants are defined in GuiConstants.
    private static final double SIDE_PANEL_W_RESERVE = PlayerPanelsRenderer.LR_PANEL_W + 40;

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
    /** View-agnostic read model over {@link #game}; rebuilt whenever the snapshot
     *  changes. Holds the shared turn/phase logic (see {@link GameViewModel}). */
    private GameViewModel vm;
    private final Set<Move> selectedMoves = new LinkedHashSet<>();
    private int upperCount = 0;
    private int lowerCount = 0;
    private boolean waitingServer = false;
    /** Identity of the picking turn the current selection belongs to; when it
     *  changes (new player or phase) the pending selection is dropped so a past
     *  turn's picks never bleed into a new ACTION_EXECUTION / EXTRA_MOVE turn. */
    private String selectionTurnToken = "none";
    private final FloatingLog floatingLog;
    /** Scaled board group; stored as a field so applyBoardScale can translate it. */
    private Group boardGroup;

    private EventsOverlay eventsOverlay;
    /** Renders the central board block (rows, offer track, order tile, decks). */
    private BoardRenderer boardRenderer;
    /** Renders the self + opponent panels (deck icons, drill-down). Owns the
     *  drill-down open state and the stable seat order. */
    private PlayerPanelsRenderer panelsRenderer;
    /** Renders the bottom hint row (plain hints or the move-count arrows). */
    private MoveHintBar hintBar;

    /**
     * Shared debounce timer for resize-driven re-renders. Each resize listener
     * resets it; the actual {@link #renderBoard()} fires only once the window
     * has been still for {@code RESIZE_DEBOUNCE_MS} (defined in GuiConstants),
     * avoiding hundreds of full board rebuilds per second while dragging the window edge.
     */
    private final PauseTransition resizeDebounce =
            new PauseTransition(Duration.millis(RESIZE_DEBOUNCE_MS));

    public GameScreen(AppCoordinator coordinator, String username, GameDTO game) {
        super(coordinator, username);
        this.game = game;
        this.vm = new GameViewModel(game, username);
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
        leftPlayersBox.setMaxWidth(PlayerPanelsRenderer.LR_PANEL_W + 40);
        leftPlayersBox.setPadding(new Insets(SIDE_PANEL_TOP, 6, 8, 14));
        StackPane.setAlignment(rightPlayersBox, Pos.TOP_RIGHT);
        rightPlayersBox.setPickOnBounds(false);
        rightPlayersBox.setMaxWidth(PlayerPanelsRenderer.LR_PANEL_W + 40);
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

        eventsOverlay = new EventsOverlay();
        rootStack.getChildren().add(eventsOverlay);
        StackPane.setAlignment(eventsOverlay, Pos.CENTER);

        boardRenderer = new BoardRenderer(topRow, bottomRow, offerTrack, buildingDecks,
                orderTilePane, deckPane, new BoardRenderer.Callbacks() {
            @Override public boolean canPickRow(Row row) { return GameScreen.this.canPickRow(row); }
            @Override public boolean canPlaceTotem()     { return GameScreen.this.canPlaceTotem(); }
            @Override public boolean isSelected(Move m)  { return selectedMoves.contains(m); }
            @Override public void onCardClicked(Row row, int idx, CardDTO card, StackPane pane) {
                GameScreen.this.onCardClicked(row, idx, card, pane);
            }
            @Override public void onOfferTileClicked(int idx, OfferTileDTO tile) {
                GameScreen.this.onOfferTileClicked(idx, tile);
            }
        });

        panelsRenderer = new PlayerPanelsRenderer(selfPanelBox, topPlayersBox, leftPlayersBox,
                rightPlayersBox, username, this::renderBoard);

        hintBar = new MoveHintBar(hintLabel, confirmButton);

        floatingLog = new FloatingLog();

        if (logBox != null) {
            logBox.getChildren().setAll(floatingLog.getFloatingNode());
            logBox.setPickOnBounds(false);
        } else {
            rootStack.getChildren().add(floatingLog.getFloatingNode());
        }

        new RulesOverlay(rootStack);
        new SummaryCardOverlay(rootStack);
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
        return FxUtil.image(path);
    }

    @Override
    public GUIScreen onEnter() {
        rootStack.requestFocus();
        return null;
    }

    // ── Events overlay ───────────────────────────────────────────────────────

    @Override
    public GUIScreen visit(EventsTriggeredEvent e) {
        String title = e.eventTitle();
        if (title == null || title.isBlank()) {
            return this;
        }
        String log = e.logMessage();
        Platform.runLater(() -> {
            eventsOverlay.show(title, log);
            if (log != null && !log.isBlank()) appendChat(log);
        });
        return this;
    }

    // ── Server events ────────────────────────────────────────────────────────

    @Override
    public GUIScreen visit(GameUpdateEvent e) {
        eventsOverlay.hide();
        this.game = e.game();
        this.vm = new GameViewModel(this.game, username);
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
        return vm.isMyTurn();
    }

    private boolean canPlaceTotem() {
        return !waitingServer && vm.isMyTurn() && vm.isTotemPlacement();
    }

    private boolean canPickCards() {
        return !waitingServer && vm.isMyTurn() && vm.isPicking();
    }

    /** During EXTRA_MOVE only the top row is pickable; otherwise any row when picking. */
    private boolean canPickRow(Row row) {
        if (!canPickCards()) return false;
        return !vm.isExtraMove() || row == Row.UPPER;
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
        return vm.pickingTurnToken();
    }

    private void resolveMoveCounts() {
        GameViewModel.MoveCounts mc = vm.moveCounts();
        upperCount = mc.upper();
        lowerCount = mc.lower();
    }

    // ── Rendering ────────────────────────────────────────────────────────────

    private static String formatPhase(Phase phase) {
        return GameViewModel.formatPhase(phase);
    }

    private void renderBoard() {
        if (game == null) return;
        headerLabel.setText(String.format("MESOS — Round %d/10  ·  Era %d  ·  Current: %s",
                game.round(), game.era(), nameFor(game.currentPlayerTotem())));
        phaseLabel.setText("Phase: " + formatPhase(game.phase()));

        // The central board (rows, offer track, order tile, decks) is drawn at a
        // fixed reference size by the BoardRenderer; the whole block is then scaled
        // as one unit (applyBoardScale) to fit the window so nothing is clipped.
        boardRenderer.render(game);

        panelsRenderer.render(game);

        renderHintAndConfirm();

        // Defer until the new content has been laid out so prefWidth/prefHeight
        // reflect it, then scale the whole board to fit the window.
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

    private static DropShadow selectedGlow() {
        return FxUtil.selectedGlow();
    }

    private void renderHintAndConfirm() {
        Phase phase = game.phase();
        if (waitingServer) {
            hintBar.setHint("Waiting for server...");
            confirmButton.setDisable(true);
            return;
        }
        if (!isMyTurn()) {
            hintBar.setHint(waitingHint());
            confirmButton.setDisable(true);
            return;
        }
        if (phase == Phase.TOTEM_PLACEMENT) {
            hintBar.setHint("Click an offer tile to place your totem.");
            confirmButton.setDisable(true);
        } else if (phase == Phase.ACTION_EXECUTION || phase == Phase.EXTRA_MOVE) {
            long pickedUpper = selectedMoves.stream().filter(m -> m.row() == Row.UPPER).count();
            long pickedLower = selectedMoves.stream().filter(m -> m.row() == Row.LOWER).count();
            hintBar.setMoveHint(upperCount, lowerCount, (int) pickedUpper, (int) pickedLower);
            confirmButton.setDisable(false);
        } else {
            hintBar.setHint(waitingHint());
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
        return vm.playerName(totem);
    }

}
