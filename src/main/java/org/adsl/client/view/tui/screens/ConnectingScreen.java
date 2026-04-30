package org.adsl.client.view.tui.screens;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import org.adsl.client.AppCoordinator;
import org.adsl.client.view.tui.events.CharInputEvent;
import org.adsl.client.view.tui.events.LoginNeededEvent;
import org.adsl.client.view.tui.events.HomeUpdateEvent;

import com.googlecode.lanterna.gui2.WindowBasedTextGUI;

import java.io.IOException;

/**
 * Initial screen shown while the TUI waits for the server to confirm the
 * connection and request login. Transitions to {@link LoginScreen} when
 * a {@link LoginNeededEvent} arrives.
 */
public class ConnectingScreen implements Screen {

    private final com.googlecode.lanterna.screen.Screen terminal;
    private final WindowBasedTextGUI gui;
    private final AppCoordinator coordinator;

    public ConnectingScreen(com.googlecode.lanterna.screen.Screen terminal,
                            WindowBasedTextGUI gui,
                            AppCoordinator coordinator) {
        this.terminal = terminal;
        this.gui = gui;
        this.coordinator = coordinator;
    }

    @Override
    public void render() throws IOException {
        terminal.clear();
        TextGraphics tg = terminal.newTextGraphics();
        int rows = terminal.getTerminalSize().getRows();
        int cols = terminal.getTerminalSize().getColumns();
        tg.setForegroundColor(TextColor.ANSI.CYAN);
        String msg = "Connecting to server...";
        tg.putString((cols - msg.length()) / 2, rows / 2, msg);
        tg.setForegroundColor(TextColor.ANSI.WHITE);
        String hint = "Press [Q] to quit";
        tg.putString((cols - hint.length()) / 2, rows / 2 + 2, hint);
        terminal.refresh();
    }

    @Override
    public boolean isToRender() {
        return true;
    }

    @Override
    public Screen visit(CharInputEvent e) {
        if (Character.toLowerCase(e.getCharacter()) == 'q') {
            return ExitScreen.INSTANCE;
        }
        return this;
    }

    @Override
    public Screen visit(LoginNeededEvent e) {
        return new LoginScreen(terminal, gui, coordinator);
    }

    @Override
    public Screen visit(HomeUpdateEvent e) {
        // Server sent HomeUpdate before LoginNeeded (shouldn't happen, but handle it)
        return new LoginScreen(terminal, gui, coordinator).visit(e);
    }
}
