package org.adsl.client.view.tui.screens;

import org.adsl.client.AppCoordinator;
import org.adsl.client.view.tui.events.ConfirmEvent;
import org.adsl.client.serverEvents.HomeUpdateEvent;
import org.adsl.client.serverEvents.LobbyUpdateEvent;
import org.adsl.client.view.tui.events.NavigateDownEvent;
import org.adsl.client.view.tui.events.NavigateLeftEvent;
import org.adsl.client.view.tui.events.NavigateRightEvent;
import org.adsl.client.view.tui.events.NavigateUpEvent;
import org.adsl.client.view.tui.events.CharInputEvent;
import org.adsl.client.view.tui.render.TuiColor;
import org.adsl.client.view.tui.render.TuiSize;
import org.adsl.client.view.tui.render.TuiTerminal;
import org.adsl.client.view.tui.render.TuiTextGraphics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Non-blocking home menu rendered with TextGraphics. Lets the player create
 * or join a game and stays in sync with {@link HomeUpdateEvent}s as new
 * active games are announced by the server. Transitions to {@link LobbyScreen}
 * on {@link LobbyUpdateEvent}.
 */
public class HomeScreen extends TUIScreen {

    private static final int CREATE_OPTIONS = 4; // 2..5 players

    private List<Integer> activeGames;
    private Map<Integer, List<String>> gamePlayers;
    private Map<Integer, Integer> gameCapacity;
    private int cursor = 0;
    private int scrollOffset = 0;

    /**
     * @param terminal     the TUI terminal used for rendering
     * @param coordinator  the coordinator used to send create/join/logout requests
     * @param username     the current player's username
     * @param activeGames  ids of games currently open for joining, or {@code null} if none
     * @param gamePlayers  per-game list of player names already joined, or {@code null}
     * @param gameCapacity per-game maximum number of players, or {@code null}
     */
    public HomeScreen(TuiTerminal terminal,
                      AppCoordinator coordinator,
                      String username,
                      List<Integer> activeGames,
                      Map<Integer, List<String>> gamePlayers,
                      Map<Integer, Integer> gameCapacity) {
        super(terminal, coordinator, username);
        this.activeGames = (activeGames != null) ? new ArrayList<>(activeGames) : new ArrayList<>();
        this.gamePlayers = (gamePlayers != null) ? gamePlayers : Collections.emptyMap();
        this.gameCapacity = (gameCapacity != null) ? gameCapacity : Collections.emptyMap();
    }

    @Override
    public void render() throws IOException {
        terminal.clear();
        TuiTextGraphics tg = terminal.newTextGraphics();
        TuiSize size = terminal.getTerminalSize();
        int cols = size.getColumns();
        int rows = size.getRows();

        // Title bar
        tg.setForegroundColor(TuiColor.BLACK);
        tg.setBackgroundColor(TuiColor.YELLOW);
        String title = "  M E S O S  –  Home  ";
        tg.putString(Math.max(0, (cols - title.length()) / 2), 0, title);
        tg.setForegroundColor(TuiColor.WHITE);
        tg.setBackgroundColor(TuiColor.BLACK);

        int row = 2;
        tg.putString(4, row++, "Welcome, " + username + "!");
        row++;
        tg.setForegroundColor(TuiColor.DARK_GRAY);
        tg.putString(4, row++, "↑ ↓ / ← → navigate   ENTER confirm   B back to login");
        tg.setForegroundColor(TuiColor.WHITE);
        row++;

        // ── CREATE section ────────────────────────────────────────
        tg.setForegroundColor(TuiColor.CYAN);
        tg.putString(4, row++, sectionHeader("CREATE"));
        for (int i = 0; i < CREATE_OPTIONS; i++) {
            String prefix = (i == cursor) ? " > " : "   ";
            tg.setForegroundColor(i == cursor ? TuiColor.YELLOW : TuiColor.WHITE);
            tg.putString(4, row++, prefix + optionLabel(i));
        }
        tg.setForegroundColor(TuiColor.WHITE);
        row++;

        // ── JOIN section ─────────────────────────────────────────
        // Rows reserved after JOIN: blank(1) + back-header(1) + back-option(1) + blank(1) + error(2) = 6
        int viewportSize = Math.max(1, rows - row - 6);

        // Clamp scrollOffset so cursor stays visible
        if (isJoinOption(cursor)) {
            int cursorInJoin = cursor - CREATE_OPTIONS;
            if (cursorInJoin < scrollOffset) scrollOffset = cursorInJoin;
            if (cursorInJoin >= scrollOffset + viewportSize) scrollOffset = cursorInJoin - viewportSize + 1;
        }

        String gameWord = activeGames.size() == 1 ? "game" : "games";
        String joinLabel = activeGames.isEmpty() ? "JOIN" : "JOIN (" + activeGames.size() + " " + gameWord + ")";
        tg.setForegroundColor(TuiColor.CYAN);
        tg.putString(4, row++, sectionHeader(joinLabel));
        tg.setForegroundColor(TuiColor.WHITE);

        if (activeGames.isEmpty()) {
            tg.setForegroundColor(TuiColor.DARK_GRAY);
            tg.putString(7, row++, "(no active games)");
            tg.setForegroundColor(TuiColor.WHITE);
        } else {
            int end = Math.min(scrollOffset + viewportSize, activeGames.size());
            for (int g = scrollOffset; g < end; g++) {
                int i = CREATE_OPTIONS + g;
                String prefix = (i == cursor) ? " > " : "   ";
                boolean full = isGameFull(activeGames.get(g));
                TuiColor color = full ? TuiColor.DARK_GRAY : (i == cursor ? TuiColor.YELLOW : TuiColor.WHITE);
                tg.setForegroundColor(color);
                tg.putString(4, row, prefix + optionLabel(i));
                // Scroll indicators on right edge
                if (g == scrollOffset && scrollOffset > 0) {
                    tg.setForegroundColor(TuiColor.CYAN);
                    tg.putString(cols - 3, row, " ^ ");
                }
                if (g == end - 1 && end < activeGames.size()) {
                    tg.setForegroundColor(TuiColor.CYAN);
                    tg.putString(cols - 3, row, " v ");
                }
                row++;
            }
            tg.setForegroundColor(TuiColor.WHITE);
        }
        row++;

        // ── BACK ──────────────────────────────────────────────────
        int backIdx = totalOptions() - 1;
        row++; // blank row separates games from back option
        String backPrefix = (cursor == backIdx) ? " > " : "   ";
        tg.setForegroundColor(cursor == backIdx ? TuiColor.YELLOW : TuiColor.WHITE);
        tg.putString(4, row, backPrefix + "Back to Login");
        tg.setForegroundColor(TuiColor.WHITE);

        // Error line
        if (error != null) {
            int errRow = rows - 2;
            tg.setForegroundColor(TuiColor.RED);
            tg.putString(2, errRow, "! " + error);
            tg.setForegroundColor(TuiColor.WHITE);
            error = null;
        }
        terminal.refresh();
    }

    // ── Server events ─────────────────────────────────────────────────────────

    @Override
    public TUIScreen visit(HomeUpdateEvent e) {
        List<Integer> incoming = e.activeGames();
        this.activeGames = (incoming != null) ? new ArrayList<>(incoming) : new ArrayList<>();
        this.gamePlayers = (e.gamePlayers() != null) ? e.gamePlayers() : Collections.emptyMap();
        this.gameCapacity = (e.gameCapacity() != null) ? e.gameCapacity() : Collections.emptyMap();
        if (cursor >= totalOptions()) {
            cursor = Math.max(0, totalOptions() - 1);
        }
        return this;
    }

    // ── Input events ──────────────────────────────────────────────────────────

    @Override
    public TUIScreen visit(NavigateUpEvent e) {
        int total = totalOptions();
        if (total > 0) cursor = (cursor - 1 + total) % total;
        return this;
    }

    @Override
    public TUIScreen visit(NavigateDownEvent e) {
        int total = totalOptions();
        if (total > 0) cursor = (cursor + 1) % total;
        return this;
    }

    /** Vertical-only menu: lateral arrows mirror up/down. */
    @Override public TUIScreen visit(NavigateLeftEvent e)  { return visit(new NavigateUpEvent()); }
    @Override public TUIScreen visit(NavigateRightEvent e) { return visit(new NavigateDownEvent()); }

    @Override
    public TUIScreen visit(ConfirmEvent e) {
        try {
            if (cursor < CREATE_OPTIONS) {
                int n = cursor + 2;
                appCoordinator.createGameRequest(n);
            } else if (cursor < CREATE_OPTIONS + activeGames.size()) {
                int gameId = activeGames.get(cursor - CREATE_OPTIONS);
                appCoordinator.enterGameRequest(gameId);
            } else {
                appCoordinator.createLogoutRequest();
            }
        } catch (Exception ex) {
            error = "Request failed: " + ex.getMessage();
        }
        return this;
    }

    @Override
    public TUIScreen visit(CharInputEvent e) {
        char ch = Character.toLowerCase(e.getCharacter());
        if (ch == 'b') {
            try {
                appCoordinator.createLogoutRequest();
            } catch (Exception ex) {
                error = ex.getMessage();
            }
        }
        return this;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int totalOptions() {
        return CREATE_OPTIONS + activeGames.size() + 1;
    }

    private boolean isJoinOption(int i) {
        return i >= CREATE_OPTIONS && i < CREATE_OPTIONS + activeGames.size();
    }

    private boolean isGameFull(int gameId) {
        int cap = gameCapacity.getOrDefault(gameId, 0);
        if (cap <= 0) return false;
        return gamePlayers.getOrDefault(gameId, Collections.emptyList()).size() >= cap;
    }

    private String optionLabel(int i) {
        if (i < CREATE_OPTIONS) {
            return "Create new game (" + (i + 2) + " players)";
        }
        if (i < CREATE_OPTIONS + activeGames.size()) {
            int gameId = activeGames.get(i - CREATE_OPTIONS);
            List<String> players = gamePlayers.getOrDefault(gameId, Collections.emptyList());
            int capacity = gameCapacity.getOrDefault(gameId, 0);
            String names = players.isEmpty() ? "empty" : String.join(", ", players);
            String cap = capacity > 0 ? players.size() + "/" + capacity : String.valueOf(players.size());
            return "Game #" + gameId + " [" + cap + "] " + names;
        }
        return "Back to Login";
    }

    private String sectionHeader(String label) {
        return "── " + label;
    }
}
