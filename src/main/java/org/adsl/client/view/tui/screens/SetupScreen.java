package org.adsl.client.view.tui.screens;

import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;

import java.util.List;

/**
 * Pre-game dialogs driven by the Lanterna GUI layer, used in network mode:
 *   1. askUsername() — shown on LoginNeeded, returns the username typed by the user
 *   2. showHome(activeGames) — shown on HomeUpdate, returns the user's choice:
 *        either CREATE (with a player count 2-5) or JOIN (with a gameId)
 *
 * These dialogs never talk to the network directly. The TUI collects the user's
 * choice and forwards it through AppCoordinator.
 */
public class SetupScreen {
    private final WindowBasedTextGUI gui;

    public SetupScreen(WindowBasedTextGUI gui) {
        this.gui = gui;
    }

    /** Result of the home dialog: either create a new game or join an existing one. */
    public record HomeChoice(Kind kind, int value) {
        public enum Kind { CREATE, JOIN }
        public static HomeChoice create(int numPlayers) { return new HomeChoice(Kind.CREATE, numPlayers); }
        public static HomeChoice join(int gameId)       { return new HomeChoice(Kind.JOIN, gameId); }
    }

    // ── Username entry ────────────────────────────────────────────────────────

    public String askUsername() {
        final String[] result = {null};

        while (result[0] == null || result[0].isBlank()) {
            BasicWindow window = new BasicWindow("MESOS – Login");
            window.setHints(List.of(Window.Hint.CENTERED));

            Panel root = new Panel(new LinearLayout(Direction.VERTICAL));
            root.addComponent(new Label(""));
            root.addComponent(new Label("  MESOS  –  Ancient Tribe Strategy  "));
            root.addComponent(new Label(""));
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

            window.setComponent(root);
            gui.addWindowAndWait(window);
        }
        return result[0];
    }

    // ── Home screen (create or join) ──────────────────────────────────────────

    public HomeChoice showHome(List<Integer> activeGames) {
        final HomeChoice[] result = {null};

        BasicWindow window = new BasicWindow("MESOS – Home");
        window.setHints(List.of(Window.Hint.CENTERED));

        Panel root = new Panel(new LinearLayout(Direction.VERTICAL));
        root.addComponent(new Label(""));
        root.addComponent(new Label("  Choose an option:"));
        root.addComponent(new Label(""));

        // Create new game
        root.addComponent(new Label("  Create new game — select number of players:"));
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

        // Join existing game
        if (activeGames != null && !activeGames.isEmpty()) {
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
}
