package org.adsl.client.view.gui.screens;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.adsl.client.AppCoordinator;
import org.adsl.client.serverEvents.ErrorEvent;
import org.adsl.client.serverEvents.GameUpdateEvent;
import org.adsl.client.view.gui.Chip;
import org.adsl.client.view.gui.ImageCatalog;
import org.adsl.client.view.tui.CardCatalog;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Phase;
import org.adsl.shared.enums.Row;
import org.adsl.shared.enums.Totem;
import org.adsl.shared.model.CardDTO;
import org.adsl.shared.model.GameDTO;
import org.adsl.shared.model.OfferTileDTO;
import org.adsl.shared.model.PlayerDTO;
import org.adsl.shared.utils.Move;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Game screen. Mirrors the TUI {@link org.adsl.client.view.tui.screens.GameScreen}
 * behaviour: cards are clickable, selection is highlighted in green, building
 * cards are tinted purple and event cards orange. Selection is unconstrained
 * client-side — the server validates the request (issue #37).
 *
 * <p>Phase rules (matching the TUI):
 * <ul>
 *   <li>{@code TOTEM_PLACEMENT}: clicking an offer tile sends the move
 *       immediately. Tiles already occupied are disabled.</li>
 *   <li>{@code ACTION_EXECUTION} / {@code EXTRA_MOVE}: clicking a card on
 *       top/bottom row toggles its selection. The "Confirm Selection" button
 *       sends the accumulated set.</li>
 *   <li>Other phases / not-my-turn: every card and tile is disabled.</li>
 * </ul>
 *
 * <p>After sending a move the screen enters a transient
 * {@code WAITING_SERVER} state where everything is disabled until the next
 * {@link GameUpdateEvent} or {@link ErrorEvent} arrives.
 */
public class GameScreen extends GUIScreen {

    private static final String BORDER_SELECTED = "#4caf50";
    private static final String BORDER_BUILDING = "#8e24aa";
    private static final String BORDER_EVENT    = "#ef6c00";
    private static final String BORDER_NONE     = "transparent";

    private static final double CARD_WIDTH  = 110;
    private static final double TILE_WIDTH  = 90;
    private static final double CHIP_WIDTH  = 40;
    private static final double TOTEM_BADGE = 22;

    @FXML private Label headerLabel;
    @FXML private Label phaseLabel;
    @FXML private FlowPane topRow;
    @FXML private FlowPane offerTrack;
    @FXML private FlowPane bottomRow;
    @FXML private Label tribeLabel;
    @FXML private Label tribeContents;
    @FXML private VBox othersBox;
    @FXML private Label hintLabel;
    @FXML private Button confirmButton;
    @FXML private Label errorLabel;
    @FXML private VBox chatBox;
    @FXML private ScrollPane chatScroll;

    private GameDTO game;
    private final Set<Move> selectedMoves = new LinkedHashSet<>();
    private int upperCount = 0;
    private int lowerCount = 0;
    private boolean waitingServer = false;

    public GameScreen(AppCoordinator coordinator, String username, GameDTO game) {
        super(coordinator, username);
        this.game = game;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/game.fxml"));
            loader.setController(this);
            this.root = loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load game.fxml", e);
        }
        resolveMoveCounts();
        renderBoard();
    }

    @Override
    public GUIScreen visit(GameUpdateEvent e) {
        this.game = e.game();
        waitingServer = false;
        // Clear selection when leaving a card-pick phase, mirroring TUI setupActiveState().
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
        if (chatBox == null) return;
        Label entry = new Label(text);
        entry.setWrapText(true);
        entry.setStyle("-fx-text-fill: #424242;");
        chatBox.getChildren().add(entry);
        if (chatScroll != null) {
            Platform.runLater(() -> chatScroll.setVvalue(1.0));
        }
    }

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

    // ── Click handlers ──────────────────────────────────────────────────────

    private void onCardClicked(Row row, int idx, CardDTO card) {
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

    // ── Phase / turn helpers ────────────────────────────────────────────────

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

    /** Required pick counts derived from the offer tile where my totem sits. */
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

    // ── Rendering ───────────────────────────────────────────────────────────

    private void renderBoard() {
        if (game == null) return;
        headerLabel.setText(String.format("MESOS — Round %d/10  ·  Era %d  ·  Current: %s",
                game.round(), game.era(), totemLabel(game.currentPlayerTotem())));
        phaseLabel.setText("Phase: " + (game.phase() != null ? game.phase().name() : "—"));

        renderRow(topRow, game.board().topRow(), Row.UPPER);
        renderRow(bottomRow, game.board().lowRow(), Row.LOWER);
        renderOfferTrack(offerTrack, game.board().offerTrack());

        PlayerDTO me = findMe();
        if (me != null) {
            tribeLabel.setText("YOUR TRIBE");
            tribeContents.setText(summariseCards(me));
        } else {
            tribeLabel.setText("YOUR TRIBE");
            tribeContents.setText("");
        }

        othersBox.getChildren().clear();
        if (me != null) {
            othersBox.getChildren().add(buildPlayerRow(me, true));
        }
        for (PlayerDTO p : game.players()) {
            if (me != null && p.totem() == me.totem()) continue;
            othersBox.getChildren().add(buildPlayerRow(p, false));
        }

        renderHintAndConfirm();
    }

    /** Compact row: totem badge | name | food chip | pp chip | optional cards summary. */
    private Node buildPlayerRow(PlayerDTO p, boolean self) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        if (p.totem() != null) {
            ImageView totem = safeImageView(() -> ImageCatalog.totem2D(p.totem()));
            if (totem != null) {
                totem.setFitWidth(TOTEM_BADGE);
                totem.setPreserveRatio(true);
                row.getChildren().add(totem);
            }
        }

        Label name = new Label((self ? "★ " : "") + p.name());
        name.setStyle("-fx-font-weight: bold;");
        row.getChildren().add(name);

        row.getChildren().add(Chip.food(p.food(), CHIP_WIDTH));
        row.getChildren().add(Chip.pp(p.pp(), CHIP_WIDTH));

        if (self) {
            Label tag = new Label("(you)");
            tag.setStyle("-fx-text-fill: #666;");
            row.getChildren().add(tag);
        }
        return row;
    }

    private void renderRow(FlowPane pane, List<CardDTO> cards, Row row) {
        pane.getChildren().clear();
        if (cards == null) return;
        boolean clickable = canPickCards();
        int idx = 0;
        for (CardDTO c : cards) {
            pane.getChildren().add(buildCardCell(c, row, idx, clickable));
            idx++;
        }
    }

    private Node buildCardCell(CardDTO card, Row row, int idx, boolean clickable) {
        StackPane cell = new StackPane();
        cell.setAlignment(Pos.CENTER);

        if (card == null) {
            cell.setPrefSize(CARD_WIDTH, CARD_WIDTH * 1.484);
            cell.setStyle("-fx-border-color: #cccccc; -fx-border-style: dashed; -fx-border-width: 1; -fx-background-color: #f5f5f5;");
            Label empty = new Label("empty");
            empty.setStyle("-fx-text-fill: #999;");
            cell.getChildren().add(empty);
            return cell;
        }

        ImageView img = safeImageView(() -> ImageCatalog.cardFront(card.id()));
        if (img != null) {
            img.setFitWidth(CARD_WIDTH);
            img.setPreserveRatio(true);
            cell.getChildren().add(img);
        } else {
            Label fallback = new Label(card.id());
            fallback.setWrapText(true);
            cell.setPrefSize(CARD_WIDTH, CARD_WIDTH * 1.484);
            cell.getChildren().add(fallback);
        }

        boolean selected = selectedMoves.contains(new Move(idx, row));
        String borderColor;
        if (selected) {
            borderColor = BORDER_SELECTED;
        } else {
            CardType type = CardCatalog.typeFromId(card.id());
            if (type == CardType.BUILDINGS) borderColor = BORDER_BUILDING;
            else if (CardCatalog.isEvent(type)) borderColor = BORDER_EVENT;
            else borderColor = BORDER_NONE;
        }
        cell.setStyle("-fx-border-color: " + borderColor + "; -fx-border-width: 3; -fx-border-radius: 6;");

        if (clickable) {
            cell.setCursor(Cursor.HAND);
            cell.setOnMouseClicked(e -> onCardClicked(row, idx, card));
        } else {
            cell.setOpacity(0.55);
        }
        return cell;
    }

    private void renderOfferTrack(FlowPane pane, List<OfferTileDTO> tiles) {
        pane.getChildren().clear();
        if (tiles == null) return;
        boolean clickable = canPlaceTotem();
        int idx = 0;
        for (OfferTileDTO t : tiles) {
            pane.getChildren().add(buildOfferTileCell(t, idx, clickable));
            idx++;
        }
    }

    private Node buildOfferTileCell(OfferTileDTO tile, int idx, boolean clickable) {
        StackPane cell = new StackPane();
        cell.setAlignment(Pos.CENTER);

        ImageView img = safeImageView(() -> ImageCatalog.offerTile(tile.id()));
        if (img != null) {
            img.setFitWidth(TILE_WIDTH);
            img.setPreserveRatio(true);
            cell.getChildren().add(img);
        } else {
            cell.setPrefSize(TILE_WIDTH, TILE_WIDTH * 1.65);
            Label fallback = new Label(tile.id());
            cell.getChildren().add(fallback);
        }

        if (tile.totem() != null) {
            ImageView totem = safeImageView(() -> ImageCatalog.totem2D(tile.totem()));
            if (totem != null) {
                totem.setFitWidth(TILE_WIDTH * 0.5);
                totem.setPreserveRatio(true);
                cell.getChildren().add(totem);
            }
        }

        boolean occupied = tile.totem() != null;
        if (clickable && !occupied) {
            cell.setCursor(Cursor.HAND);
            cell.setOnMouseClicked(e -> onOfferTileClicked(idx, tile));
        } else if (!clickable) {
            cell.setOpacity(0.6);
        }
        return cell;
    }

    /** Wraps Image loading so a missing resource doesn't crash the screen. */
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

    private static String totemLabel(Totem t) {
        return t == null ? "—" : t.name();
    }

    private static String summariseCards(PlayerDTO p) {
        StringBuilder sb = new StringBuilder();
        if (p.cards() == null) return "";
        p.cards().forEach((type, set) -> {
            if (set != null && !set.isEmpty()) {
                if (sb.length() > 0) sb.append("  ");
                sb.append(type.name()).append(":").append(set.size());
            }
        });
        return sb.toString();
    }
}
