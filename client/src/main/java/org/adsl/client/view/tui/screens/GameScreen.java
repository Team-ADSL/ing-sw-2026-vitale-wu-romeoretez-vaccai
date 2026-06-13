package org.adsl.client.view.tui.screens;

import org.adsl.client.AppCoordinator;
import org.adsl.client.serverEvents.EndGameEvent;
import org.adsl.client.serverEvents.ErrorEvent;
import org.adsl.client.serverEvents.EventsTriggeredEvent;
import org.adsl.client.serverEvents.GameUpdateEvent;
import org.adsl.client.view.tui.CardCatalog;
import org.adsl.client.view.tui.CardTokens;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    // Substate active right before a move was sent. While WAITING_SERVER the
    // board is rendered as if still in this state so the cursor stays put
    // instead of flashing to the top-left view cursor during the round-trip.
    private SubState preWaitState = SubState.NOT_MY_TURN;

    // Card selection state
    private int selectionIndex = 0;
    private boolean onTopRow = true;
    private final Set<Move> selectedMoves = new LinkedHashSet<>();
    private int upperCount = 0;
    private int lowerCount = 0;

    // View-row cursor: which card row A/D scrolls when not in MY_TURN_CARDS
    private boolean viewOnTopRow = true;

    // Independent scroll offsets per row (A/D keys)
    private int topRowOffset = 0;
    private int lowRowOffset = 0;

    // Legend overlay
    private boolean showLegend = false;
    private boolean showSummaryCard = false;

    // Deck inspector overlay (C key): shows one player's full deck as card boxes
    // in the bottom area. ↑/↓ switch player, A/D scroll, Q/E jump to ends.
    private boolean showDecks = false;
    private int deckPlayerIndex = 0;
    private int deckOffset = 0;

    private static final int CARD_W = 15;  // inner content width (box = CARD_W+2)

    // Events overlay — one title at a time, paced by AppCoordinator. The
    // overlay stays visible until the next response (GameUpdate / next
    // EventsTriggered) replaces or clears it.
    private volatile String overlayTitle = null;
    private volatile String overlayLog = null;

    // Rules overlay
    private boolean showRules = false;
    private int rulesPageIndex = 0;
    private int rulesScrollOffset = 0;
    private List<String> rulesPages = null;

    /**
     * @param terminal    the TUI terminal used for rendering
     * @param coordinator the coordinator used to send move requests to the server
     * @param username    the current player's username
     * @param initialGame the game state to render when this screen is first shown
     */
    public GameScreen(TuiTerminal terminal,
            AppCoordinator coordinator,
            String username,
            GameDTO initialGame) {
        super(terminal, coordinator, username);
        this.game = initialGame;
    }

    /**
     * Resolves the player's totem and computes the initial turn sub-state
     * (whose turn it is and what input, if any, is expected).
     *
     * @return {@code null} always, staying on this screen
     */
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

        switch (visualState()) {
            case MY_TURN_TOTEM -> renderBoard(tg, sz, selectionIndex, Collections.emptySet());
            case MY_TURN_CARDS -> renderBoard(tg, sz, -1, selectedMoves);
            case NOT_MY_TURN,
                    WAITING_SERVER ->
                renderBoard(tg, sz, -1, Collections.emptySet());
        }

        if (showLegend) {
            drawLegend(tg, sz);
        }

        if (showRules) {
            drawRulesWindow(tg, sz);
        } else if (showSummaryCard) {
            drawSummaryCardWindow(tg, sz);
        }

        if (showLog) {
            drawLogWindow(tg, sz);
        } else {
            drawLogPreview(tg, sz);
        }

        if (overlayTitle != null) {
            drawEventsOverlay(tg, sz);
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
        // A new game state means the events overlay (if any) is over.
        this.overlayTitle = null;
        this.overlayLog = null;
        this.game = e.game();
        this.myTotem = findMyTotem();

        if (game.phase() == Phase.END_GAME) {
            subState = SubState.NOT_MY_TURN;
            return this;
        }

        setupActiveState();
        return this;
    }

    @Override
    public TUIScreen visit(EventsTriggeredEvent e) {
        String title = e.eventTitle();
        if (title == null || title.isBlank()) return this;
        this.overlayTitle = title;
        String log = e.logMessage();
        this.overlayLog = log;
        if (log != null && !log.isBlank()) {
            org.adsl.client.view.tui.TuiGameLog.INSTANCE.append(log);
        }
        setToRender(true);
        return this;
    }

    @Override
    public TUIScreen visit(ErrorEvent e) {
        super.visit(e);
        if (subState == SubState.WAITING_SERVER) {
            Totem current = game.currentPlayerTotem();
            if (myTotem != null && myTotem.equals(current)) {
                Phase phase = game.phase();
                if (phase == Phase.TOTEM_PLACEMENT) {
                    subState = SubState.MY_TURN_TOTEM;
                } else if (phase == Phase.ACTION_EXECUTION || phase == Phase.EXTRA_MOVE) {
                    subState = SubState.MY_TURN_CARDS;
                } else {
                    subState = SubState.NOT_MY_TURN;
                }
            } else {
                subState = SubState.NOT_MY_TURN;
            }
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
        char c = e.getCharacter();
        if (c == 'r' || c == 'R') {
            toggleRules();
        } else if (c == 's' || c == 'S') {
            toggleSummaryCard();
        } else if (c == 'l' || c == 'L') {
            showLegend = !showLegend;
        } else if (c == 'm' || c == 'M') {
            toggleLog();
        } else if (c == 'c' || c == 'C') {
            toggleDecks();
        } else if (c == 'a' || c == 'A') {
            if (showDecks) deckScrollLeft(); else scrollCurrentRowLeft();
        } else if (c == 'd' || c == 'D') {
            if (showDecks) deckScrollRight(); else scrollCurrentRowRight();
        } else if (c == 'q' || c == 'Q') {
            if (showDecks) deckJumpStart(); else jumpRowStart();
        } else if (c == 'e' || c == 'E') {
            if (showDecks) deckJumpEnd(); else jumpRowEnd();
        }
        return this;
    }

    private void jumpRowStart() {
        if (subState == SubState.MY_TURN_CARDS) {
            if (onTopRow) topRowOffset = 0;
            else lowRowOffset = 0;
            selectionIndex = 0;
        } else {
            if (viewOnTopRow) topRowOffset = 0;
            else lowRowOffset = 0;
        }
    }

    private void jumpRowEnd() {
        int visible = visibleCardsCount();
        if (subState == SubState.MY_TURN_CARDS) {
            List<CardDTO> row = onTopRow ? game.board().topRow() : game.board().lowRow();
            int content = contentLength(row);
            int max = Math.max(0, content - visible);
            if (onTopRow) topRowOffset = max;
            else lowRowOffset = max;
            selectionIndex = Math.max(0, content - 1);
        } else {
            List<CardDTO> row = viewOnTopRow ? game.board().topRow() : game.board().lowRow();
            int max = Math.max(0, contentLength(row) - visible);
            if (viewOnTopRow) topRowOffset = max;
            else lowRowOffset = max;
        }
    }

    private void scrollCurrentRowLeft() {
        boolean top = (subState == SubState.MY_TURN_CARDS) ? onTopRow : viewOnTopRow;
        if (top) {
            topRowOffset = Math.max(0, topRowOffset - 1);
        } else {
            lowRowOffset = Math.max(0, lowRowOffset - 1);
        }
    }

    private void scrollCurrentRowRight() {
        boolean top = (subState == SubState.MY_TURN_CARDS) ? onTopRow : viewOnTopRow;
        int visible = visibleCardsCount();
        if (top) {
            int max = Math.max(0, contentLength(game.board().topRow()) - visible);
            topRowOffset = Math.min(max, topRowOffset + 1);
        } else {
            int max = Math.max(0, contentLength(game.board().lowRow()) - visible);
            lowRowOffset = Math.min(max, lowRowOffset + 1);
        }
    }

    private int visibleCardsCount() {
        int cols = terminal.getTerminalSize().getColumns();
        return Math.max(1, (cols - 3) / (CARD_W + 3));
    }

    /**
     * Number of slots up to and including the last non-null card. Scrolling and
     * jumping clamp to this so the cursor never wanders into the trailing
     * empty (picked-out / building) slots that carry no real card.
     */
    private int contentLength(List<CardDTO> row) {
        int last = -1;
        for (int i = 0; i < row.size(); i++) {
            if (row.get(i) != null) last = i;
        }
        return last + 1;
    }

    // ── Deck inspector ────────────────────────────────────────────────────────

    private void toggleDecks() {
        showDecks = !showDecks;
        if (showDecks) {
            deckPlayerIndex = 0;
            deckOffset = 0;
        }
    }

    private void deckPlayerPrev() {
        if (deckPlayerIndex > 0) {
            deckPlayerIndex--;
            deckOffset = 0;
        }
    }

    private void deckPlayerNext() {
        if (deckPlayerIndex < playerList().size() - 1) {
            deckPlayerIndex++;
            deckOffset = 0;
        }
    }

    /** Players in a stable totem order so the deck index always maps to the same player. */
    private List<PlayerDTO> playerList() {
        List<PlayerDTO> list = new ArrayList<>(game.players());
        list.sort(Comparator.comparingInt(
                p -> p.totem() == null ? Integer.MAX_VALUE : p.totem().ordinal()));
        return list;
    }

    private void deckScrollLeft() {
        deckOffset = Math.max(0, deckOffset - 1);
    }

    private void deckScrollRight() {
        int max = Math.max(0, currentDeck().size() - visibleCardsCount());
        deckOffset = Math.min(max, deckOffset + 1);
    }

    private void deckJumpStart() {
        deckOffset = 0;
    }

    private void deckJumpEnd() {
        deckOffset = Math.max(0, currentDeck().size() - visibleCardsCount());
    }

    /** Flat, type-ordered list of the currently inspected player's cards. */
    private List<CardDTO> currentDeck() {
        List<PlayerDTO> players = playerList();
        if (players.isEmpty()) return Collections.emptyList();
        int idx = Math.min(deckPlayerIndex, players.size() - 1);
        return deckCards(players.get(idx));
    }

    private List<CardDTO> deckCards(PlayerDTO p) {
        List<CardDTO> out = new ArrayList<>();
        Map<CardType, Set<CardDTO>> cards = p.cards();
        if (cards == null) return out;
        for (CardType t : CardType.values()) {
            Set<CardDTO> set = cards.get(t);
            if (set != null) out.addAll(set);
        }
        return out;
    }

    /**
     * Bottom-area panel showing one player's full collection as card boxes,
     * reusing {@link #drawCardRow}. The player is chosen with ↑/↓ and the row
     * scrolls with A/D (Q/E jump to ends), mirroring the board-row controls.
     */
    private void drawDecksPanel(TuiTextGraphics tg, TuiSize sz, int startRow) {
        int cols = sz.getColumns();
        List<PlayerDTO> players = playerList();
        if (players.isEmpty()) return;
        if (deckPlayerIndex >= players.size()) deckPlayerIndex = players.size() - 1;
        PlayerDTO p = players.get(deckPlayerIndex);

        boolean isMe = (p.totem() == myTotem);
        String who = p.name() + (isMe ? " (you)" : "")
                + " [" + CardCatalog.totemLabel(p.totem()) + "]"
                + "  " + (deckPlayerIndex + 1) + "/" + players.size();
        drawSectionLabel(tg, startRow, cols, "DECK: " + who,
                "↑↓ player · A/D scroll · Q/E ends · C close");

        List<CardDTO> deck = currentDeck();
        if (deck.isEmpty()) {
            tg.setForegroundColor(TuiColor.DARK_GRAY);
            tg.putString(2, startRow + 2, "(no cards yet)");
            tg.setForegroundColor(TuiColor.WHITE);
            return;
        }

        int visible = visibleCardsCount();
        int max = Math.max(0, deck.size() - visible);
        if (deckOffset > max) deckOffset = max;

        drawCardRow(tg, deck, startRow + 1, Collections.emptySet(), Row.UPPER, cols, deckOffset);

        // Active-row marker on the left, plus edge hints when more cards exist.
        tg.setForegroundColor(TuiColor.YELLOW);
        tg.putString(0, startRow + 2, "►");
        if (deckOffset > 0) {
            tg.putString(1, startRow + 2, "«");
        }
        if (deckOffset < max) {
            tg.putString(cols - 1, startRow + 2, "»");
        }
        tg.setForegroundColor(TuiColor.WHITE);
    }

    @Override
    public TUIScreen visit(NavigateLeftEvent e) {
        if (showLog) return this;
        // When rules overlay is open, ← goes to previous page.
        if (showRules) {
            if (rulesPageIndex > 0) {
                rulesPageIndex--;
                rulesScrollOffset = 0;
            }
            return this;
        }
        if (showDecks) {
            deckScrollLeft();
            return this;
        }
        if (subState == SubState.MY_TURN_TOTEM) {
            selectionIndex = Math.max(0, selectionIndex - 1);
        } else if (subState == SubState.MY_TURN_CARDS) {
            int prev = selectionIndex;
            selectionIndex = Math.max(0, selectionIndex - 1);
            if (selectionIndex < prev) {
                int offset = onTopRow ? topRowOffset : lowRowOffset;
                if (selectionIndex < offset) {
                    if (onTopRow) topRowOffset = Math.max(0, topRowOffset - 1);
                    else lowRowOffset = Math.max(0, lowRowOffset - 1);
                }
            }
        }
        return this;
    }

    @Override
    public TUIScreen visit(NavigateRightEvent e) {
        if (showLog) return this;
        // When rules overlay is open, → goes to next page.
        if (showRules) {
            if (rulesPages != null && rulesPageIndex < rulesPages.size() - 1) {
                rulesPageIndex++;
                rulesScrollOffset = 0;
            }
            return this;
        }
        if (showDecks) {
            deckScrollRight();
            return this;
        }
        if (subState == SubState.MY_TURN_TOTEM) {
            selectionIndex = Math.min(game.board().offerTrack().size() - 1, selectionIndex + 1);
        } else if (subState == SubState.MY_TURN_CARDS) {
            List<CardDTO> row = onTopRow ? game.board().topRow() : game.board().lowRow();
            int content = contentLength(row);
            int prev = selectionIndex;
            selectionIndex = Math.min(Math.max(0, content - 1), selectionIndex + 1);
            if (selectionIndex > prev) {
                int visible = visibleCardsCount();
                int offset = onTopRow ? topRowOffset : lowRowOffset;
                if (selectionIndex >= offset + visible) {
                    int max = Math.max(0, content - visible);
                    if (onTopRow) topRowOffset = Math.min(max, topRowOffset + 1);
                    else lowRowOffset = Math.min(max, lowRowOffset + 1);
                }
            }
        }
        return this;
    }

    @Override
    public TUIScreen visit(NavigateUpEvent e) {
        if (showLog) { logScrollUp(); return this; }
        if (showRules) { rulesScrollOffset = Math.max(0, rulesScrollOffset - 1); return this; }
        if (showDecks) {
            deckPlayerPrev();
            return this;
        }
        if (subState == SubState.MY_TURN_CARDS) {
            if (!onTopRow) {
                int screenPos = selectionIndex - lowRowOffset;
                int visible = visibleCardsCount();
                int newIndex = topRowOffset + screenPos;
                newIndex = Math.max(topRowOffset, Math.min(topRowOffset + visible - 1, newIndex));
                selectionIndex = Math.min(game.board().topRow().size() - 1, Math.max(0, newIndex));
                onTopRow = true;
            }
        } else {
            viewOnTopRow = true;
        }
        return this;
    }

    @Override
    public TUIScreen visit(NavigateDownEvent e) {
        if (showLog) { logScrollDown(); return this; }
        if (showRules) { rulesScrollOffset++; return this; }
        if (showDecks) {
            deckPlayerNext();
            return this;
        }
        if (subState == SubState.MY_TURN_CARDS) {
            if (onTopRow) {
                int screenPos = selectionIndex - topRowOffset;
                int visible = visibleCardsCount();
                int newIndex = lowRowOffset + screenPos;
                newIndex = Math.max(lowRowOffset, Math.min(lowRowOffset + visible - 1, newIndex));
                selectionIndex = Math.min(game.board().lowRow().size() - 1, Math.max(0, newIndex));
                onTopRow = false;
            }
        } else {
            viewOnTopRow = false;
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
            appCoordinator.makeMoveRequest(moves);
            preWaitState = subState;
            subState = SubState.WAITING_SERVER;
        } catch (Exception e) {
            error = "Move failed: " + e.getMessage();
        }
    }

    /**
     * State used for cursor rendering. While WAITING_SERVER we mirror the
     * pre-send state so the cursor does not visibly jump during the round-trip.
     */
    private SubState visualState() {
        return subState == SubState.WAITING_SERVER ? preWaitState : subState;
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
                topRowOffset = 0;
                lowRowOffset = 0;
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
        drawCardRow(tg, game.board().topRow(), topRowY + 1, highlights, Row.UPPER, cols, topRowOffset);

        int offerTrackY = 8;
        String turnOrderText = "Turn Order: ";
        if (game.board().orderTile() != null) {
            List<OrderCellDTO> cells = game.board().orderTile().cells();
            if (cells != null) {
                StringBuilder sb = new StringBuilder("Turn Order: ");
                for (int i = 0; i < cells.size(); i++) {
                    OrderCellDTO cell = cells.get(i);
                    if (cell.totem() != null) {
                        sb.append(CardCatalog.totemLabel(cell.totem()));
                    } else if (cell.isMalus()) {
                        sb.append("-1").append(CardTokens.toEmoji(CardToken.FOOD))
                          .append("/-2").append(CardTokens.toEmoji(CardToken.PP));
                    } else if (cell.bonus() > 0) {
                        sb.append("+").append(cell.bonus()).append(CardTokens.toEmoji(CardToken.FOOD));
                    } else {
                        sb.append("--");
                    }
                    if (i < cells.size() - 1) sb.append(" → ");
                }
                turnOrderText = sb.toString();
            }
        }
        drawSectionLabel(tg, offerTrackY, cols, "OFFER TRACK", turnOrderText);
        drawOfferTrack(tg, game.board().offerTrack(), offerTrackY + 1, cursorOfferIndex, cols);

        int botRowY = 14;
        drawSectionLabel(tg, botRowY, cols, "BOTTOM ROW");
        drawCardRow(tg, game.board().lowRow(), botRowY + 1, highlights, Row.LOWER, cols, lowRowOffset);

        SubState vs = visualState();
        if (vs == SubState.MY_TURN_CARDS) {
            int cursorRow = onTopRow ? topRowY + 1 : botRowY + 1;
            int cursorOffset = onTopRow ? topRowOffset : lowRowOffset;
            highlightCursor(tg, selectionIndex, cursorRow, cursorOffset, cols);
        }

        if (vs == SubState.MY_TURN_TOTEM) {
            highlightOfferCursor(tg, selectionIndex, offerTrackY + 1);
        }

        if (vs != SubState.MY_TURN_CARDS) {
            drawRowViewCursor(tg, topRowY, botRowY);
        }

        if (showDecks) {
            drawDecksPanel(tg, sz, 19);
        } else {
            drawCurrentPlayerTribe(tg, 20, cols);
            drawOtherPlayers(tg, 24, cols);
        }

        String hint;
        if (showRules) {
            int w = Math.min(160, Math.max(40, cols * 80 / 100));
            int h = Math.min(43, Math.max(14, sz.getRows() * 75 / 100));
            int innerH = h - 6;
            String pageText = rulesPages.get(Math.min(rulesPageIndex, rulesPages.size() - 1));
            if (wrapText(pageText, w - 4).size() > innerH) {
                hint = "R Close rules   ← Prev page   → Next page   ↑ ↓ Scroll text";
            } else {
                hint = "R Close rules   ← Prev page   → Next page";
            }
        } else if (showSummaryCard) {
            hint = "S Close SC";
        } else if (showLog) {
            hint = "↑ ↓ Scroll log   M Close log";
        } else if (showDecks) {
            hint = "↑ ↓ Player   A/D Scroll   Q/E Jump ends   C Close decks   L Legend   M Log   R Rules   S SC";
        } else {
            hint = switch (subState) {
                case MY_TURN_TOTEM -> "← → Offer tiles   ↑ ↓ Switch rows   A/D Scroll   Q/E Jump ends   ENTER Place totem   C Decks   L Legend   M Log   R Rules   S SC";
                case MY_TURN_CARDS -> String.format(
                        "← → Navigate   ↑ ↓ Switch rows   A/D Scroll   Q/E Jump ends   SPACE Select (%d/%d)   ENTER Confirm   C Decks   L Legend   M Log   R Rules   S SC",
                        selectedMoves.size(), upperCount + lowerCount);
                case WAITING_SERVER -> "Waiting for server...   ↑ ↓ Switch rows   A/D Scroll   Q/E Jump ends   C Decks   L Legend   M Log   R Rules   S SC";
                case NOT_MY_TURN -> waitingHint() + "   ↑ ↓ Switch rows   A/D Scroll   Q/E Jump ends   C Decks   L Legend   M Log   R Rules   S SC";
                default -> "C Decks   L Legend   M Log   R Rules   S SC";
            };
        }
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
            tg.putString(col, startRow + 1, "│ " + padRight(padLeft(playerName, (8 + visualWidth(playerName)) / 2), 8) + " │");
            tg.putString(col, startRow + 2, "│ " + padRight(padLeft(movesLabel, (8 + visualWidth(movesLabel)) / 2), 8) + " │");
            tg.putString(col, startRow + 3, "└──────────┘");
            tg.setForegroundColor(TuiColor.WHITE);
            col += 14;
            if (col + 14 > cols)
                break;
        }
    }

    private void drawCardRow(TuiTextGraphics tg, List<CardDTO> cards, int startRow,
            Set<Move> highlights, Row rowType, int cols, int offset) {
        // Build card data first (before writing, so colors reset cleanly)
        String border = "┌" + "─".repeat(CARD_W) + "┐";
        String borderBot = "└" + "─".repeat(CARD_W) + "┘";

        // We render row-by-row (all tops, then all type lines, etc.)
        // so that each output line is a single putString call.
        StringBuilder topLine    = new StringBuilder("  ");
        StringBuilder typeLine   = new StringBuilder("  ");
        StringBuilder effectLine = new StringBuilder("  ");
        StringBuilder botLine    = new StringBuilder("  ");

        String dim   = TuiColor.DARK_GRAY.fg();
        String reset = TuiColor.WHITE.fg();

        // Card stride: box (CARD_W+2) + 1 gap = CARD_W+3 columns per card.
        // The right border of card at rendered-index n sits at 0-based column:
        //   2  (initial "  " indent)  +  n * (CARD_W+3)  +  (CARD_W+1)
        // We jump the cursor there with "\033[NG" (N = 1-based) before printing │,
        // so the right border is always at the correct column regardless of how
        // the terminal rendered the emoji content.
        int count = 0;
        int start = Math.max(0, offset);
        for (int i = start; i < cards.size(); i++) {
            if (2 + (count + 1) * (CARD_W + 3) > cols - 1) break;

            // 1-based ANSI column of this card's right border
            String jumpRight = "\033[" + (2 + count * (CARD_W + 3) + CARD_W + 2) + "G";

            CardDTO card = cards.get(i);
            if (card == null) {
                topLine.append(dim).append(border).append(reset).append(" ");
                typeLine.append(dim).append("│").append(" ".repeat(CARD_W)).append(jumpRight).append("│").append(reset).append(" ");
                effectLine.append(dim).append("│").append(" ".repeat(CARD_W)).append(jumpRight).append("│").append(reset).append(" ");
                botLine.append(dim).append(borderBot).append(reset).append(" ");
            } else {
                boolean highlighted = highlights.contains(new Move(i, rowType));
                CardType type = CardCatalog.typeFromId(card.id());
                TuiColor cardColor = TuiColor.WHITE;
                if (type == CardType.BUILDINGS)       cardColor = TuiColor.MAGENTA;
                else if (CardCatalog.isEvent(type))   cardColor = TuiColor.ORANGE;
                TuiColor c = highlighted ? TuiColor.GREEN : cardColor;


                String rawTl = CardTokens.toEmoji(card.typeLabel() != null ? card.typeLabel() : "");
                String costStr = card.costLabel() != null ? CardTokens.toEmoji(card.costLabel()) : "";
                String combined = rawTl.isEmpty() ? costStr
                        : costStr.isEmpty() ? rawTl
                        : (visualWidth(rawTl) + 1 + visualWidth(costStr) <= CARD_W)
                                ? rawTl + " " + costStr
                                : rawTl + costStr;
                String tl = padRight(padLeft(combined, (CARD_W + visualWidth(combined)) / 2), CARD_W);
                String rawEl = CardTokens.toEffectLabel(card.effectsLabel() != null ? card.effectsLabel() : "");
                String el = padRight(padLeft(rawEl, (CARD_W + visualWidth(rawEl)) / 2), CARD_W);


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


    private void highlightCursor(TuiTextGraphics tg, int index, int rowY, int offset, int cols) {
        int visibleIndex = index - offset;
        if (visibleIndex < 0) return;
        int step = CARD_W + 3;
        int col = 2 + visibleIndex * step + (CARD_W + 2) / 2;
        if (col >= cols) return;
        if (2 + (visibleIndex + 1) * (CARD_W + 3) > cols - 1) return;
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
            return "+3" + CardTokens.toEmoji(CardToken.FOOD);
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
        String prefix = " ── ";
        String text = String.format("YOUR TRIBE: %s  Food: %d  PP: %d",
                me.name(), me.food(), me.pp());

        // Prefix dashes — totem color, never highlighted
        tg.setForegroundColor(totemColor(me.totem()));
        tg.setBackgroundColor(TuiColor.BLACK);
        tg.putString(0, startRow, prefix);

        // Text — highlighted only on my turn
        if (isMyTurn) {
            tg.setBackgroundColor(totemColor(me.totem()));
            tg.setForegroundColor(TuiColor.BLACK);
        } else {
            tg.setForegroundColor(totemColor(me.totem()));
            tg.setBackgroundColor(TuiColor.BLACK);
        }
        int textCol = prefix.length();
        int textEnd = Math.min(cols, textCol + text.length());
        tg.putString(textCol, startRow,
                textEnd <= cols ? text : text.substring(0, cols - textCol));

        // Trailing dashes — totem color
        tg.setForegroundColor(totemColor(me.totem()));
        tg.setBackgroundColor(TuiColor.BLACK);
        int trailCol = textCol + text.length();
        int trailLen = Math.max(0, cols - trailCol - 1);
        if (trailLen > 0) {
            tg.putString(trailCol, startRow, " " + "─".repeat(trailLen - 1));
        }
        tg.setForegroundColor(TuiColor.WHITE);
        tg.setBackgroundColor(TuiColor.BLACK);

        List<SummaryChunk> chunks = new ArrayList<>();
        appendCharacterChunks(chunks, me);
        appendBuildingsChunks(chunks, me);
        if (!chunks.isEmpty()) {
            renderChunks(tg, 0, startRow + 1, chunks, me, isMyTurn, cols);
        }
    }

    private void drawOtherPlayers(TuiTextGraphics tg, int startRow, int cols) {
        tg.setForegroundColor(TuiColor.WHITE);
        tg.putString(0, startRow - 1, " ── OTHERS " + "─".repeat(Math.max(0, cols - 10)));

        Totem currentTotem = game.currentPlayerTotem();
        int row = startRow;
        for (PlayerDTO p : game.players()) {
            if (p.totem() == myTotem)
                continue;

            boolean isTheirTurn = (p.totem() == currentTotem);

            List<SummaryChunk> chunks = new ArrayList<>();
            chunks.add(new SummaryChunk(p.name(), true));
            chunks.add(new SummaryChunk(
                    String.format(" F:%d PP:%d %s ", p.food(), p.pp(), SEP),
                    false));
            appendCharacterChunks(chunks, p);
            appendBuildingsChunks(chunks, p);
            renderChunks(tg, 0, row, chunks, p, isTheirTurn, cols, true);
            row++;
        }
    }

    // ── Player card summary (full-text, compact) ───────────────────────────────

    private static final Pattern PP_FROM_EFFECT  = Pattern.compile("(\\d+)\\[PP\\]");
    private static final Pattern DISC_FROM_EFFECT = Pattern.compile("-(\\d+)\\[FOOD\\]");
    private static final Pattern PP_FROM_TYPE    = Pattern.compile("(\\d+)\\[PP\\]");

    private static final String SEP = "│";
    private static final String STAR_EMOJI = "★";

    /** Tagged text fragment: when {@code highlight} is true it renders in the player's totem color. */
    private record SummaryChunk(String text, boolean highlight) {}

    /** Builds character-section chunks: highlighted "N TYPE:" labels, plain detail strings. */
    private static void appendCharacterChunks(List<SummaryChunk> out, PlayerDTO p) {
        int h = sizeOf(p, CardType.HUNTER);
        int g = sizeOf(p, CardType.GATHERER);
        int a = sizeOf(p, CardType.ARTIST);
        if (h > 0) out.add(new SummaryChunk(h + " HUNTER ", false));
        if (g > 0) out.add(new SummaryChunk(g + " GATHERER ", false));
        if (a > 0) out.add(new SummaryChunk(a + " ARTIST ", false));

        Set<CardDTO> builders = cardsOf(p, CardType.BUILDER);
        if (!builders.isEmpty()) {
            int pp = 0, disc = 0;
            for (CardDTO c : builders) {
                pp   += firstInt(PP_FROM_EFFECT,  c.effectsLabel());
                disc += firstInt(DISC_FROM_EFFECT, c.effectsLabel());
            }
            out.add(new SummaryChunk(builders.size() + " BUILDER: ", false));
            out.add(new SummaryChunk("🌟:" + pp + ",🍖:-" + disc + " ", false));
        }

        Set<CardDTO> shamans = cardsOf(p, CardType.SHAMAN);
        if (!shamans.isEmpty()) {
            int stars = 0;
            for (CardDTO c : shamans) {
                stars += occurrences(c.effectsLabel(), "[SHAMAN_STAR]");
            }
            out.add(new SummaryChunk(shamans.size() + " SHAMAN: ", false));
            out.add(new SummaryChunk(STAR_EMOJI + ":" + stars + " ", false));
        }

        Set<CardDTO> inventors = cardsOf(p, CardType.INVENTOR);
        if (!inventors.isEmpty()) {
            Map<String, Integer> iconCount = new LinkedHashMap<>();
            for (CardDTO c : inventors) {
                String emoji = CardTokens.toEmoji(c.effectsLabel() == null ? "" : c.effectsLabel().trim());
                iconCount.merge(emoji, 1, Integer::sum);
            }
            out.add(new SummaryChunk(inventors.size() + " INVENTOR: ", false));
            StringBuilder icons = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, Integer> e : iconCount.entrySet()) {
                if (!first) icons.append(", ");
                first = false;
                icons.append(e.getKey()).append(':').append(e.getValue());
            }
            icons.append(' ');
            out.add(new SummaryChunk(icons.toString(), false));
        }
    }

    /** Buildings chunk: highlighted "│ BUILDINGS:" label, plain enumeration after. */
    private static void appendBuildingsChunks(List<SummaryChunk> out, PlayerDTO p) {
        Set<CardDTO> buildings = cardsOf(p, CardType.BUILDINGS);
        if (buildings.isEmpty()) return;
        out.add(new SummaryChunk(SEP + " BUILDINGS: ", false));
        StringBuilder body = new StringBuilder();
        boolean first = true;
        for (CardDTO c : buildings) {
            if (!first) body.append(" / ");
            first = false;
            int pp = firstInt(PP_FROM_TYPE, c.typeLabel());
            body.append("pp:").append(pp);
            String eff = CardTokens.toEffectLabel(c.effectsLabel());
            if (eff != null && !eff.isBlank()) {
                body.append('[').append(eff).append(']');
            }
        }
        out.add(new SummaryChunk(body.toString(), false));
    }

    /** Renders chunks left-to-right starting at {@code col}; highlighted chunks use totem color. */
    private void renderChunks(TuiTextGraphics tg, int col, int row,
                              List<SummaryChunk> chunks, PlayerDTO p,
                              boolean isTheirTurn, int cols) {
        renderChunks(tg, col, row, chunks, p, isTheirTurn, cols, false);
    }

    private void renderChunks(TuiTextGraphics tg, int col, int row,
                              List<SummaryChunk> chunks, PlayerDTO p,
                              boolean isTheirTurn, int cols,
                              boolean tintNonHighlightedWithTotem) {
        TuiColor nonHighlightFg = tintNonHighlightedWithTotem
                ? totemColor(p.totem()) : TuiColor.WHITE;
        for (SummaryChunk ch : chunks) {
            if (col >= cols) break;
            if (ch.highlight()) {
                if (isTheirTurn) {
                    tg.setBackgroundColor(totemColor(p.totem()));
                    tg.setForegroundColor(TuiColor.BLACK);
                } else {
                    tg.setForegroundColor(totemColor(p.totem()));
                    tg.setBackgroundColor(TuiColor.BLACK);
                }
            } else {
                tg.setForegroundColor(nonHighlightFg);
                tg.setBackgroundColor(TuiColor.BLACK);
            }
            String text = ch.text();
            int w = visualWidth(text);
            if (col + w > cols) {
                text = text.substring(0, Math.min(text.length(), cols - col));
                w = visualWidth(text);
            }
            tg.putString(col, row, text);
            col += w;
        }
        tg.setForegroundColor(TuiColor.WHITE);
        tg.setBackgroundColor(TuiColor.BLACK);
    }

    private static int sizeOf(PlayerDTO p, CardType t) {
        Set<CardDTO> s = p.cards().get(t);
        return s == null ? 0 : s.size();
    }

    private static Set<CardDTO> cardsOf(PlayerDTO p, CardType t) {
        Set<CardDTO> s = p.cards().get(t);
        return s == null ? Set.of() : s;
    }

    private static int firstInt(Pattern pattern, String s) {
        if (s == null) return 0;
        Matcher m = pattern.matcher(s);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    private static int occurrences(String s, String needle) {
        if (s == null || needle.isEmpty()) return 0;
        int count = 0, idx = 0;
        while ((idx = s.indexOf(needle, idx)) != -1) {
            count++;
            idx += needle.length();
        }
        return count;
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

    /**
     * Overlay drawn during EVENTS_EXECUTION: every other cell of the board area
     * gets an orange '/'. The current event title is centered in white over a
     * cleared band so it stays legible against the pattern.
     */
    private void drawEventsOverlay(TuiTextGraphics tg, TuiSize sz) {
        int cols = sz.getColumns();
        int rows = sz.getRows();
        if (cols <= 0 || rows <= 0) return;

        int top = 1;
        int bottom = Math.max(top, rows - 2);

        tg.setForegroundColor(TuiColor.ORANGE);
        tg.setBackgroundColor(TuiColor.BLACK);
        StringBuilder line = new StringBuilder(cols);
        for (int r = top; r <= bottom; r++) {
            line.setLength(0);
            for (int c = 0; c < cols; c++) {
                line.append(((c + r) % 2 == 0) ? '/' : ' ');
            }
            tg.putString(0, r, line.toString());
        }

        String current = overlayTitle;
        if (current == null) {
            tg.setForegroundColor(TuiColor.WHITE);
            tg.setBackgroundColor(TuiColor.BLACK);
            return;
        }

        String title = "  " + current.toUpperCase() + "  ";
        int titleW = title.length();
        int titleCol = Math.max(0, (cols - titleW) / 2);

        List<String> playerLines = parseOverlayPlayerLines(overlayLog);
        int blockHeight = 3 + (playerLines.isEmpty() ? 0 : playerLines.size() + 1);
        int blockTop = Math.max(top, (top + bottom) / 2 - blockHeight / 2);
        int titleRow = blockTop + 1;

        tg.setForegroundColor(TuiColor.WHITE);
        tg.setBackgroundColor(TuiColor.BLACK);
        for (int r = blockTop; r < blockTop + blockHeight && r <= bottom; r++) {
            tg.putString(0, r, " ".repeat(cols));
        }
        tg.putString(titleCol, titleRow, title);

        int lineRow = titleRow + 2;
        for (String pl : playerLines) {
            if (lineRow > bottom) break;
            int col = Math.max(0, (cols - pl.length()) / 2);
            tg.putString(col, lineRow, pl);
            lineRow++;
        }

        tg.setForegroundColor(TuiColor.WHITE);
        tg.setBackgroundColor(TuiColor.BLACK);
    }

    /**
     * Splits the server-side log message ("Title — Player1: +2 PP | Player2: no change …")
     * into the per-player segments after the em-dash separator. Empty if the
     * message is missing or doesn't follow the expected format — the overlay
     * then falls back to showing only the title.
     */
    private static List<String> parseOverlayPlayerLines(String log) {
        if (log == null || log.isBlank()) return Collections.emptyList();
        int sep = log.indexOf(" — ");
        if (sep < 0) return Collections.emptyList();
        String body = log.substring(sep + " — ".length());
        List<String> out = new ArrayList<>();
        for (String seg : body.split("\\s\\|\\s")) {
            String t = seg.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
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
            case TOTEM_PICKING -> "TOTEM PICKING";
        };
    }

    private TuiColor totemColor(Totem totem) {
        if (totem == null)
            return TuiColor.WHITE;
        return switch (totem) {
            case RED -> TuiColor.RED;
            case BLUE -> TuiColor.CYAN;
            case WHITE -> TuiColor.WHITE;
            case BLACK -> TuiColor.DARK_PURPLE;
            case YELLOW -> TuiColor.YELLOW;
        };
    }

    /**
     * Visual width of a string in terminal columns (Windows Terminal rules).
     *   supplementary emoji (surrogate
     *   pair) + U+FE0F  → 3 cols
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

    private void drawRowViewCursor(TuiTextGraphics tg, int topRowY, int botRowY) {
        tg.setForegroundColor(TuiColor.YELLOW);
        tg.putString(0, topRowY, viewOnTopRow ? "►" : " ");
        tg.putString(0, botRowY, viewOnTopRow ? " " : "►");
        tg.setForegroundColor(TuiColor.WHITE);
    }

    private void drawLegend(TuiTextGraphics tg, TuiSize sz) {
        int w = 38;
        int h = 14;
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
            {"─── SYMBOLS ───────────────────────"},
            {"🌟", "Prestige Points (PP)"},
            {"★", " Shaman ritual stars"},
            {"💰", "Food cost"},
            {"🍖", "Extra food on pick"},
            {"🗿", "Totem symbol"},
            {"🛡️", " Immunity"},
            {"🏁", "End-game PP bonus"},
            {"🌈", "Set collection bonus"},
            {"🧍", "Character card"},
            {" I II III", "Card Era"},
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

    // ── Rules overlay ─────────────────────────────────────────────────────────

    /**
     * Loads the raw text of the game rules from the embedded classpath resource
     * {@code /assets/rules/tui_textual_rules} (shipped in the jar under the assets target
     * directory). The text is cached so it's only loaded once per session. The file uses
     * {@code --- PAGE N of 8: ... ---} separators to split
     * sections; everything between two separators is one page.
     */
    private void toggleRules() {
        if (showRules) {
            showRules = false;
            return;
        }
        if (rulesPages == null) {
            rulesPages = loadRulesPages();
        }
        rulesPageIndex = 0;
        showSummaryCard = false;
        showRules = true;
    }

    private void toggleSummaryCard() {
        if (showSummaryCard) {
            showSummaryCard = false;
        } else {
            showRules = false;
            showSummaryCard = true;
        }
    }

    /** Reads the rules file and splits it into pages on the separator lines. */
    private List<String> loadRulesPages() {
        List<String> pages = new ArrayList<>();
        try {
            var stream = getClass().getResourceAsStream("/assets/rules/tui_textual_rules");
            if (stream == null) {
                pages.add("(Rules file not found.)");
                return pages;
            }
            String full = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            // Split on separator lines that look like: "--- PAGE N of M: ... ---"
            String[] parts = full.split("(?m)^---\\s+PAGE\\s+\\d+.*?---\\s*$");
            for (String part : parts) {
                String trimmed = part.strip();
                if (!trimmed.isEmpty()) pages.add(trimmed);
            }
            if (pages.isEmpty()) pages.add(full.strip());
        } catch (Exception ex) {
            pages.add("(Error loading rules: " + ex.getMessage() + ")");
        }
        return pages;
    }

    /**
     * Draws the rules overlay as a large centered box (≈88% wide, ≈80% tall).
     * The current page text is word-wrapped to fit the inner width. The board
     * is still visible on the edges behind the overlay.
     */
    private void drawRulesWindow(TuiTextGraphics tg, TuiSize sz) {
        if (rulesPages == null || rulesPages.isEmpty()) return;

        int cols = sz.getColumns();
        int rows = sz.getRows();

        // Panel dimensions — large but not fullscreen so the board shows on edges.
        // Capped at 160 width and 43 height to avoid huge empty spaces but allow fullscreen text.
        int w = Math.min(160, Math.max(40, cols * 80 / 100));
        int h = Math.min(43, Math.max(14, rows * 75 / 100));
        int x = (cols - w) / 2;
        int y = (rows - h) / 2;

        // Draw panel background (fill with spaces to erase board content).
        tg.setBackgroundColor(TuiColor.BLACK);
        tg.setForegroundColor(TuiColor.WHITE);
        for (int r = y; r < y + h && r < rows; r++) {
            tg.putString(x, r, " ".repeat(w));
        }

        // Box border in cyan.
        tg.setForegroundColor(TuiColor.CYAN);
        String hbar = "─".repeat(w - 2);
        tg.putString(x,         y,         "┌" + hbar + "┐");
        tg.putString(x,         y + h - 1, "└" + hbar + "┘");
        for (int r = y + 1; r < y + h - 1; r++) {
            tg.putString(x,         r, "│");
            tg.putString(x + w - 1, r, "│");
        }

        // ── Title bar on row y+1: yellow background, black text ──────────────
        int pageNum   = rulesPageIndex + 1;
        int pageTotal = rulesPages.size();
        String titleLeft  = "  MESOS RULES  —  page " + pageNum + " of " + pageTotal + "  ";
        String titleRight = "  ← prev   → next  ";
        int fillLen = w - 2 - titleLeft.length() - titleRight.length();
        String titleRow = titleLeft + " ".repeat(Math.max(0, fillLen)) + titleRight;
        if (titleRow.length() > w - 2) titleRow = titleRow.substring(0, w - 2);
        tg.setBackgroundColor(TuiColor.YELLOW);
        tg.setForegroundColor(TuiColor.BLACK);
        tg.putString(x + 1, y + 1, titleRow);
        tg.setBackgroundColor(TuiColor.BLACK);

        // ── Separator on row y+2 ──────────────────────────────────────────────
        tg.setForegroundColor(TuiColor.CYAN);
        tg.putString(x, y + 2, "├" + "─".repeat(w - 2) + "┤");

        // ── Inner text area: rows y+3 to y+h-3, hint on y+h-2 ───────────────
        int innerW = w - 4;
        int innerH = h - 5;  // top + titlebar + separator + hintrow + bottom = 5 fixed rows
        int textX  = x + 2;
        int textY  = y + 3;

        String pageText = rulesPages.get(Math.min(rulesPageIndex, rulesPages.size() - 1));
        List<String> lines = wrapText(pageText, innerW);

        int maxScroll = Math.max(0, lines.size() - innerH);
        rulesScrollOffset = Math.min(rulesScrollOffset, maxScroll);

        // Render content lines; section headers (ALL-CAPS) in orange, body in white.
        int lineY = textY;
        for (int i = rulesScrollOffset; i < lines.size() && lineY < textY + innerH; i++, lineY++) {
            String line = lines.get(i);
            boolean isHeader = !line.isEmpty()
                    && line.equals(line.toUpperCase())
                    && line.matches("[A-Z0-9 /\\-()&,:!?'🌟🧍🏠🍖🏁]+")
                    && !line.matches("^[0-9].*");  // numbered building items stay white
            boolean isSubHeader = !line.isEmpty()
                    && line.equals(line.toUpperCase())
                    && line.matches("[ A-Z\\[\\]]+");  // numbered building items stay white
            if (isSubHeader) {
                tg.setForegroundColor(TuiColor.YELLOW);
            } else if (isHeader) {
                tg.setForegroundColor(TuiColor.ORANGE);
            } else {
                tg.setForegroundColor(TuiColor.WHITE);
            }
            tg.putString(textX, lineY, padRight(line, innerW));
        }
        // Bottom hint line inside the box.
        if (rulesScrollOffset < maxScroll) {
            tg.setForegroundColor(TuiColor.DARK_GRAY);
            tg.putString(x + 2, y + h - 2, " ... ");
        }

        tg.setForegroundColor(TuiColor.WHITE);
        tg.setBackgroundColor(TuiColor.BLACK);
    }

    private void drawSummaryCardWindow(TuiTextGraphics tg, TuiSize sz) {
        int cols = sz.getColumns();
        int rows = sz.getRows();

        int w = 55;
        int h = 28;
        int x = Math.max(0, (cols - w) / 2);
        int y = Math.max(0, (rows - h) / 2);

        tg.setBackgroundColor(TuiColor.BLACK);
        tg.setForegroundColor(TuiColor.WHITE);
        for (int r = y; r < y + h && r < rows; r++) {
            tg.putString(x, r, " ".repeat(w));
        }

        tg.setForegroundColor(TuiColor.CYAN);
        String hbar = "─".repeat(w - 2);
        tg.putString(x, y, "┌" + hbar + "┐");
        tg.putString(x, y + h - 1, "└" + hbar + "┘");
        for (int r = y + 1; r < y + h - 1; r++) {
            tg.putString(x, r, "│");
            tg.putString(x + w - 1, r, "│");
        }

        tg.setBackgroundColor(TuiColor.YELLOW);
        tg.setForegroundColor(TuiColor.BLACK);
        String title = "  SUMMARY CARD  ";
        int fillLen = w - 2 - title.length();
        String titleRow = " ".repeat(fillLen / 2) + title + " ".repeat(fillLen - fillLen / 2);
        tg.putString(x + 1, y + 1, titleRow);
        tg.setBackgroundColor(TuiColor.BLACK);

        tg.setForegroundColor(TuiColor.CYAN);
        tg.putString(x, y + 2, "├" + "─".repeat(w - 2) + "┤");

        int midX = x + w / 2;
        for (int r = y + 3; r < y + h - 1; r++) {
            tg.putString(midX, r, "│");
        }
        tg.putString(midX, y + 2, "┬");
        tg.putString(midX, y + h - 1, "┴");

        int colW = (w - 3) / 2;
        String evTitle = "EVENTS";
        String egTitle = "END GAME " + org.adsl.client.view.tui.CardTokens.toEmoji(CardToken.ENDGAME);

        tg.setBackgroundColor(TuiColor.MAGENTA);
        tg.setForegroundColor(TuiColor.WHITE);
        tg.putString(x + 1 + (colW - evTitle.length()) / 2, y + 3, evTitle);
        
        tg.setBackgroundColor(TuiColor.RED);
        tg.setForegroundColor(TuiColor.WHITE);
        tg.putString(midX + 1 + (colW - visualWidth(egTitle)) / 2, y + 3, egTitle);
        
        tg.setBackgroundColor(TuiColor.BLACK);
        tg.setForegroundColor(TuiColor.WHITE);

        int boxW = 22;
        int boxH = 5;
        int startY = y + 4;

        // MATRICE TESTI SINISTRA (EVENTS) - [box][riga]
        String[][] leftTexts = {
                {"HUNT(HNT)", "↓ ↓ ↓", "1🍖+N🌟 x HUN"},
                {"SUSTENANCE(SUS)", "↓ ↓ ↓", "-1🍖/N🌟 x 🧍"},
                {"SHAMANIC RITUAL(RIT)", "↓ ↓ ↓", ">★:N🌟 | <★:-N🌟"},
                {"CAVE PAINTINGS(PAI)", "↓ ↓ ↓", ">A:+N🌟xA|<A:-N🌟"}
        };

        // MATRICE TESTI DESTRA (END GAME) - [box][riga]
        String[][] rightTexts = {
                {"", "BLD: SUM🌟", ""}, // Box 1
                {"", "INV: 🌟 = INV x ICON", ""}, // Box 2
                {"", "ART: 10🌟 X 2ART", ""}, // Box 3
                {"", "BUI: 🏁 + SUM🌟", ""}  // Box 4
        };

        int leftX = x + 1 + (colW - boxW) / 2;
        int rightX = midX + 1 + (colW - boxW) / 2;

        for (int i = 0; i < 4; i++) {
            int boxY = startY + i * (boxH + 1);
            if (boxY + boxH > y + h - 1) break;

            drawCardBox(tg, leftX, boxY, boxW, boxH, leftTexts[i][0], leftTexts[i][1], leftTexts[i][2]);
            drawCardBox(tg, rightX, boxY, boxW, boxH, rightTexts[i][0], rightTexts[i][1], rightTexts[i][2]);
        }
    }

    private void drawCardBox(TuiTextGraphics tg, int x, int y, int w, int h, String line1, String line2, String line3) {
        tg.setForegroundColor(TuiColor.DARK_GRAY);
        String hbar = "─".repeat(w - 2);
        tg.putString(x, y, "┌" + hbar + "┐");
        tg.putString(x, y + h - 1, "└" + hbar + "┘");
        for (int r = y + 1; r < y + h - 1; r++) {
            tg.putString(x, r, "│");
            tg.putString(x + w - 1, r, "│");
        }
        
        // Stampa testi all'interno del box se non vuoti
        tg.setForegroundColor(TuiColor.WHITE);
        int innerW = w - 2;
        if (!line1.isEmpty()) tg.putString(x + 1, y + 1, padRight(padLeft(line1, (innerW + visualWidth(line1)) / 2), innerW));
        if (!line2.isEmpty()) tg.putString(x + 1, y + 2, padRight(padLeft(line2, (innerW + visualWidth(line2)) / 2), innerW));
        if (!line3.isEmpty()) tg.putString(x + 1, y + 3, padRight(padLeft(line3, (innerW + visualWidth(line3)) / 2), innerW));
    }

    /**
     * Word-wraps {@code text} to at most {@code maxW} visible characters per line.
     * Preserves intentional blank lines (paragraph breaks) and treats CRLF/LF uniformly.
     */
    private static List<String> wrapText(String text, int maxW) {
        if (text == null || text.isBlank()) return List.of();
        List<String> result = new ArrayList<>();
        text.lines().forEach(paragraph -> {
            if (paragraph.isBlank()) {
                result.add("");
                return;
            }
            StringBuilder current = new StringBuilder();
            for (String word : paragraph.split("\\s+")) {
                if (word.isEmpty()) continue;
                if (current.isEmpty()) {
                    current.append(word);
                } else if (current.length() + 1 + word.length() <= maxW) {
                    current.append(' ').append(word);
                } else {
                    result.add(current.toString());
                    current.setLength(0);
                    current.append(word);
                }
            }
            if (!current.isEmpty()) result.add(current.toString());
        });
        return result;
    }
}
