package org.adsl.client.view.gui.screens;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.adsl.client.AppCoordinator;
import org.adsl.client.view.events.ErrorEvent;
import org.adsl.client.view.events.GameUpdateEvent;
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
import java.util.ArrayList;
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

    private static final String STYLE_SELECTED = "-fx-background-color: #4caf50; -fx-text-fill: white; -fx-font-weight: bold;";
    private static final String STYLE_BUILDING = "-fx-text-fill: #8e24aa; -fx-font-weight: bold;";
    private static final String STYLE_EVENT    = "-fx-text-fill: #ef6c00; -fx-font-weight: bold;";

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
        this.game = e.getGame();
        waitingServer = false;
        // Clear selection when leaving a card-pick phase, mirroring TUI setupActiveState().
        if (game.phase() != Phase.ACTION_EXECUTION && game.phase() != Phase.EXTRA_MOVE) {
            selectedMoves.clear();
        }
        resolveMoveCounts();
        renderBoard();
        return this;
    }

    @Override
    public GUIScreen visit(ErrorEvent e) {
        if (errorLabel != null) errorLabel.setText(e.getMessage());
        if (waitingServer) {
            waitingServer = false;
            renderBoard();
        }
        return this;
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
            coordinator.makeMoveRequest(toSend);
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
            coordinator.makeMoveRequest(Set.of(new Move(idx, Row.OFFER)));
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
            tribeLabel.setText(String.format("YOUR TRIBE — %s  ·  Food: %d  ·  PP: %d",
                    me.name(), me.food(), me.pp()));
            tribeContents.setText(summariseCards(me));
        } else {
            tribeLabel.setText("YOUR TRIBE");
            tribeContents.setText("");
        }

        othersBox.getChildren().clear();
        for (PlayerDTO p : game.players()) {
            if (me != null && p.totem() == me.totem()) continue;
            othersBox.getChildren().add(new Label(String.format("%s [%s]  F:%d  PP:%d  | %s",
                    p.name(), totemLabel(p.totem()), p.food(), p.pp(), summariseCards(p))));
        }

        renderHintAndConfirm();
    }

    private void renderRow(FlowPane pane, List<CardDTO> cards, Row row) {
        pane.getChildren().clear();
        if (cards == null) return;
        boolean clickable = canPickCards();
        int idx = 0;
        for (CardDTO c : cards) {
            final int i = idx;
            final CardDTO card = c;
            Button btn;
            if (c == null) {
                btn = new Button(String.format("[%d] empty", idx));
                btn.setDisable(true);
            } else {
                btn = new Button(String.format("[%d] %s", idx, c.id()));
                boolean selected = selectedMoves.contains(new Move(idx, row));
                if (selected) {
                    btn.setStyle(STYLE_SELECTED);
                } else {
                    CardType type = CardCatalog.typeFromId(c.id());
                    if (type == CardType.BUILDINGS) {
                        btn.setStyle(STYLE_BUILDING);
                    } else if (CardCatalog.isEvent(type)) {
                        btn.setStyle(STYLE_EVENT);
                    }
                }
                btn.setDisable(!clickable);
                btn.setOnAction(e -> onCardClicked(row, i, card));
            }
            pane.getChildren().add(btn);
            idx++;
        }
    }

    private void renderOfferTrack(FlowPane pane, List<OfferTileDTO> tiles) {
        pane.getChildren().clear();
        if (tiles == null) return;
        boolean clickable = canPlaceTotem();
        int idx = 0;
        for (OfferTileDTO t : tiles) {
            final int i = idx;
            final OfferTileDTO tile = t;
            String moves = t.givesFood() ? "+3 food" : formatMoves(t.moves());
            String occupant = t.totem() != null ? totemLabel(t.totem()) : "free";
            Button btn = new Button(String.format("[%d] %s  (%s)", idx, moves, occupant));
            btn.setDisable(!clickable || t.totem() != null);
            btn.setOnAction(e -> onOfferTileClicked(i, tile));
            pane.getChildren().add(btn);
            idx++;
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

    private static String formatMoves(Map<Row, Integer> moves) {
        if (moves == null || moves.isEmpty()) return "—";
        int up = moves.getOrDefault(Row.UPPER, 0);
        int lo = moves.getOrDefault(Row.LOWER, 0);
        List<String> parts = new ArrayList<>();
        if (up > 0) parts.add("UPPER×" + up);
        if (lo > 0) parts.add("LOWER×" + lo);
        return String.join(" ", parts);
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
