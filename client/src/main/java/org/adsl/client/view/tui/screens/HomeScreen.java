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
import java.util.List;

/**
 * Non-blocking home menu rendered with TextGraphics. Lets the player create
 * or join a game and stays in sync with {@link HomeUpdateEvent}s as new
 * active games are announced by the server. Transitions to {@link LobbyScreen}
 * on {@link LobbyUpdateEvent}.
 */
public class HomeScreen extends TUIScreen {

    private static final int CREATE_OPTIONS = 4; // 2..5 players

    private List<Integer> activeGames;
    private int cursor = 0;

    public HomeScreen(TuiTerminal terminal,
                      AppCoordinator coordinator,
                      String username,
                      List<Integer> activeGames) {
        super(terminal, coordinator, username);
        this.activeGames = (activeGames != null) ? new ArrayList<>(activeGames) : new ArrayList<>();
    }

    @Override
    public void render() throws IOException {
        terminal.clear();
        TuiTextGraphics tg = terminal.newTextGraphics();
        TuiSize size = terminal.getTerminalSize();
        int cols = size.getColumns();

        tg.setForegroundColor(TuiColor.BLACK);
        tg.setBackgroundColor(TuiColor.YELLOW);
        String title = "  M E S O S  –  Home  ";
        tg.putString(Math.max(0, (cols - title.length()) / 2), 0, title);

        tg.setForegroundColor(TuiColor.WHITE);
        tg.setBackgroundColor(TuiColor.BLACK);

        int row = 2;
        tg.putString(4, row++, "Welcome, " + username + "!");
        row++;
        tg.putString(4, row++, "Choose an option:  (← → / ↑ ↓ to navigate, ENTER to confirm)");
        row++;

        int totalOptions = totalOptions();
        for (int i = 0; i < totalOptions; i++) {
            String prefix = (i == cursor) ? " > " : "   ";
            tg.setForegroundColor(i == cursor ? TuiColor.YELLOW : TuiColor.WHITE);
            tg.putString(4, row++, prefix + optionLabel(i));
        }
        tg.setForegroundColor(TuiColor.WHITE);

        row++;
        tg.setForegroundColor(TuiColor.CYAN);
        tg.putString(4, row, activeGames.isEmpty()
                ? "(no active games available to join)"
                : "Active games: " + activeGames.size());
        tg.setForegroundColor(TuiColor.WHITE);

        if (error != null) {
            int errRow = size.getRows() - 2;
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
                // Server will reply with LoginNeededEvent which routes to LoginScreen.
            }
        } catch (Exception ex) {
            error = "Request failed: " + ex.getMessage(); // TODO: see error handling
        }
        return this;
    }

    @Override
    public TUIScreen visit(CharInputEvent e) {
        char ch = Character.toLowerCase(e.getCharacter());
        if (ch == 'b') {
            try {
                appCoordinator.createLogoutRequest();
                // Server will reply with LoginNeededEvent which routes to LoginScreen.
            } catch (Exception ex) {
                error = ex.getMessage();
            }
        }
        return this;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int totalOptions() {
        return CREATE_OPTIONS + activeGames.size() + 1; // create variants + joins + back
    }

    private String optionLabel(int i) {
        if (i < CREATE_OPTIONS) {
            return "Create new game (" + (i + 2) + " players)";
        }
        if (i < CREATE_OPTIONS + activeGames.size()) {
            return "Join Game #" + activeGames.get(i - CREATE_OPTIONS);
        }
        return "Back to Login";
    }
}
