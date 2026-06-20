package org.adsl.client.view.tui.screens;

import org.adsl.client.AppCoordinator;
import org.adsl.client.serverEvents.EndGameEvent;
import org.adsl.client.serverEvents.ErrorEvent;
import org.adsl.client.serverEvents.EventsTriggeredEvent;
import org.adsl.client.serverEvents.GameUpdateEvent;
import org.adsl.client.view.GameViewModel;
import org.adsl.client.view.tui.game.TuiBoardRenderer;
import org.adsl.client.view.tui.game.TuiBoardRenderer.BoardFrame;
import org.adsl.client.view.tui.game.TuiCardWindows;
import org.adsl.client.view.tui.game.TuiDeckInspector;
import org.adsl.client.view.tui.game.TuiEventsOverlay;
import org.adsl.client.view.tui.game.TuiLegendOverlay;
import org.adsl.client.view.tui.game.TuiPlayerPanels;
import org.adsl.client.view.tui.events.*;
import org.adsl.client.view.tui.events.CharInputEvent;
import org.adsl.client.view.tui.render.TuiColor;
import org.adsl.client.view.tui.render.TuiSize;
import org.adsl.client.view.tui.render.TuiTerminal;
import org.adsl.client.view.tui.render.TuiText;
import org.adsl.client.view.tui.render.TuiTextGraphics;
import org.adsl.shared.enums.Phase;
import org.adsl.shared.enums.Row;
import org.adsl.shared.enums.Totem;
import org.adsl.shared.model.*;
import org.adsl.shared.utils.Move;

import java.io.IOException;
import java.util.*;

/**
 * Main game screen, implemented as a non-blocking state machine that drives the
 * stateless board/panel renderers in {@link org.adsl.client.view.tui.game}.
 *
 * <p>Server events ({@link GameUpdateEvent}, {@link EndGameEvent}) and input
 * events ({@link ConfirmEvent}, {@link SelectEvent}, navigate events) are all
 * dispatched through the visitor pattern. This class keeps only controller state
 * (turn sub-state, selection, scroll offsets and overlay flags); each render
 * pass it builds an immutable {@link BoardFrame} snapshot and hands it, plus the
 * player panels and overlays, to the dedicated renderers — mirroring the GUI's
 * {@code GameScreen} / {@code BoardRenderer} / {@code PlayerPanelsRenderer} split.
 */
public class GameScreen extends TUIScreen {

    /**
     * The four turn sub-states. Each knows how to render its own controls hint,
     * so the screen never branches on the enum to build the bottom bar.
     */
    private enum SubState {
        MY_TURN_TOTEM {
            @Override String hint(GameScreen s) {
                return "← → Offer tiles   " + NAV + "   ENTER Place totem   " + OVERLAYS;
            }
        },
        MY_TURN_CARDS {
            @Override String hint(GameScreen s) {
                return String.format("← → Navigate   %s   SPACE Select (%d/%d)   ENTER Confirm   %s",
                        NAV, s.selectedMoves.size(), s.upperCount + s.lowerCount, OVERLAYS);
            }
        },
        NOT_MY_TURN {
            @Override String hint(GameScreen s) {
                return s.waitingHint() + "   " + NAV + "   " + OVERLAYS;
            }
        },
        WAITING_SERVER {
            @Override String hint(GameScreen s) {
                return "Waiting for server...   " + NAV + "   " + OVERLAYS;
            }
        };

        /** Shared trailing segments of the controls hint. */
        static final String NAV = "↑ ↓ Switch rows   A/D Scroll   Q/E Jump ends";
        static final String OVERLAYS = "C Decks   L Legend   M Log   R Rules   S SC";

        abstract String hint(GameScreen s);
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

    // Deck inspector overlay (C key): owns its own player/scroll state.
    private final TuiDeckInspector decks = new TuiDeckInspector();

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

        TuiBoardRenderer.render(tg, sz, buildFrame());

        if (decks.isOpen()) {
            decks.draw(tg, sz, 19, game, myTotem, visibleCardsCount());
        } else {
            TuiPlayerPanels.drawCurrentPlayerTribe(tg, game, myTotem, 20, sz.getColumns());
            TuiPlayerPanels.drawOtherPlayers(tg, game, myTotem, 24, sz.getColumns());
        }

        TuiBoardRenderer.drawControls(tg, sz, buildHint(sz));

        if (showLegend) {
            TuiLegendOverlay.draw(tg, sz);
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

    /**
     * Builds the immutable per-frame snapshot the board renderer draws. Which
     * cursor is active follows from the (visual) sub-state: card cursor while
     * picking, offer cursor while placing a totem, passive row marker otherwise.
     */
    private BoardFrame buildFrame() {
        SubState vs = visualState();
        boolean picking = vs == SubState.MY_TURN_CARDS;
        boolean placing = vs == SubState.MY_TURN_TOTEM;

        BoardFrame.CardCursor cardCursor = picking
                ? new BoardFrame.CardCursor(selectionIndex,
                        onTopRow ? topRowOffset : lowRowOffset, onTopRow)
                : null;
        int offerCursor = placing ? selectionIndex : -1;
        BoardFrame.RowFocus rowFocus = picking ? null
                : (viewOnTopRow ? BoardFrame.RowFocus.TOP : BoardFrame.RowFocus.BOTTOM);
        Set<Move> highlights = picking ? selectedMoves : Collections.emptySet();

        return new BoardFrame(game, vm, myTotem, highlights, offerCursor,
                cardCursor, rowFocus, topRowOffset, lowRowOffset);
    }

    /** Bottom controls hint: overlay-specific text when an overlay is open, else
     *  the active sub-state's own hint. */
    private String buildHint(TuiSize sz) {
        if (showRules) {
            int w = Math.min(160, Math.max(40, sz.getColumns() * 80 / 100));
            int h = Math.min(43, Math.max(14, sz.getRows() * 75 / 100));
            int innerH = h - 6;
            String pageText = rulesPages.get(Math.min(rulesPageIndex, rulesPages.size() - 1));
            return TuiText.wrapText(pageText, w - 4).size() > innerH
                    ? "R Close rules   ← Prev page   → Next page   ↑ ↓ Scroll text"
                    : "R Close rules   ← Prev page   → Next page";
        }
        if (showSummaryCard) return "S Close SC";
        if (showLog)         return "↑ ↓ Scroll log   M Close log";
        if (decks.isOpen())  return "↑ ↓ Player   A/D Scroll   Q/E Jump ends   C Close decks   L Legend   M Log   R Rules   S SC";
        return subState.hint(this);
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
        // A rejected move leaves the game state unchanged, so restore the exact
        // interactive sub-state captured before the move was sent.
        if (subState == SubState.WAITING_SERVER) {
            subState = preWaitState;
        }
        return this;
    }

    // ── Input event visitors ──────────────────────────────────────────────────

    @Override
    public TUIScreen visit(ConfirmEvent e) {
        if (subState == SubState.MY_TURN_TOTEM) {
            confirmTotemPlacement();
        } else if (subState == SubState.MY_TURN_CARDS) {
            confirmCardSelection();
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
            decks.toggle();
        } else if (c == 'a' || c == 'A') {
            if (decks.isOpen()) decks.scrollLeft(); else scrollCurrentRowLeft();
        } else if (c == 'd' || c == 'D') {
            if (decks.isOpen()) decks.scrollRight(game, visibleCardsCount()); else scrollCurrentRowRight();
        } else if (c == 'q' || c == 'Q') {
            if (decks.isOpen()) decks.jumpStart(); else jumpRowStart();
        } else if (c == 'e' || c == 'E') {
            if (decks.isOpen()) decks.jumpEnd(game, visibleCardsCount()); else jumpRowEnd();
        }
        return this;
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
        if (decks.isOpen()) {
            decks.scrollLeft();
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
        if (decks.isOpen()) {
            decks.scrollRight(game, visibleCardsCount());
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
        if (decks.isOpen()) {
            decks.playerPrev();
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
        if (decks.isOpen()) {
            decks.playerNext(game);
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

    // ── Row navigation helpers ──────────────────────────────────────────────────

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
        return Math.max(1, (cols - 3) / (TuiBoardRenderer.CARD_W + 3));
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

    // ── Display helpers ───────────────────────────────────────────────────────

    private String nameFor(Totem totem) {
        return vm.playerName(totem);
    }

    /**
     * Fallback hint while it isn't this player's turn. Phases without a current
     * player (END_ROUND, EVENTS, END_GAME) used to render as "Waiting for ?..."
     * which looked broken; a phase-specific message is shown instead, looked up
     * declaratively.
     */
    private static final Map<Phase, String> WAITING_BY_PHASE = new EnumMap<>(Map.of(
            Phase.EVENTS_EXECUTION, "Resolving events...",
            Phase.END_ROUND,        "Resolving end-of-round...",
            Phase.END_GAME,         "Game over."));

    private String waitingHint() {
        if (game.currentPlayerTotem() != null) {
            return "Waiting for " + nameFor(game.currentPlayerTotem()) + "...";
        }
        return WAITING_BY_PHASE.getOrDefault(game.phase(), "Waiting...");
    }

    private void flashError(TuiTextGraphics tg, TuiSize sz, String msg) {
        tg.setForegroundColor(TuiColor.RED);
        tg.putString(2, sz.getRows() - 2,
                "! " + msg + " ".repeat(Math.max(0, sz.getColumns() - msg.length() - 4)));
        tg.setForegroundColor(TuiColor.WHITE);
    }

    // ── Overlay toggles ─────────────────────────────────────────────────────────

    /**
     * Loads the raw text of the game rules (cached for the session) and opens the
     * rules overlay, closing the summary card if it was showing.
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
