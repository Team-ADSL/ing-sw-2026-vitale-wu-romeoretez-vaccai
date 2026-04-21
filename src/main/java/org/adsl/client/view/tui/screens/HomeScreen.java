package org.adsl.client.view.tui.screens;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import org.adsl.client.AppCoordinator;
import org.adsl.client.view.tui.events.LobbyUpdateEvent;

import java.io.IOException;
import java.util.List;

/**
 * Lets the player create or join a game via a Lanterna GUI dialog (blocking in
 * {@link #onEnter()}). Stores the chosen player count for the lobby display.
 * Transitions to {@link LobbyScreen} when {@link LobbyUpdateEvent} arrives.
 */
public class HomeScreen implements Screen {

    private final com.googlecode.lanterna.screen.Screen terminal;
    private final WindowBasedTextGUI gui;
    private final AppCoordinator coordinator;
    private final String username;
    private final List<Integer> activeGames;
    private int totalPlayers = -1;

    public HomeScreen(com.googlecode.lanterna.screen.Screen terminal,
                      WindowBasedTextGUI gui,
                      AppCoordinator coordinator,
                      String username,
                      List<Integer> activeGames) {
        this.terminal = terminal;
        this.gui = gui;
        this.coordinator = coordinator;
        this.username = username;
        this.activeGames = activeGames != null ? activeGames : List.of();
    }

    @Override
    public void onEnter() throws Exception {
        HomeChoice choice = showHomeDialog();
        if (choice.kind() == HomeChoice.Kind.CREATE) {
            totalPlayers = choice.value();
            coordinator.createGameRequest(choice.value());
        } else {
            coordinator.enterGameRequest(choice.value());
        }
    }

    @Override
    public void render() throws IOException {
        terminal.clear();
        TextGraphics tg = terminal.newTextGraphics();
        int rows = terminal.getTerminalSize().getRows();
        int cols = terminal.getTerminalSize().getColumns();
        tg.setForegroundColor(TextColor.ANSI.CYAN);
        String msg = "Waiting for lobby...";
        tg.putString(Math.max(0, (cols - msg.length()) / 2), rows / 2, msg);
        tg.setForegroundColor(TextColor.ANSI.WHITE);
        terminal.refresh();
    }

    @Override
    public Screen visit(LobbyUpdateEvent e) {
        return new LobbyScreen(terminal, coordinator, username, e.getPlayers(), totalPlayers);
    }

    private HomeChoice showHomeDialog() {
        final HomeChoice[] result = {null};

        BasicWindow window = new BasicWindow("MESOS – Home");
        window.setHints(List.of(Window.Hint.CENTERED));

        Panel root = new Panel(new LinearLayout(Direction.VERTICAL));
        root.addComponent(new Label(""));
        root.addComponent(new Label("  Welcome, " + username + "!  Choose an option:"));
        root.addComponent(new Label(""));

        root.addComponent(new Label("  Create new game — number of players:"));
        RadioBoxList<String> playerCount = new RadioBoxList<>();
        for (int n = 2; n <= 5; n++) playerCount.addItem(n + " players");
        playerCount.setCheckedItemIndex(0);
        root.addComponent(playerCount);
        root.addComponent(new Button("  Create Game  ", () -> {
            result[0] = HomeChoice.create(playerCount.getCheckedItemIndex() + 2);
            window.close();
        }));

        root.addComponent(new Label(""));
        root.addComponent(new Separator(Direction.HORIZONTAL));
        root.addComponent(new Label(""));

        if (!activeGames.isEmpty()) {
            root.addComponent(new Label("  Join an active game:"));
            RadioBoxList<String> gameList = new RadioBoxList<>();
            for (Integer gid : activeGames) gameList.addItem("Game #" + gid);
            gameList.setCheckedItemIndex(0);
            root.addComponent(gameList);
            root.addComponent(new Button("  Join Game  ", () -> {
                int idx = gameList.getCheckedItemIndex();
                if (idx < 0) {
                    MessageDialog.showMessageDialog(gui, "Invalid", "Select a game first.", MessageDialogButton.OK);
                    return;
                }
                result[0] = HomeChoice.join(activeGames.get(idx));
                window.close();
            }));
        } else {
            root.addComponent(new Label("  (no active games available to join)"));
        }
        root.addComponent(new Label(""));

        window.setComponent(root);
        gui.addWindowAndWait(window);

        return result[0];
    }

    public record HomeChoice(Kind kind, int value) {
        public enum Kind { CREATE, JOIN }
        public static HomeChoice create(int numPlayers) { return new HomeChoice(Kind.CREATE, numPlayers); }
        public static HomeChoice join(int gameId)       { return new HomeChoice(Kind.JOIN, gameId); }
    }
}
