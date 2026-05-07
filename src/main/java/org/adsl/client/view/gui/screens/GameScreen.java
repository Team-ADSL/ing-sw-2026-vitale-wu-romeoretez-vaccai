package org.adsl.client.view.gui.screens;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.adsl.client.AppCoordinator;
import org.adsl.client.view.events.ErrorEvent;
import org.adsl.client.view.events.GameUpdateEvent;
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
 * Skeleton game screen. Displays the board state in plain labels and lets the
 * player submit a move via a free-text input field — full of the form
 * {@code OFFER:0} or {@code UPPER:1,LOWER:2}. Validation is delegated to the
 * server (per the existing design — see issue #37); this screen forwards the
 * request and surfaces server errors.
 */
public class GameScreen extends GUIScreen {

    @FXML private Label headerLabel;
    @FXML private Label phaseLabel;
    @FXML private FlowPane topRow;
    @FXML private FlowPane offerTrack;
    @FXML private FlowPane bottomRow;
    @FXML private Label tribeLabel;
    @FXML private Label tribeContents;
    @FXML private VBox othersBox;
    @FXML private TextField moveInput;
    @FXML private Label errorLabel;

    private GameDTO game;

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
        renderBoard();
    }

    @Override
    public GUIScreen visit(GameUpdateEvent e) {
        this.game = e.getGame();
        renderBoard();
        return this;
    }

    @Override
    public GUIScreen visit(ErrorEvent e) {
        if (errorLabel != null) errorLabel.setText(e.getMessage());
        return this;
    }

    @FXML
    private void onSendMove() {
        String text = moveInput.getText() == null ? "" : moveInput.getText().trim();
        if (text.isEmpty()) {
            errorLabel.setText("Type a move (e.g. OFFER:0 or UPPER:1,LOWER:2).");
            return;
        }
        Set<Move> moves = parseMoves(text);
        if (moves == null) {
            errorLabel.setText("Bad format. Use OFFER:0 or UPPER:1,LOWER:2.");
            return;
        }
        try {
            coordinator.makeMoveRequest(moves);
            errorLabel.setText("");
        } catch (Exception ex) {
            errorLabel.setText("Move failed: " + ex.getMessage());
        }
    }

    private Set<Move> parseMoves(String text) {
        Set<Move> out = new LinkedHashSet<>();
        for (String chunk : text.split(",")) {
            String[] parts = chunk.trim().split(":");
            if (parts.length != 2) return null;
            try {
                Row row = Row.valueOf(parts[0].trim().toUpperCase());
                int idx = Integer.parseInt(parts[1].trim());
                out.add(new Move(idx, row));
            } catch (Exception ex) {
                return null;
            }
        }
        return out;
    }

    // ── Rendering ───────────────────────────────────────────────────────────

    private void renderBoard() {
        if (game == null) return;
        headerLabel.setText(String.format("MESOS — Round %d/10  ·  Era %d  ·  Current: %s",
                game.round(), game.era(), totemLabel(game.currentPlayerTotem())));
        phaseLabel.setText("Phase: " + (game.phase() != null ? game.phase().name() : "—"));

        renderRow(topRow, game.board().topRow());
        renderRow(bottomRow, game.board().lowRow());
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
    }

    private void renderRow(FlowPane pane, List<CardDTO> cards) {
        pane.getChildren().clear();
        if (cards == null) return;
        int idx = 0;
        for (CardDTO c : cards) {
            String label = c == null
                    ? String.format("[%d] empty", idx)
                    : String.format("[%d] %s", idx, c.id());
            pane.getChildren().add(new Label(label));
            idx++;
        }
    }

    private void renderOfferTrack(FlowPane pane, List<OfferTileDTO> tiles) {
        pane.getChildren().clear();
        if (tiles == null) return;
        int idx = 0;
        for (OfferTileDTO t : tiles) {
            String moves = t.givesFood() ? "+3 food" : formatMoves(t.moves());
            String occupant = t.totem() != null ? totemLabel(t.totem()) : "free";
            pane.getChildren().add(new Label(String.format("[%d] %s  (%s)", idx, moves, occupant)));
            idx++;
        }
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
