package org.adsl.client.view.tui.screens;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import org.adsl.client.AppCoordinator;
import org.adsl.client.view.tui.CardCatalog;
import org.adsl.client.view.tui.OfferTileCatalog;
import org.adsl.client.view.tui.events.*;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Phase;
import org.adsl.shared.enums.Row;
import org.adsl.shared.enums.Totem;
import org.adsl.shared.model.*;
import org.adsl.shared.utils.Move;

import java.io.IOException;
import java.util.*;

/**
 * Main game screen implemented as a non-blocking state machine.
 *
 * Server events ({@link GameUpdateEvent}, {@link EndGameEvent}) and input
 * events ({@link ConfirmEvent}, {@link SelectEvent}, navigate events) are all
 * dispatched through the visitor pattern. No blocking calls are made — the
 * TUI loop handles rendering and event dispatch.
 */
public class GameScreen implements Screen {

    private enum SubState {
        PASS_SCREEN,
        MY_TURN_TOTEM,
        MY_TURN_CARDS,
        NOT_MY_TURN,
        WAITING_SERVER
    }

    private final com.googlecode.lanterna.screen.Screen terminal;
    private final AppCoordinator coordinator;
    private final String username;

    private GameDTO game;
    private Totem myTotem;
    private SubState subState;

    // Card selection state
    private int selectionIndex = 0;
    private boolean onTopRow = true;
    private final Set<Move> selectedMoves = new LinkedHashSet<>();
    private int upperCount = 0;
    private int lowerCount = 0;

    private String pendingError = null;

    public GameScreen(com.googlecode.lanterna.screen.Screen terminal,
                      AppCoordinator coordinator,
                      String username,
                      GameDTO initialGame) {
        this.terminal = terminal;
        this.coordinator = coordinator;
        this.username = username;
        this.game = initialGame;
    }

    @Override
    public void onEnter() {
        myTotem = findMyTotem();
        subState = SubState.PASS_SCREEN;
    }

    @Override
    public void render() throws IOException {
        terminal.clear();
        TextGraphics tg = terminal.newTextGraphics();
        TerminalSize sz = terminal.getTerminalSize();

        switch (subState) {
            case PASS_SCREEN    -> renderPassScreen(tg, sz);
            case MY_TURN_TOTEM  -> renderBoard(tg, sz, selectionIndex, Collections.emptySet());
            case MY_TURN_CARDS  -> renderBoard(tg, sz, -1, selectedMoves);
            case NOT_MY_TURN,
                 WAITING_SERVER -> renderBoard(tg, sz, -1, Collections.emptySet());
        }

        if (pendingError != null) {
            flashError(tg, sz, pendingError);
            pendingError = null;
        }

        terminal.refresh();
    }

    // ── Server event visitors ─────────────────────────────────────────────────

    @Override
    public Screen visit(GameUpdateEvent e) {
        GameDTO prev = this.game;
        this.game = e.getGame();
        this.myTotem = findMyTotem();

        if (game.phase() == Phase.END_GAME) {
            subState = SubState.NOT_MY_TURN;
            return this;
        }

        Totem prevTotem = (prev != null) ? prev.currentPlayerTotem() : null;
        if (!Objects.equals(prevTotem, game.currentPlayerTotem())) {
            subState = SubState.PASS_SCREEN;
        } else if (subState == SubState.WAITING_SERVER) {
            setupActiveState();
        }
        return this;
    }

    @Override
    public Screen visit(EndGameEvent e) {
        return new EndGameScreen(terminal, e.getResults());
    }

    // ── Input event visitors ──────────────────────────────────────────────────

    @Override
    public Screen visit(ConfirmEvent e) {
        switch (subState) {
            case PASS_SCREEN   -> confirmPassScreen();
            case MY_TURN_TOTEM -> confirmTotemPlacement();
            case MY_TURN_CARDS -> confirmCardSelection();
            default            -> {}
        }
        return this;
    }

    @Override
    public Screen visit(SelectEvent e) {
        if (subState == SubState.MY_TURN_CARDS) {
            Row row = onTopRow ? Row.UPPER : Row.LOWER;
            List<CardDTO> cards = onTopRow ? game.board().topRow() : game.board().lowRow();
            toggleCardSelection(row, cards);
        }
        return this;
    }

    @Override
    public Screen visit(NavigateLeftEvent e) {
        if (subState == SubState.MY_TURN_TOTEM) {
            selectionIndex = Math.max(0, selectionIndex - 1);
        } else if (subState == SubState.MY_TURN_CARDS) {
            selectionIndex = Math.max(0, selectionIndex - 1);
        }
        return this;
    }

    @Override
    public Screen visit(NavigateRightEvent e) {
        if (subState == SubState.MY_TURN_TOTEM) {
            selectionIndex = Math.min(game.board().offerTrack().size() - 1, selectionIndex + 1);
        } else if (subState == SubState.MY_TURN_CARDS) {
            List<CardDTO> row = onTopRow ? game.board().topRow() : game.board().lowRow();
            selectionIndex = Math.min(row.size() - 1, selectionIndex + 1);
        }
        return this;
    }

    @Override
    public Screen visit(NavigateUpEvent e) {
        if (subState == SubState.MY_TURN_CARDS) {
            onTopRow = true;
            selectionIndex = Math.min(selectionIndex, game.board().topRow().size() - 1);
        }
        return this;
    }

    @Override
    public Screen visit(NavigateDownEvent e) {
        if (subState == SubState.MY_TURN_CARDS) {
            onTopRow = false;
            selectionIndex = Math.min(selectionIndex, game.board().lowRow().size() - 1);
        }
        return this;
    }

    // ── Confirm handlers ──────────────────────────────────────────────────────

    private void confirmPassScreen() {
        setupActiveState();
    }

    private void confirmTotemPlacement() {
        OfferTileDTO tile = game.board().offerTrack().get(selectionIndex);
        if (tile.totem() != null) {
            pendingError = "That tile is already occupied!";
        } else {
            sendMove(Set.of(new Move(selectionIndex, Row.OFFER)));
        }
    }

    private void confirmCardSelection() {
        int required = upperCount + lowerCount;
        if (selectedMoves.size() != required) {
            pendingError = String.format("Select exactly %d card(s): %d from top, %d from bottom.",
                    required, upperCount, lowerCount);
            return;
        }
        long selTop = selectedMoves.stream().filter(m -> m.row() == Row.UPPER).count();
        long selBot = selectedMoves.stream().filter(m -> m.row() == Row.LOWER).count();
        if (selTop != upperCount || selBot != lowerCount) {
            pendingError = String.format("Need %d from top row, %d from bottom row.", upperCount, lowerCount);
            return;
        }
        sendMove(Collections.unmodifiableSet(new LinkedHashSet<>(selectedMoves)));
    }

    private void toggleCardSelection(Row row, List<CardDTO> cards) {
        if (selectionIndex >= cards.size() || cards.get(selectionIndex) == null) {
            pendingError = "That slot is empty.";
            return;
        }
        Move move = new Move(selectionIndex, row);
        if (selectedMoves.contains(move)) {
            selectedMoves.remove(move);
        } else {
            int limit = (row == Row.UPPER) ? upperCount : lowerCount;
            long already = selectedMoves.stream().filter(m -> m.row() == row).count();
            if (already >= limit) {
                pendingError = "You can only pick " + limit + " card(s) from this row.";
            } else {
                selectedMoves.add(move);
            }
        }
    }

    private void sendMove(Set<Move> moves) {
        try {
            coordinator.makeMoveRequest(moves);
            subState = SubState.WAITING_SERVER;
        } catch (Exception e) {
            pendingError = "Move failed: " + e.getMessage();
        }
    }

    // ── State setup ───────────────────────────────────────────────────────────

    private void setupActiveState() {
        Totem current = game.currentPlayerTotem();
        if (myTotem != null && myTotem.equals(current)) {
            Phase phase = game.phase();
            if (phase == Phase.TOTEM_PLACEMENT) {
                selectionIndex = 0;
                subState = SubState.MY_TURN_TOTEM;
            } else if (phase == Phase.ACTION_EXECUTION || phase == Phase.EXTRA_MOVE) {
                selectedMoves.clear();
                selectionIndex = 0;
                onTopRow = true;
                resolveMoveCounts();
                subState = SubState.MY_TURN_CARDS;
            } else {
                subState = SubState.NOT_MY_TURN;
            }
        } else {
            subState = SubState.NOT_MY_TURN;
        }
    }

    private void resolveMoveCounts() {
        Totem current = game.currentPlayerTotem();
        for (OfferTileDTO tile : game.board().offerTrack()) {
            if (tile.totem() == current) {
                upperCount = OfferTileCatalog.upperMoves(tile.id());
                lowerCount = OfferTileCatalog.lowerMoves(tile.id());
                return;
            }
        }
        upperCount = 1;
        lowerCount = 0;
    }

    private Totem findMyTotem() {
        if (game == null || username == null) return null;
        for (PlayerDTO p : game.players()) {
            if (username.equals(p.name())) return p.totem();
        }
        return null;
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    private void renderPassScreen(TextGraphics tg, TerminalSize sz) {
        Totem totem = game.currentPlayerTotem();
        String name = nameFor(totem);
        int midRow = sz.getRows() / 2;
        int cols = sz.getColumns();

        tg.setForegroundColor(totemColor(totem));
        tg.setBackgroundColor(TextColor.ANSI.BLACK);
        putCentered(tg, midRow - 3, cols, "────────────────────────────────────");
        putCentered(tg, midRow - 2, cols, "  Pass the keyboard to:  ");
        putCentered(tg, midRow - 1, cols, "");
        putCentered(tg, midRow,     cols, "  " + name.toUpperCase() + "  (" + CardCatalog.totemLabel(totem) + " TRIBE)  ");
        putCentered(tg, midRow + 1, cols, "");
        tg.setForegroundColor(TextColor.ANSI.WHITE);
        putCentered(tg, midRow + 2, cols, "  Press ENTER to continue...  ");
        putCentered(tg, midRow + 3, cols, "────────────────────────────────────");
    }

    private void renderBoard(TextGraphics tg, TerminalSize sz,
                             int cursorOfferIndex, Set<Move> highlights) {
        int cols = sz.getColumns();
        tg.setForegroundColor(TextColor.ANSI.WHITE);
        tg.setBackgroundColor(TextColor.ANSI.BLACK);

        drawHeader(tg, cols);

        drawSectionLabel(tg, 2, cols, "OFFER TRACK");
        drawOfferTrack(tg, game.board().offerTrack(), 3, cursorOfferIndex, cols);

        int topRowY = 8;
        drawSectionLabel(tg, topRowY, cols, "TOP ROW");
        drawCardRow(tg, game.board().topRow(), topRowY + 1, highlights, Row.UPPER, cols);

        int botRowY = 14;
        drawSectionLabel(tg, botRowY, cols, "BOTTOM ROW");
        drawCardRow(tg, game.board().lowRow(), botRowY + 1, highlights, Row.LOWER, cols);

        if (subState == SubState.MY_TURN_CARDS) {
            int cursorRow = onTopRow ? topRowY + 1 : botRowY + 1;
            highlightCursor(tg, selectionIndex, cursorRow);
        }

        drawBuildingDecks(tg, game.board().remainingBuildings(), topRowY + 1, cols);
        drawCurrentPlayerTribe(tg, 20, cols);
        drawOtherPlayers(tg, 24, cols);
        drawTurnOrder(tg, 29, cols);

        String hint = switch (subState) {
            case MY_TURN_TOTEM  -> "← → Navigate offer tiles   ENTER Place totem";
            case MY_TURN_CARDS  -> String.format(
                    "← → Navigate   ↑ ↓ Switch rows   SPACE Select (%d/%d)   ENTER Confirm",
                    selectedMoves.size(), upperCount + lowerCount);
            case WAITING_SERVER -> "Waiting for server...";
            case NOT_MY_TURN    -> "Waiting for " + nameFor(game.currentPlayerTotem()) + "...";
            default             -> "";
        };
        drawControls(tg, sz, hint);
    }

    // ── Draw helpers ──────────────────────────────────────────────────────────

    private void drawHeader(TextGraphics tg, int cols) {
        tg.setForegroundColor(TextColor.ANSI.BLACK);
        tg.setBackgroundColor(TextColor.ANSI.YELLOW);
        String header = String.format("  MESOS  │  Round %d/10  │  Era %d  │  Phase: %-18s │  Current: %s  ",
                game.round(), game.era(), phaseLabel(game.phase()), currentPlayerLabel());
        if (header.length() < cols) header += " ".repeat(cols - header.length());
        tg.putString(0, 0, header.substring(0, Math.min(header.length(), cols)));
        tg.setForegroundColor(TextColor.ANSI.WHITE);
        tg.setBackgroundColor(TextColor.ANSI.BLACK);
    }

    private void drawSectionLabel(TextGraphics tg, int row, int cols, String label) {
        tg.setForegroundColor(TextColor.ANSI.CYAN);
        String line = " ── " + label + " " + "─".repeat(Math.max(0, cols - label.length() - 6));
        tg.putString(0, row, line.substring(0, Math.min(line.length(), cols)));
        tg.setForegroundColor(TextColor.ANSI.WHITE);
    }

    private void drawOfferTrack(TextGraphics tg, List<OfferTileDTO> tiles,
                                int startRow, int cursorIndex, int cols) {
        int col = 2;
        for (int i = 0; i < tiles.size(); i++) {
            OfferTileDTO tile = tiles.get(i);
            boolean selected = (i == cursorIndex);
            tg.setForegroundColor(selected ? TextColor.ANSI.YELLOW : TextColor.ANSI.WHITE);
            String idLabel = tile.id() != null ? tile.id().replace("offer_tile_", "") : "?";
            tg.putString(col, startRow,     "┌──────────┐");
            tg.putString(col, startRow + 1, "│ [" + padRight(idLabel.toUpperCase(), 7) + "] │");
            String playerLabel = (tile.totem() != null)
                    ? "(" + CardCatalog.totemLabel(tile.totem()) + ")" : "(empty)";
            tg.putString(col, startRow + 2, "│ " + padRight(playerLabel, 9) + " │");
            tg.putString(col, startRow + 3, "└──────────┘");
            tg.setForegroundColor(TextColor.ANSI.WHITE);
            col += 14;
            if (col + 14 > cols) break;
        }
    }

    private void drawCardRow(TextGraphics tg, List<CardDTO> cards, int startRow,
                             Set<Move> highlights, Row rowType, int cols) {
        int col = 2;
        for (int i = 0; i < cards.size(); i++) {
            CardDTO card = cards.get(i);
            if (card == null) {
                tg.setForegroundColor(TextColor.ANSI.BLACK);
                tg.putString(col, startRow,     "┌────────┐");
                tg.putString(col, startRow + 1, "│        │");
                tg.putString(col, startRow + 2, "│  empty │");
                tg.putString(col, startRow + 3, "└────────┘");
                tg.setForegroundColor(TextColor.ANSI.WHITE);
            } else {
                boolean highlighted = highlights.contains(new Move(i, rowType));
                tg.setForegroundColor(highlighted ? TextColor.ANSI.GREEN : TextColor.ANSI.WHITE);
                CardType type = CardCatalog.typeFromId(card.id());
                String typeStr = padRight(CardCatalog.typeLabel(type), 8);
                String idStr   = padRight(card.id().length() > 8 ? card.id().substring(0, 8) : card.id(), 8);
                tg.putString(col, startRow,     "┌────────┐");
                if (CardCatalog.isEvent(type)) tg.setForegroundColor(TextColor.ANSI.MAGENTA);
                tg.putString(col, startRow + 1, "│" + typeStr + "│");
                tg.putString(col, startRow + 2, "│" + idStr   + "│");
                tg.setForegroundColor(highlighted ? TextColor.ANSI.GREEN : TextColor.ANSI.WHITE);
                tg.putString(col, startRow + 3, "└────────┘");
                tg.setForegroundColor(TextColor.ANSI.WHITE);
            }
            col += 11;
            if (col + 11 > cols - 20) break;
        }
    }

    private void highlightCursor(TextGraphics tg, int index, int rowY) {
        int col = 2 + index * 11;
        tg.setForegroundColor(TextColor.ANSI.YELLOW);
        tg.putString(col, rowY - 1, "▼");
        tg.setForegroundColor(TextColor.ANSI.WHITE);
    }

    private void drawBuildingDecks(TextGraphics tg, List<Boolean> decks, int startRow, int cols) {
        int col = cols - 18;
        tg.setForegroundColor(TextColor.ANSI.YELLOW);
        tg.putString(col, startRow, "BUILDINGS:");
        String[] eras = {"Era I", "Era II", "Era III"};
        for (int i = 0; i < Math.min(decks.size(), 3); i++) {
            tg.putString(col, startRow + 1 + i, eras[i] + (decks.get(i) ? " [▣]" : " [ ]"));
        }
        tg.setForegroundColor(TextColor.ANSI.WHITE);
    }

    private void drawCurrentPlayerTribe(TextGraphics tg, int startRow, int cols) {
        Totem currentTotem = game.currentPlayerTotem();
        PlayerDTO me = game.players().stream()
                .filter(p -> p.totem() == currentTotem)
                .findFirst()
                .orElse(game.players().isEmpty() ? null : game.players().iterator().next());
        if (me == null) return;

        tg.setForegroundColor(totemColor(me.totem()));
        String header = String.format(" ── YOUR TRIBE: %s [%s]  Food: %d  PP: %d %s",
                me.name(), CardCatalog.totemLabel(me.totem()), me.food(), me.pp(),
                "─".repeat(Math.max(0, cols - 60)));
        tg.putString(0, startRow, header.substring(0, Math.min(header.length(), cols)));
        tg.setForegroundColor(TextColor.ANSI.WHITE);

        StringBuilder sb = new StringBuilder("  ");
        for (CardType type : CardType.values()) {
            Set<CardDTO> cardSet = me.cards().get(type);
            if (cardSet != null && !cardSet.isEmpty()) {
                sb.append(CardCatalog.typeSymbol(type)).append(":").append(cardSet.size()).append("  ");
            }
        }
        tg.putString(0, startRow + 1, sb.toString());
    }

    private void drawOtherPlayers(TextGraphics tg, int startRow, int cols) {
        tg.setForegroundColor(TextColor.ANSI.CYAN);
        tg.putString(0, startRow - 1, " ── OTHERS " + "─".repeat(Math.max(0, cols - 10)));
        tg.setForegroundColor(TextColor.ANSI.WHITE);

        Totem currentTotem = game.currentPlayerTotem();
        int row = startRow;
        for (PlayerDTO p : game.players()) {
            if (p.totem() == currentTotem) continue;
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("  %-12s [%-6s] F:%-3d PP:%-4d | ",
                    p.name(), CardCatalog.totemLabel(p.totem()), p.food(), p.pp()));
            for (CardType type : CardType.values()) {
                Set<CardDTO> cards = p.cards().get(type);
                if (cards != null && !cards.isEmpty()) {
                    sb.append(CardCatalog.typeSymbol(type)).append(":").append(cards.size()).append(" ");
                }
            }
            tg.setForegroundColor(totemColor(p.totem()));
            tg.putString(0, row, sb.toString().substring(0, Math.min(sb.length(), cols)));
            tg.setForegroundColor(TextColor.ANSI.WHITE);
            row++;
        }
    }

    private void drawTurnOrder(TextGraphics tg, int startRow, int cols) {
        tg.setForegroundColor(TextColor.ANSI.CYAN);
        StringBuilder sb = new StringBuilder(" ── Turn Order: ");
        List<Totem> order = (game.board().orderTile() != null)
                ? game.board().orderTile().totems() : Collections.emptyList();
        for (int i = 0; i < order.size(); i++) {
            sb.append(CardCatalog.totemLabel(order.get(i)));
            if (i < order.size() - 1) sb.append(" → ");
        }
        tg.putString(0, startRow, sb.toString().substring(0, Math.min(sb.length(), cols)));
        tg.setForegroundColor(TextColor.ANSI.WHITE);
    }

    private void drawControls(TextGraphics tg, TerminalSize sz, String hint) {
        int row = sz.getRows() - 1;
        tg.setForegroundColor(TextColor.ANSI.BLACK);
        tg.setBackgroundColor(TextColor.ANSI.WHITE);
        String line = "  " + hint;
        tg.putString(0, row, line + " ".repeat(Math.max(0, sz.getColumns() - line.length())));
        tg.setForegroundColor(TextColor.ANSI.WHITE);
        tg.setBackgroundColor(TextColor.ANSI.BLACK);
    }

    private void flashError(TextGraphics tg, TerminalSize sz, String msg) {
        tg.setForegroundColor(TextColor.ANSI.RED);
        tg.putString(2, sz.getRows() - 2,
                "! " + msg + " ".repeat(Math.max(0, sz.getColumns() - msg.length() - 4)));
        tg.setForegroundColor(TextColor.ANSI.WHITE);
    }

    // ── Display helpers ───────────────────────────────────────────────────────

    private String nameFor(Totem totem) {
        if (game == null || totem == null) return "?";
        return game.players().stream()
                .filter(p -> p.totem() == totem)
                .map(PlayerDTO::name)
                .findFirst()
                .orElse("?");
    }

    private String currentPlayerLabel() {
        Totem t = game.currentPlayerTotem();
        if (t == null) return "---";
        return nameFor(t) + " [" + CardCatalog.totemLabel(t) + "]";
    }

    private String phaseLabel(Phase phase) {
        if (phase == null) return "---";
        return switch (phase) {
            case TOTEM_PLACEMENT  -> "PLACE TOTEM";
            case ACTION_EXECUTION -> "PICK CARDS";
            case EXTRA_MOVE       -> "EXTRA PICK";
            case EVENTS_EXECUTION -> "EVENTS";
            case END_ROUND        -> "END OF ROUND";
            case END_GAME         -> "GAME OVER";
        };
    }

    private TextColor totemColor(Totem totem) {
        if (totem == null) return TextColor.ANSI.WHITE;
        return switch (totem) {
            case RED    -> TextColor.ANSI.RED;
            case BLUE   -> TextColor.ANSI.CYAN;
            case WHITE  -> TextColor.ANSI.WHITE;
            case BLACK  -> TextColor.ANSI.WHITE;
            case YELLOW -> TextColor.ANSI.YELLOW;
        };
    }

    private String padRight(String s, int len) {
        if (s == null) s = "";
        if (s.length() >= len) return s.substring(0, len);
        return s + " ".repeat(len - s.length());
    }

    private void putCentered(TextGraphics tg, int row, int cols, String text) {
        int col = Math.max(0, (cols - text.length()) / 2);
        tg.putString(col, row, text);
    }
}
