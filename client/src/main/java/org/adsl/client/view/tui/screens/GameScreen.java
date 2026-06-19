package org.adsl.client.view.tui.screens;

import org.adsl.client.AppCoordinator;
import org.adsl.client.serverEvents.EndGameEvent;
import org.adsl.client.serverEvents.ErrorEvent;
import org.adsl.client.serverEvents.EventsTriggeredEvent;
import org.adsl.client.serverEvents.GameUpdateEvent;
import org.adsl.client.view.GameViewModel;
import org.adsl.client.view.tui.CardCatalog;
import org.adsl.client.view.tui.CardTokens;
import org.adsl.client.view.tui.game.TuiCardWindows;
import org.adsl.client.view.tui.game.TuiEventsOverlay;
import org.adsl.client.view.tui.game.TuiSummaryChunks;
import org.adsl.client.view.tui.events.*;
import org.adsl.client.view.tui.events.CharInputEvent;
import org.adsl.client.view.tui.render.TuiColor;
import org.adsl.client.view.tui.render.TuiSize;
import org.adsl.client.view.tui.render.TuiTerminal;
import org.adsl.client.view.tui.render.TuiText;
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
    /** Shared read model over {@link #game} (used for player-name lookups). */
    private GameViewModel vm;
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
        this.vm = new GameViewModel(initialGame, username);
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
            rulesScrollOffset = TuiCardWindows.drawRules(tg, sz, rulesPages, rulesPageIndex, rulesScrollOffset);
        } else if (showSummaryCard) {
            TuiCardWindows.drawSummaryCard(tg, sz);
        }

        if (showLog) {
            drawLogWindow(tg, sz);
        } else {
            drawLogPreview(tg, sz);
        }

        if (overlayTitle != null) {
            TuiEventsOverlay.draw(tg, sz, overlayTitle, overlayLog);
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
        this.vm = new GameViewModel(this.game, username);
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
            if (TuiText.wrapText(pageText, w - 4).size() > innerH) {
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
            tg.putString(col, startRow + 1, "│ " + TuiText.padRight(TuiText.padLeft(playerName, (8 + TuiText.visualWidth(playerName)) / 2), 8) + " │");
            tg.putString(col, startRow + 2, "│ " + TuiText.padRight(TuiText.padLeft(movesLabel, (8 + TuiText.visualWidth(movesLabel)) / 2), 8) + " │");
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
                        : (TuiText.visualWidth(rawTl) + 1 + TuiText.visualWidth(costStr) <= CARD_W)
                                ? rawTl + " " + costStr
                                : rawTl + costStr;
                String tl = TuiText.padRight(TuiText.padLeft(combined, (CARD_W + TuiText.visualWidth(combined)) / 2), CARD_W);
                String rawEl = CardTokens.toEffectLabel(card.effectsLabel() != null ? card.effectsLabel() : "");
                String el = TuiText.padRight(TuiText.padLeft(rawEl, (CARD_W + TuiText.visualWidth(rawEl)) / 2), CARD_W);


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

        List<TuiSummaryChunks.Chunk> chunks = new ArrayList<>();
        TuiSummaryChunks.appendCharacterChunks(chunks, me);
        TuiSummaryChunks.appendBuildingsChunks(chunks, me);
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

            List<TuiSummaryChunks.Chunk> chunks = new ArrayList<>();
            chunks.add(new TuiSummaryChunks.Chunk(p.name(), true));
            chunks.add(new TuiSummaryChunks.Chunk(
                    String.format(" F:%d PP:%d %s ", p.food(), p.pp(), TuiSummaryChunks.SEP),
                    false));
            TuiSummaryChunks.appendCharacterChunks(chunks, p);
            TuiSummaryChunks.appendBuildingsChunks(chunks, p);
            renderChunks(tg, 0, row, chunks, p, isTheirTurn, cols, true);
            row++;
        }
    }

    /** Renders chunks left-to-right starting at {@code col}; highlighted chunks use totem color. */
    private void renderChunks(TuiTextGraphics tg, int col, int row,
                              List<TuiSummaryChunks.Chunk> chunks, PlayerDTO p,
                              boolean isTheirTurn, int cols) {
        renderChunks(tg, col, row, chunks, p, isTheirTurn, cols, false);
    }

    private void renderChunks(TuiTextGraphics tg, int col, int row,
                              List<TuiSummaryChunks.Chunk> chunks, PlayerDTO p,
                              boolean isTheirTurn, int cols,
                              boolean tintNonHighlightedWithTotem) {
        TuiColor nonHighlightFg = tintNonHighlightedWithTotem
                ? totemColor(p.totem()) : TuiColor.WHITE;
        for (TuiSummaryChunks.Chunk ch : chunks) {
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
            int w = TuiText.visualWidth(text);
            if (col + w > cols) {
                text = text.substring(0, Math.min(text.length(), cols - col));
                w = TuiText.visualWidth(text);
            }
            tg.putString(col, row, text);
            col += w;
        }
        tg.setForegroundColor(TuiColor.WHITE);
        tg.setBackgroundColor(TuiColor.BLACK);
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
        return vm.playerName(totem);
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
                tg.putString(x + 2, lineY, TuiText.padRight(entry[0], w - 4));
                tg.setForegroundColor(TuiColor.WHITE);
            } else {
                int tokenW = TuiText.visualWidth(entry[0]);
                boolean hasFE0F = entry[0].indexOf('️') >= 0;
                String sep = hasFE0F ? "" : " ";
                int descW = hasFE0F ? (w - 4 - tokenW) : (w - 5 - tokenW);
                tg.putString(x + 2, lineY, entry[0] + sep + TuiText.padRight(entry[1], descW));
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
            rulesPages = TuiCardWindows.loadRulesPages();
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

}
