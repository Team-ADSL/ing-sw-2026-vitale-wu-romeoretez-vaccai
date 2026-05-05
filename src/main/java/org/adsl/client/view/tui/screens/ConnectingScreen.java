package org.adsl.client.view.tui.screens;

import org.adsl.client.AppCoordinator;
import org.adsl.client.view.tui.events.CharInputEvent;
import org.adsl.client.view.tui.events.LoginNeededEvent;
import org.adsl.client.view.tui.render.TuiColor;
import org.adsl.client.view.tui.render.TuiTerminal;
import org.adsl.client.view.tui.render.TuiTextGraphics;

import java.io.IOException;

/**
 * Initial screen shown while the TUI waits for the server to confirm the
 * connection and request login. Transitions to {@link LoginScreen} when
 * a {@link LoginNeededEvent} arrives (handled by {@link Screen}'s default).
 */
public class ConnectingScreen extends Screen {

    public ConnectingScreen(TuiTerminal terminal, AppCoordinator coordinator) {
        super(terminal, coordinator);
    }

    @Override
    public void render() throws IOException {
        terminal.clear();
        TuiTextGraphics tg = terminal.newTextGraphics();
        int rows = terminal.getTerminalSize().getRows();
        int cols = terminal.getTerminalSize().getColumns();
        tg.setForegroundColor(TuiColor.CYAN);
        String msg = "Connecting to server...";
        tg.putString((cols - msg.length()) / 2, rows / 2, msg);
        tg.setForegroundColor(TuiColor.WHITE);
        String hint = "Press [Q] to quit";
        tg.putString((cols - hint.length()) / 2, rows / 2 + 2, hint);
        terminal.refresh();
    }

    @Override
    public Screen visit(CharInputEvent e) {
        if (Character.toLowerCase(e.getCharacter()) == 'q') {
            return ExitScreen.INSTANCE;
        }
        return this;
    }
}
