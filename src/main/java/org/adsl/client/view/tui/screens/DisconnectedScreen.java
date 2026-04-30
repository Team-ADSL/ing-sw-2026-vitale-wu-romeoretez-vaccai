package org.adsl.client.view.tui.screens;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import org.adsl.client.AppCoordinator;

import java.io.IOException;
import java.util.List;

/**
 * Shown whenever the server connection is lost. Presents the error message and
 * two actions:
 * <ul>
 *   <li><b>Reconnect</b> – calls {@link AppCoordinator#connectRequest()} and
 *       transitions to {@link ConnectingScreen}.</li>
 *   <li><b>Exit</b> – transitions to {@link ExitScreen} to close the application.</li>
 * </ul>
 *
 * {@link #onEnter()} blocks until the user makes a choice and returns the
 * appropriate next {@link Screen} immediately, so the TUI loop never renders
 * this screen in a waiting state.
 */
public class DisconnectedScreen implements Screen {

    private final com.googlecode.lanterna.screen.Screen terminal;
    private final WindowBasedTextGUI gui;
    private final AppCoordinator coordinator;
    private final String message;

    public DisconnectedScreen(com.googlecode.lanterna.screen.Screen terminal,
                              WindowBasedTextGUI gui,
                              AppCoordinator coordinator,
                              String message) {
        this.terminal    = terminal;
        this.gui         = gui;
        this.coordinator = coordinator;
        this.message     = message;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public Screen onEnter() {
        return showDialog();
    }

    @Override
    public boolean isToRender() {
        return true;
    }

    @Override
    public void render() throws IOException {
        terminal.clear();
        TextGraphics tg = terminal.newTextGraphics();
        int rows = terminal.getTerminalSize().getRows();
        int cols = terminal.getTerminalSize().getColumns();
        tg.setForegroundColor(TextColor.ANSI.RED);
        String msg = "Connection lost. Select an option…";
        tg.putString(Math.max(0, (cols - msg.length()) / 2), rows / 2, msg);
        tg.setForegroundColor(TextColor.ANSI.WHITE);
        terminal.refresh();
    }

    // ── Dialog ────────────────────────────────────────────────────────────────

    private Screen showDialog() {
        final Screen[] result = {null};

        while (result[0] == null) {
            BasicWindow window = new BasicWindow("MESOS – Disconnected");
            window.setHints(List.of(Window.Hint.CENTERED));

            Panel root = new Panel(new LinearLayout(Direction.VERTICAL));
            root.addComponent(new Label(""));
            root.addComponent(new Label("  Connection to the server was lost."));
            if (message != null && !message.isBlank()) {
                root.addComponent(new Label("  " + message));
            }
            root.addComponent(new Label(""));

            root.addComponent(new Button("  Reconnect  ", () -> {
                try {
                    coordinator.connectRequest();
                    result[0] = new ConnectingScreen(terminal, gui, coordinator);
                } catch (Exception e) {
                    MessageDialog.showMessageDialog(gui, "Error",
                            "Could not reconnect: " + e.getMessage(),
                            MessageDialogButton.OK);
                }
                window.close();
            }));

            root.addComponent(new Label(""));

            root.addComponent(new Button("  Exit Application  ", () -> {
                result[0] = ExitScreen.INSTANCE;
                window.close();
            }));

            root.addComponent(new Label(""));
            window.setComponent(root);
            gui.addWindowAndWait(window);
        }

        return result[0];
    }
}
