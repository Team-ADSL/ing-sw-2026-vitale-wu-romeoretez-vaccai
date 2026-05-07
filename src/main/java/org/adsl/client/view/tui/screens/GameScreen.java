package org.adsl.client.view.tui.screens;

import org.adsl.client.AppCoordinator;
import org.adsl.client.view.tui.CardCatalog;
import org.adsl.client.view.tui.CardTokens;
import org.adsl.client.view.events.*;
import org.adsl.client.view.tui.events.*;
import org.adsl.client.view.tui.events.CharInputEvent;
import org.adsl.client.view.tui.render.TuiColor;
import org.adsl.client.view.tui.render.TuiSize;
import org.adsl.client.view.tui.render.TuiTerminal;
import org.adsl.client.view.tui.render.TuiTextGraphics;
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
public class GameScreen extends TUIScreen {

    private enum SubState {
        MY_TURN_TOTEM,
        MY_TURN_CARDS,
        NOT_MY_TURN,
        WAITING_SERVER
    }

    private GameDTO game;
    private Totem myTotem;
    private SubState subState;

    // Card selection state
    private int selectionIndex = 0;
    private boolean onTopRow = true;
    private final Set<Move> selectedMoves = new LinkedHashSet<>();
    private int upperCount = 0;
    private int lowerCount = 0;

    // Legend overlay
    private boolean showLegend = false;
    private static final int CARD_W = 12;  // inner content width (box = CARD_W+2)

    public GameScreen(TuiTerminal terminal,
            AppCoordinator coordinator,
            String username,
            GameDTO initialGame) {
        super(terminal, coordinator, username);
        this.game = initialGame;
    }

    @Override
    public TUIScreen onEnter() {
        myTotem = findMyTotem();
        setupActiveState();
        return null;
    }

    @Override
    public void render() throws IOException {
        terminal.clear();
        TuiTextGraphics tg = terminal.newTextGraphics();
        TuiSize sz = terminal.getTerminalSize();

        switch (subState) {
            case MY_TURN_TOTEM -> renderBoard(tg, sz, selectionIndex, Collections.emptySet());
            case MY_TURN_CARDS -> renderBoard(tg, sz, -1, selectedMoves);
            case NOT_MY_TURN,
                    WAITING_SERVER ->
                renderBoard(tg, sz, -1, Collections.emptySet());
        }

        if (showLegend) {
            drawLegend(tg, sz);
        }

        if (error != null) {
            flashError(tg, sz, error);
            error = null;
        }

        terminal.refresh();
    }

    // ── Server event visitors ─────────────────────────────────────────────────

    @Override
    public TUIScreen visit(GameUpdateEvent e) {
        this.game = e.getGame();
        this.myTotem = findMyTotem();

        if (game.phase() == Phase.END_GAME) {
            subState = SubState.NOT_MY_TURN;
            return this;
        }

        setupActiveState();
        return this;
    }

    @Override
    public TUIScreen visit(ErrorEvent e) {
        super.visit(e);
        if (subState == SubState.WAITING_SERVER) {
            setupActiveState();
        }
        return this;
    }

    // ── Input event visitors ──────────────────────────────────────────────────

    @Override
    public TUIScreen visit(ConfirmEvent e) {
        switch (subState) {
            case MY_TURN_TOTEM -> confirmTotemPlacement();
            case MY_TURN_CARDS -> confirmCardSelection();
            default -> {
            }
        }
        return this;
    }

    @Override
    public TUIScreen visit(SelectEvent e) {
        if (subState == SubState.MY_TURN_CARDS) {
            Row row = onTopRow ? Row.UPPER : Row.LOWER;
            List<CardDTO> cards = onTopRow ? game.board().topRow() : game.board().lowRow();
            toggleCardSelection(row, cards);
        }
        return this;
    }

    @Override
    public TUIScreen visit(CharInputEvent e) {
        if (e.getCharacter() == 'l' || e.getCharacter() == 'L') {
            showLegend = !showLegend;
        }
        return this;
    }

    @Override
    public TUIScreen visit(NavigateLeftEvent e) {
        if (subState == SubState.MY_TURN_TOTEM) {
            selectionIndex = Math.max(0, selectionIndex - 1);
        } else if (subState == SubState.MY_TURN_CARDS) {
            selectionIndex = Math.max(0, selectionIndex - 1);
        }
        return this;
    }

    @Override
    public TUIScreen visit(NavigateRightEvent e) {
        if (subState == SubState.MY_TURN_TOTEM) {
            selectionIndex = Math.min(game.board().offerTrack().size() - 1, selectionIndex + 1);
        } else if (subState == SubState.MY_TURN_CARDS) {
            List<CardDTO> row = onTopRow ? game.board().topRow() : game.board().lowRow();
            selectionIndex = Math.min(row.size() - 1, selectionIndex + 1);
        }
        return this;
    }

    @Override
    public TUIScreen visit(NavigateUpEvent e) {
        if (subState == SubState.MY_TURN_CARDS) {
            onTopRow = true;
            selectionIndex = Math.min(selectionIndex, game.board().topRow().size() - 1);
        }
        return this;
    }

    @Override
    public TUIScreen visit(NavigateDownEvent e) {
        if (subState == SubState.MY_TURN_CARDS) {
            onTopRow = false;
            selectionIndex = Math.min(selectionIndex, game.board().lowRow().size() - 1);
        }
        return this;
    }

    // ── Confirm handlers ──────────────────────────────────────────────────────

    private void confirmTotemPlacement() {
        OfferTileDTO tile = game.board().offerTrack().get(selectionIndex);
        if (tile.totem() != null) {
            error = "That tile is already occupied!";
        } else {
            sendMove(Set.of(new Move(selectionIndex, Row.OFFER)));
        }
    }

    private void confirmCardSelection() {
        sendMove(Collections.unmodifiableSet(new LinkedHashSet<>(selectedMoves)));
    }

    private void toggleCardSelection(Row row, List<CardDTO> cards) {
        if (selectionIndex >= cards.size() || cards.get(selectionIndex) == null) {
            error = "That slot is empty.";
            return;
        }
        Move move = new Move(selectionIndex, row);
        if (selectedMoves.contains(move)) {
            selectedMoves.remove(move);
        } else {
            selectedMoves.add(move);
        }
    }

    private void sendMove(Set<Move> moves) {
        try {
            coordinator.makeMoveRequest(moves);
            subState = SubState.WAITING_SERVER;
        } catch (Exception e) {
            error = "Move failed: " + e.getMessage();
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
                Map<Row, Integer> moves = tile.moves();
                if (moves != null) {
                    upperCount = moves.getOrDefault(Row.UPPER, 0);
                    lowerCount = moves.getOrDefault(Row.LOWER, 0);
                } else {
                    upperCount = 0;
                    lowerCount = 0;
                }
                return;
            }
        }
        upperCount = 0;
        lowerCount = 0;
    }

    private Totem findMyTotem() {
        if (game == null || username == null)
            return null;
        for (PlayerDTO p : game.players()) {
            if (username.equals(p.name()))
                return p.totem();
        }
        return null;
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    private void renderBoard(TuiTextGraphics tg, TuiSize sz,
            int cursorOfferIndex, Set<Move> highlights) {
        int cols = sz.getColumns();
        tg.setForegroundColor(TuiColor.WHITE);
        tg.setBackgroundColor(TuiColor.BLACK);

        drawHeader(tg, cols);

        int topRowY = 2;
        drawSectionLabel(tg, topRowY, cols, "TOP ROW");
        drawCardRow(tg, game.board().topRow(), topRowY + 1, highlights, Row.UPPER, cols);

        int offerTrackY = 8;
        String turnOrderText = "Turn Order: ";
        if (game.board().orderTile() != null) {
            List<Totem> order = game.board().orderTile().totems();
            if (order != null) {
                StringBuilder sb = new StringBuilder("Turn Order: ");
                for (int i = 0; i < order.size(); i++) {
                    sb.append(CardCatalog.totemLabel(order.get(i)));
                    if (i < order.size() - 1)
                        sb.append(" → ");
                }
                turnOrderText = sb.toString();
            }
        }
        drawSectionLabel(tg, offerTrackY, cols, "OFFER TRACK", turnOrderText);
        drawOfferTrack(tg, game.board().offerTrack(), offerTrackY + 1, cursorOfferIndex, cols);

        int botRowY = 14;
        drawSectionLabel(tg, botRowY, cols, "BOTTOM ROW");
        drawCardRow(tg, game.board().lowRow(), botRowY + 1, highlights, Row.LOWER, cols);

        if (subState == SubState.MY_TURN_CARDS) {
            int cursorRow = onTopRow ? topRowY + 1 : botRowY + 1;
            highlightCursor(tg, selectionIndex, cursorRow);
        }

        if (subState == SubState.MY_TURN_TOTEM) {
            highlightOfferCursor(tg, selectionIndex, offerTrackY + 1);
        }

        drawCurrentPlayerTribe(tg, 20, cols);
        drawOtherPlayers(tg, 24, cols);

        String hint = switch (subState) {
            case MY_TURN_TOTEM -> "← → Navigate offer tiles   ENTER Place totem   L Legend";
            case MY_TURN_CARDS -> String.format(
                    "← → Navigate   ↑ ↓ Switch rows   SPACE Select (%d/%d)   ENTER Confirm   L Legend",
                    selectedMoves.size(), upperCount + lowerCount);
            case WAITING_SERVER -> "Waiting for server...   L Legend";
            case NOT_MY_TURN -> waitingHint() + "   L Legend";
            default -> "L Legend";
        };
        drawControls(tg, sz, hint);
    }

    // ── Draw helpers ──────────────────────────────────────────────────────────

    private void drawHeader(TuiTextGraphics tg, int cols) {
        tg.setForegroundColor(TuiColor.BLACK);
        tg.setBackgroundColor(TuiColor.YELLOW);
        String header = String.format("  MESOS  │  Round %d/10  │  Era %d  │  Phase: %-18s │  Current: %s  ",
                game.round(), game.era(), phaseLabel(game.phase()), currentPlayerLabel());
        if (header.length() < cols)
            header += " ".repeat(cols - header.length());
        tg.putString(0, 0, header.substring(0, Math.min(header.length(), cols)));
        tg.setForegroundColor(TuiColor.WHITE);
        tg.setBackgroundColor(TuiColor.BLACK);
    }

    private void drawSectionLabel(TuiTextGraphics tg, int row, int cols, String leftLabel, String rightLabel) {
        tg.setForegroundColor(TuiColor.CYAN);
        if (rightLabel == null || rightLabel.isEmpty()) {
            String line = " ── " + leftLabel + " " + "─".repeat(Math.max(0, cols - leftLabel.length() - 6));
            tg.putString(0, row, line.substring(0, Math.min(line.length(), cols)));
        } else {
            int dashCount = cols - leftLabel.length() - rightLabel.length() - 9;
            if (dashCount < 1)
                dashCount = 1;
            String line = " ── " + leftLabel + " " + "─".repeat(dashCount) + " " + rightLabel + " ──";
            tg.putString(0, row, line.substring(0, Math.min(line.length(), cols)));
        }
        tg.setForegroundColor(TuiColor.WHITE);
    }

    private void drawSectionLabel(TuiTextGraphics tg, int row, int cols, String label) {
        drawSectionLabel(tg, row, cols, label, null);
    }

    private void drawOfferTrack(TuiTextGraphics tg, List<OfferTileDTO> tiles,
            int startRow, int cursorIndex, int cols) {
        int col = 2;
        for (int i = 0; i < tiles.size(); i++) {
            OfferTileDTO tile = tiles.get(i);
            boolean selected = (i == cursorIndex);

            if (selected) {
                tg.setForegroundColor(TuiColor.YELLOW);
            } else if (tile.totem() != null) {
                tg.setForegroundColor(totemColor(tile.totem()));
            } else {
                tg.setForegroundColor(TuiColor.WHITE);
            }

            String playerName = (tile.totem() != null) ? nameFor(tile.totem()) : "";
            String movesLabel = formatMovesLabel(tile);

            tg.putString(col, startRow, "┌──────────┐");
            tg.putString(col, startRow + 1, "│ " + padRight(playerName, 8) + " │");
            tg.putString(col, startRow + 2, "│ " + padRight(movesLabel, 8) + " │");
            tg.putString(col, startRow + 3, "└──────────┘");
            tg.setForegroundColor(TuiColor.WHITE);
            col += 14;
            if (col + 14 > cols)
                break;
        }
    }

    private void drawCardRow(TuiTextGraphics tg, List<CardDTO> cards, int startRow,
            Set<Move> highlights, Row rowType, int cols) {
        // Build card data first (before writing, so colors reset cleanly)
        String border = "┌" + "─".repeat(CARD_W) + "┐";
        String borderBot = "└" + "─".repeat(CARD_W) + "┘";
        String emptyMid = "│" + " ".repeat(CARD_W) + "│";
        String emptyLabel = "│" + padRight(padLeft("empty", (CARD_W + 5) / 2), CARD_W) + "│";

        // We render row-by-row (all tops, then all type lines, etc.)
        // so that each output line is a single putString call.
        StringBuilder topLine    = new StringBuilder("  ");
        StringBuilder typeLine   = new StringBuilder("  ");
        StringBuilder effectLine = new StringBuilder("  ");
        StringBuilder botLine    = new StringBuilder("  ");

        // Card stride: box (CARD_W+2) + 1 gap = CARD_W+3 columns per card.
        // The right border of card at rendered-index n sits at 0-based column:
        //   2  (initial "  " indent)  +  n * (CARD_W+3)  +  (CARD_W+1)
        // We jump the cursor there with "\033[NG" (N = 1-based) before printing │,
        // so the right border is always at the correct column regardless of how
        // the terminal rendered the emoji content.
        int count = 0;
        for (int i = 0; i < cards.size(); i++) {
            if (2 + (count + 1) * (CARD_W + 3) > cols - 1) break;

            // 1-based ANSI column of this card's right border
            String jumpRight = "\033[" + (2 + count * (CARD_W + 3) + CARD_W + 2) + "G";

            CardDTO card = cards.get(i);
            if (card == null) {
                topLine.append(border).append(" ");
                typeLine.append("│").append(" ".repeat(CARD_W)).append(jumpRight).append("│").append(" ");
                effectLine.append("│").append(padRight(padLeft("empty", (CARD_W + 5) / 2), CARD_W)).append(jumpRight).append("│").append(" ");
                botLine.append(borderBot).append(" ");
            } else {
                boolean highlighted = highlights.contains(new Move(i, rowType));
                CardType type = CardCatalog.typeFromId(card.id());
                TuiColor cardColor = TuiColor.WHITE;
                if (type == CardType.BUILDINGS)       cardColor = TuiColor.MAGENTA;
                else if (CardCatalog.isEvent(type))   cardColor = TuiColor.ORANGE;
                TuiColor c = highlighted ? TuiColor.GREEN : cardColor;

                String tl = padRight(CardTokens.toEmoji(card.typeLabel()   != null ? card.typeLabel()   : ""), CARD_W);
                String el = padRight(CardTokens.toEmoji(card.effectsLabel() != null ? card.effectsLabel() : ""), CARD_W);

                topLine.append(c.fg()).append(border).append(TuiColor.WHITE.fg()).append(" ");
                typeLine.append(c.fg()).append("│").append(tl).append(jumpRight).append("│").append(TuiColor.WHITE.fg()).append(" ");
                effectLine.append(c.fg()).append("│").append(el).append(jumpRight).append("│").append(TuiColor.WHITE.fg()).append(" ");
                botLine.append(c.fg()).append(borderBot).append(TuiColor.WHITE.fg()).append(" ");
            }
            count++;
        }

        tg.setForegroundColor(TuiColor.DARK_GRAY);
        // We can't use putString's col param accurately for emoji; print each row as one string from col=0
        // Use raw ANSI cursor positioning then dump the whole built line
        tg.putString(0, startRow,     topLine.toString());
        tg.setForegroundColor(TuiColor.DARK_GRAY);
        tg.putString(0, startRow + 1, typeLine.toString());
        tg.setForegroundColor(TuiColor.DARK_GRAY);
        tg.putString(0, startRow + 2, effectLine.toString());
        tg.setForegroundColor(TuiColor.DARK_GRAY);
        tg.putString(0, startRow + 3, botLine.toString());
        tg.setForegroundColor(TuiColor.WHITE);
    }


    private void highlightCursor(TuiTextGraphics tg, int index, int rowY) {
        int step = CARD_W + 3;
        int col = 2 + index * step + (CARD_W + 2) / 2;
        tg.setForegroundColor(TuiColor.YELLOW);
        tg.putString(col, rowY - 1, "▼");
        tg.setForegroundColor(TuiColor.WHITE);
    }

    private void highlightOfferCursor(TuiTextGraphics tg, int index, int rowY) {
        int col = 2 + index * 14 + 5;
        tg.setForegroundColor(TuiColor.YELLOW);
        tg.putString(col, rowY - 1, "▼");
        tg.setForegroundColor(TuiColor.WHITE);
    }

    private static String formatMovesLabel(OfferTileDTO tile) {
        if (tile.givesFood())
            return "+3food";
        Map<Row, Integer> moves = tile.moves();
        if (moves == null)
            return "—";
        int up = moves.getOrDefault(Row.UPPER, 0);
        int lo = moves.getOrDefault(Row.LOWER, 0);
        if (up == 0 && lo == 0)
            return "—";
        return "▲".repeat(up) + "▼".repeat(lo);
    }

    private void drawCurrentPlayerTribe(TuiTextGraphics tg, int startRow, int cols) {
        Totem currentTotem = game.currentPlayerTotem();
        PlayerDTO me = game.players().stream()
                .filter(p -> p.totem() == myTotem)
                .findFirst()
                .orElse(game.players().isEmpty() ? null : game.players().iterator().next());
        if (me == null)
            return;

        boolean isMyTurn = (myTotem == currentTotem);
        if (isMyTurn) {
            tg.setBackgroundColor(totemColor(me.totem()));
            tg.setForegroundColor(TuiColor.BLACK);
        } else {
            tg.setForegroundColor(totemColor(me.totem()));
        }

        String turnText = isMyTurn ? " (Your Turn)" : "";
        String label = String.format("%s Tribe%s", CardCatalog.totemLabel(me.totem()), turnText);
        String header = String.format(" ── YOUR TRIBE: %s [%s]  Food: %d  PP: %d %s",
                me.name(), label, me.food(), me.pp(),
                "─".repeat(Math.max(0, cols - 65)));

        tg.putString(0, startRow, header.substring(0, Math.min(header.length(), cols)));
        tg.setForegroundColor(TuiColor.WHITE);
        tg.setBackgroundColor(TuiColor.BLACK);

        StringBuilder sb = new StringBuilder("  ");
        for (CardType type : CardType.values()) {
            Set<CardDTO> cardSet = me.cards().get(type);
            if (cardSet != null && !cardSet.isEmpty()) {
                sb.append(CardCatalog.typeSymbol(type)).append(":").append(cardSet.size()).append("  ");
            }
        }
        tg.putString(0, startRow + 1, sb.toString());
    }

    private void drawOtherPlayers(TuiTextGraphics tg, int startRow, int cols) {
        tg.setForegroundColor(TuiColor.CYAN);
        tg.putString(0, startRow - 1, " ── OTHERS " + "─".repeat(Math.max(0, cols - 10)));
        tg.setForegroundColor(TuiColor.WHITE);

        Totem currentTotem = game.currentPlayerTotem();
        int row = startRow;
        for (PlayerDTO p : game.players()) {
            if (p.totem() == myTotem)
                continue;

            boolean isTheirTurn = (p.totem() == currentTotem);
            if (isTheirTurn) {
                tg.setBackgroundColor(totemColor(p.totem()));
                tg.setForegroundColor(TuiColor.BLACK);
            } else {
                tg.setForegroundColor(totemColor(p.totem()));
            }

            String turnText = isTheirTurn ? " (Your Turn)" : "";
            String label = String.format("%s Tribe%s", CardCatalog.totemLabel(p.totem()), turnText);

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("  %-12s [%s] F:%-3d PP:%-4d | ",
                    p.name(), label, p.food(), p.pp()));
            for (CardType type : CardType.values()) {
                Set<CardDTO> cards = p.cards().get(type);
                if (cards != null && !cards.isEmpty()) {
                    sb.append(CardCatalog.typeSymbol(type)).append(":").append(cards.size()).append(" ");
                }
            }
            if (!isTheirTurn) {
                tg.setForegroundColor(totemColor(p.totem()));
            }
            tg.putString(0, row, sb.toString().substring(0, Math.min(sb.length(), cols)));
            tg.setForegroundColor(TuiColor.WHITE);
            tg.setBackgroundColor(TuiColor.BLACK);
            row++;
        }
    }

    private void drawControls(TuiTextGraphics tg, TuiSize sz, String hint) {
        int row = sz.getRows() - 1;
        tg.setForegroundColor(TuiColor.BLACK);
        tg.setBackgroundColor(TuiColor.WHITE);
        String line = "  " + hint;
        tg.putString(0, row, line + " ".repeat(Math.max(0, sz.getColumns() - line.length())));
        tg.setForegroundColor(TuiColor.WHITE);
        tg.setBackgroundColor(TuiColor.BLACK);
    }

    private void flashError(TuiTextGraphics tg, TuiSize sz, String msg) {
        tg.setForegroundColor(TuiColor.RED);
        tg.putString(2, sz.getRows() - 2,
                "! " + msg + " ".repeat(Math.max(0, sz.getColumns() - msg.length() - 4)));
        tg.setForegroundColor(TuiColor.WHITE);
    }

    // ── Display helpers ───────────────────────────────────────────────────────

    private String nameFor(Totem totem) {
        if (game == null || totem == null)
            return "?";
        return game.players().stream()
                .filter(p -> p.totem() == totem)
                .map(PlayerDTO::name)
                .findFirst()
                .orElse("?");
    }

    /**
     * NOT_MY_TURN hint. Phases without a current player (END_ROUND, EVENTS,
     * END_GAME) used to render as "Waiting for ?..." which looked broken;
     * fall back to a phase-specific message instead.
     */
    private String waitingHint() {
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

    private String currentPlayerLabel() {
        Totem t = game.currentPlayerTotem();
        if (t == null)
            return "---";
        return nameFor(t) + " [" + CardCatalog.totemLabel(t) + "]";
    }

    private String phaseLabel(Phase phase) {
        if (phase == null)
            return "---";
        return switch (phase) {
            case TOTEM_PLACEMENT -> "PLACE TOTEM";
            case ACTION_EXECUTION -> "PICK CARDS";
            case EXTRA_MOVE -> "EXTRA PICK";
            case EVENTS_EXECUTION -> "EVENTS";
            case END_ROUND -> "END OF ROUND";
            case END_GAME -> "GAME OVER";
        };
    }

    private TuiColor totemColor(Totem totem) {
        if (totem == null)
            return TuiColor.WHITE;
        return switch (totem) {
            case RED -> TuiColor.RED;
            case BLUE -> TuiColor.CYAN;
            case WHITE -> TuiColor.WHITE;
            case BLACK -> TuiColor.WHITE;
            case YELLOW -> TuiColor.YELLOW;
        };
    }

    /**
     * Visual width of a string in terminal columns (Windows Terminal rules).
     *   supplementary emoji (surrogate pair) + U+FE0F  → 3 cols
     *   supplementary emoji (surrogate pair) no FE0F   → 2 cols
     *   U+FE0F, U+200D, U+200B..U+200F (zero-width)   → 0 cols
     *   U+2500..U+257F (box-drawing)                   → 1 col
     *   U+23E9..U+23FA (BMP emoji presentation, e.g. ⏩) → 2 cols
     *   everything else (ASCII + regular BMP)          → 1 col
     */
    private static int visualWidth(String s) {
        if (s == null) return 0;
        int width = 0;
        int i = 0;
        while (i < s.length()) {
            int ch = s.charAt(i);
            if (ch >= 0xD800 && ch <= 0xDBFF && i + 1 < s.length()) {
                i += 2;
                if (i < s.length() && s.charAt(i) == 0xFE0F) {
                    width += 3; i++;
                } else {
                    width += 2;
                }
            } else if (ch == 0xFE0F || ch == 0x200D || (ch >= 0x200B && ch <= 0x200F)) {
                i++;
            } else if (ch >= 0x2500 && ch <= 0x257F) {
                width += 1; i++;
            } else if (ch >= 0x23E9 && ch <= 0x23FA) {
                width += 1; i++;
            } else {
                width += 1; i++;
            }
        }
        return width;
    }

    private String padRight(String s, int len) {
        if (s == null) s = "";
        int vw = visualWidth(s);
        if (vw == len) return s;
        if (vw > len) {
            StringBuilder sb = new StringBuilder();
            int w = 0;
            int i = 0;
            while (i < s.length()) {
                int ch = s.charAt(i);
                int cw, advance;
                if (ch >= 0xD800 && ch <= 0xDBFF && i + 1 < s.length()) {
                    if (i + 2 < s.length() && s.charAt(i + 2) == 0xFE0F) {
                        cw = 3; advance = 3;
                    } else {
                        cw = 2; advance = 2;
                    }
                } else if (ch == 0xFE0F || ch == 0x200D || (ch >= 0x200B && ch <= 0x200F)) {
                    cw = 0; advance = 1;
                } else if (ch >= 0x2500 && ch <= 0x257F) {
                    cw = 1; advance = 1;
                } else if (ch >= 0x23E9 && ch <= 0x23FA) {
                    cw = 3; advance = 1;
                } else {
                    cw = 1; advance = 1;
                }
                if (w + cw > len) break;
                sb.append(s, i, i + advance);
                w += cw;
                i += advance;
            }
            if (w < len) sb.append(" ".repeat(len - w));
            return sb.toString();
        }
        return s + " ".repeat(len - vw);
    }

    private String padLeft(String s, int len) {
        if (s == null) s = "";
        int vw = visualWidth(s);
        if (vw >= len) return s;
        return " ".repeat(len - vw) + s;
    }

    private void drawLegend(TuiTextGraphics tg, TuiSize sz) {
        int w = 38;
        int h = 26;
        int x = sz.getColumns() - w - 2;
        int y = 1;

        tg.setBackgroundColor(TuiColor.BLACK);
        tg.setForegroundColor(TuiColor.CYAN);
        String top = "┌" + "─".repeat(w - 2) + "┐";
        String bot = "└" + "─".repeat(w - 2) + "┘";
        tg.putString(x, y, top);
        tg.putString(x, y + h - 1, bot);
        for (int r = 1; r < h - 1; r++)
            tg.putString(x, y + r, "│" + " ".repeat(w - 2) + "│");

        tg.setForegroundColor(TuiColor.YELLOW);
        tg.putString(x + 2, y, " LEGEND (L to close) ");

        String[][] entries = {
            {"─── CHARACTERS ────────────────────"},
            {"🏹", "Hunter – draws on pick"},
            {"🧺", "Gatherer – building discount"},
            {"🔨", "Builder – build discount+PP"},
            {"🔮", "Shaman – ritual bonus"},
            {"🎨", "Artist – paintings bonus"},
            {"💡", "Inventor – activates icon"},
            {"─── EVENTS ────────────────────────"},
            {"🐗", "Hunt – PP per hunter"},
            {"🍲", "Sustenance – PP penalty"},
            {"🎭", "Shamanic Ritual – PP trade"},
            {"🖌️", "Cave Paintings – artist bonus"},
            {"─── BUILDINGS ─────────────────────"},
            {"🏛️", "All buildings"},
            {"🏁", "End-game PP bonus"},
            {"─── SYMBOLS ───────────────────────"},
            {"🌟", "Prestige Points (PP)"},
            {"★", "Shaman ritual stars"},
            {"💰", "Food cost"},
            {"🍖", "Extra food on pick"},
            {"🍞", "Food reward"},
            {"🗿", "Totem symbol"},
            {"I II III", "Card Era"},
        };

        tg.setForegroundColor(TuiColor.WHITE);
        int lineY = y + 2;
        for (String[] entry : entries) {
            if (entry.length == 1) {
                tg.setForegroundColor(TuiColor.CYAN);
                tg.putString(x + 2, lineY, padRight(entry[0], w - 4));
                tg.setForegroundColor(TuiColor.WHITE);
            } else {
                int tokenW = visualWidth(entry[0]);
                boolean hasFE0F = entry[0].indexOf('️') >= 0;
                String sep = hasFE0F ? "" : " ";
                int descW = hasFE0F ? (w - 4 - tokenW) : (w - 5 - tokenW);
                tg.putString(x + 2, lineY, entry[0] + sep + padRight(entry[1], descW));
            }
            lineY++;
            if (lineY >= y + h - 1) break;
        }
        tg.setBackgroundColor(TuiColor.BLACK);
    }
}
