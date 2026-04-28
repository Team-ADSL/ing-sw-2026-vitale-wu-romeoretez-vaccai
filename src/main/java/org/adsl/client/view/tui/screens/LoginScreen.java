package org.adsl.client.view.tui.screens;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import org.adsl.client.AppCoordinator;
import org.adsl.client.view.tui.events.ErrorEvent;
import org.adsl.client.view.tui.events.HomeUpdateEvent;

import java.io.IOException;
import java.util.List;

/**
 * Collects the player's username via a Lanterna GUI dialog (blocking in
 * {@link #onEnter()}), then sends the login request and waits for
 * {@link HomeUpdateEvent}, transitioning to {@link HomeScreen}.
 */
public class LoginScreen implements Screen {

    private final com.googlecode.lanterna.screen.Screen terminal;
    private final WindowBasedTextGUI gui;
    private final AppCoordinator coordinator;
    private String username;
    private boolean exitRequested = false;
    private final String initialError;

    public LoginScreen(com.googlecode.lanterna.screen.Screen terminal,
                       WindowBasedTextGUI gui,
                       AppCoordinator coordinator) {
        this(terminal, gui, coordinator, null);
    }

    public LoginScreen(com.googlecode.lanterna.screen.Screen terminal,
                       WindowBasedTextGUI gui,
                       AppCoordinator coordinator,
                       String initialError) {
        this.terminal = terminal;
        this.gui = gui;
        this.coordinator = coordinator;
        this.initialError = initialError;
    }

    @Override
    public Screen onEnter() throws Exception {
        username = collectUsername();
        if (exitRequested) return ExitScreen.INSTANCE;
        coordinator.createLoginRequest(username);
        return null;
    }

    @Override
    public void render() throws IOException {
        terminal.clear();
        TextGraphics tg = terminal.newTextGraphics();
        int rows = terminal.getTerminalSize().getRows();
        int cols = terminal.getTerminalSize().getColumns();
        tg.setForegroundColor(TextColor.ANSI.CYAN);
        String msg = "Logged in as \"" + username + "\". Waiting for server...";
        tg.putString(Math.max(0, (cols - msg.length()) / 2), rows / 2, msg);
        tg.setForegroundColor(TextColor.ANSI.WHITE);
        terminal.refresh();
    }

    @Override
    public Screen visit(HomeUpdateEvent e) {
        return new HomeScreen(terminal, gui, coordinator, username, e.getActiveGames());
    }

    @Override
    public Screen visit(ErrorEvent e) {
        return new LoginScreen(terminal, gui, coordinator, e.getMessage());
    }

    private String collectUsername() {
        final String[] result = {null};
        while (!exitRequested && (result[0] == null || result[0].isBlank())) {
            BasicWindow window = new BasicWindow("MESOS – Login");
            window.setHints(List.of(Window.Hint.CENTERED));

            Panel root = new Panel(new LinearLayout(Direction.VERTICAL));
            root.addComponent(new Label(""));
            root.addComponent(new Label("  MESOS  –  Ancient Tribe Strategy  "));
            root.addComponent(new Label(""));
            if (initialError != null && !initialError.isBlank()) {
                Label errorLabel = new Label("  " + initialError);
                errorLabel.setForegroundColor(com.googlecode.lanterna.TextColor.ANSI.RED);
                root.addComponent(errorLabel);
                root.addComponent(new Label(""));
            }
            root.addComponent(new Label("  Enter your username:"));

            TextBox tb = new TextBox(new com.googlecode.lanterna.TerminalSize(20, 1));
            root.addComponent(tb);
            root.addComponent(new Label(""));

            root.addComponent(new Button("  Login  ", () -> {
                String name = tb.getText().trim();
                if (name.isBlank()) {
                    MessageDialog.showMessageDialog(gui, "Invalid", "Username cannot be empty.", MessageDialogButton.OK);
                    return;
                }
                if (name.length() > 20) {
                    MessageDialog.showMessageDialog(gui, "Invalid", "Username must be 20 characters or fewer.", MessageDialogButton.OK);
                    return;
                }
                result[0] = name;
                window.close();
            }));
            root.addComponent(new Label(""));

            root.addComponent(new Button("  Close Application  ", () -> {
                exitRequested = true;
                window.close();
            }));
            root.addComponent(new Label(""));

            window.setComponent(root);
            gui.addWindowAndWait(window);
        }
        return result[0];
    }
}
