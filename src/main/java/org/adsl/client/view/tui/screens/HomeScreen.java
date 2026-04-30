package org.adsl.client.view.tui.screens;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import org.adsl.client.AppCoordinator;
import org.adsl.client.view.tui.events.ConfirmEvent;
import org.adsl.client.view.tui.events.ErrorEvent;
import org.adsl.client.view.tui.events.HomeUpdateEvent;
import org.adsl.client.view.tui.events.LobbyUpdateEvent;
import org.adsl.client.view.tui.events.NavigateDownEvent;
import org.adsl.client.view.tui.events.NavigateUpEvent;
import org.adsl.client.view.tui.events.CharInputEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Non-blocking home menu rendered with TextGraphics. Lets the player create
 * or join a game and stays in sync with {@link HomeUpdateEvent}s as new
 * active games are announced by the server. Transitions to {@link LobbyScreen}
 * on {@link LobbyUpdateEvent}.
 */
public class HomeScreen implements Screen {

    private static final int CREATE_OPTIONS = 4; // 2..5 players

    private final com.googlecode.lanterna.screen.Screen terminal;
    private final WindowBasedTextGUI gui;
    private final AppCoordinator coordinator;
    private final String username;

    private List<Integer> activeGames;
    private int totalPlayers = -1;
    private boolean toRender;
    private int cursor = 0;
    private String error = null;

    public HomeScreen(com.googlecode.lanterna.screen.Screen terminal,
                      WindowBasedTextGUI gui,
                      AppCoordinator coordinator,
                      String username,
                      List<Integer> activeGames) {
        this.terminal = terminal;
        this.gui = gui;
        this.coordinator = coordinator;
        this.username = username;
        this.activeGames = (activeGames != null) ? new ArrayList<>(activeGames) : new ArrayList<>();
        this.toRender = true;
        this.error = null;
    }

    @Override
    public void render() throws IOException {
        terminal.clear();
        TextGraphics tg = terminal.newTextGraphics();
        TerminalSize size = terminal.getTerminalSize();
        int cols = size.getColumns();

        tg.setForegroundColor(TextColor.ANSI.BLACK);
        tg.setBackgroundColor(TextColor.ANSI.YELLOW);
        String title = "  M E S O S  –  Home  ";
        tg.putString(Math.max(0, (cols - title.length()) / 2), 0, title);

        tg.setForegroundColor(TextColor.ANSI.WHITE);
        tg.setBackgroundColor(TextColor.ANSI.BLACK);

        int row = 2;
        tg.putString(4, row++, "Welcome, " + username + "!");
        row++;
        tg.putString(4, row++, "Choose an option:  (↑ ↓ to navigate, ENTER to confirm)");
        row++;

        int totalOptions = totalOptions();
        for (int i = 0; i < totalOptions; i++) {
            String prefix = (i == cursor) ? " > " : "   ";
            tg.setForegroundColor(i == cursor ? TextColor.ANSI.YELLOW : TextColor.ANSI.WHITE);
            tg.putString(4, row++, prefix + optionLabel(i));
        }
        tg.setForegroundColor(TextColor.ANSI.WHITE);

        row++;
        tg.setForegroundColor(TextColor.ANSI.CYAN);
        tg.putString(4, row, activeGames.isEmpty()
                ? "(no active games available to join)"
                : "Active games: " + activeGames.size());
        tg.setForegroundColor(TextColor.ANSI.WHITE);

        if (error != null) {
            int errRow = size.getRows() - 2;
            tg.setForegroundColor(TextColor.ANSI.RED);
            tg.putString(2, errRow, "! " + error);
            tg.setForegroundColor(TextColor.ANSI.WHITE);
            error = null;
        }
        terminal.refresh();
    }

    @Override
    public boolean isToRender() {
        return toRender;
    }

    @Override
    public void setToRender(boolean value) {
        toRender = value;
    }

    // ── Server events ─────────────────────────────────────────────────────────

    @Override
    public Screen visit(HomeUpdateEvent e) {
        List<Integer> incoming = e.getActiveGames();
        this.activeGames = (incoming != null) ? new ArrayList<>(incoming) : new ArrayList<>();
        if (cursor >= totalOptions()) {
            cursor = Math.max(0, totalOptions() - 1);
        }
        toRender = true;
        return this;
    }

    @Override
    public Screen visit(LobbyUpdateEvent e) {
        return new LobbyScreen(terminal, gui, coordinator, username, e.getPlayers(), totalPlayers);
    }

    @Override
    public Screen visit(ErrorEvent e) {
        error = e.getMessage();
        toRender = true;
        return this;
    }

    // ── Input events ──────────────────────────────────────────────────────────

    @Override
    public Screen visit(NavigateUpEvent e) {
        int total = totalOptions();
        if (total > 0) cursor = (cursor - 1 + total) % total;
        toRender = true;
        return this;
    }

    @Override
    public Screen visit(NavigateDownEvent e) {
        int total = totalOptions();
        if (total > 0) cursor = (cursor + 1) % total;
        toRender = true;
        return this;
    }

    @Override
    public Screen visit(ConfirmEvent e) {
        try {
            if (cursor < CREATE_OPTIONS) {
                int n = cursor + 2;
                totalPlayers = n;
                coordinator.createGameRequest(n);
            } else if (cursor < CREATE_OPTIONS + activeGames.size()) {
                int gameId = activeGames.get(cursor - CREATE_OPTIONS);
                coordinator.enterGameRequest(gameId);
            } else {
                return new LoginScreen(terminal, gui, coordinator);
            }
        } catch (Exception ex) {
            error = "Request failed: " + ex.getMessage(); // TODO: see error handling
            toRender = true;
        }
        return this;
    }

    @Override
    public Screen visit(CharInputEvent e) {
        char ch = Character.toLowerCase(e.getCharacter());
        if (ch == 'b') {
            try {
                coordinator.createLogoutRequest();
                return new LoginScreen(terminal, gui, coordinator);
            } catch (Exception ex) {
                error = ex.getMessage();
                toRender = true;
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
