package org.adsl.client.view.tui.screens;

import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles the two setup phases before a local game:
 *   1. Player count selection (2–5) via radio buttons
 *   2. Name entry for each player with duplicate-name validation
 */
public class SetupScreen {
    private final WindowBasedTextGUI gui;

    public SetupScreen(WindowBasedTextGUI gui) {
        this.gui = gui;
    }

    // ── Phase 1: Choose number of players ────────────────────────────────────

    public int selectPlayerCount() {
        final int[] result = {2};

        BasicWindow window = new BasicWindow("MESOS – Player Setup");
        window.setHints(List.of(Window.Hint.CENTERED));

        Panel root = new Panel(new LinearLayout(Direction.VERTICAL));

        root.addComponent(new Label(""));
        root.addComponent(new Label("  MESOS  –  Ancient Tribe Strategy  "));
        root.addComponent(new Label(""));
        root.addComponent(new Label("  How many players?"));
        root.addComponent(new Label(""));

        RadioBoxList<String> radioBox = new RadioBoxList<>();
        for (int n = 2; n <= 5; n++) {
            radioBox.addItem(n + " players");
        }
        radioBox.setCheckedItemIndex(0);
        root.addComponent(radioBox);

        root.addComponent(new Label(""));

        root.addComponent(new Button("  Confirm  ", () -> {
            result[0] = radioBox.getCheckedItemIndex() + 2;
            window.close();
        }));

        root.addComponent(new Label(""));

        window.setComponent(root);
        gui.addWindowAndWait(window);

        return result[0];
    }

    // ── Phase 2: Enter player names ───────────────────────────────────────────

    public List<String> enterPlayerNames(int numPlayers) {
        while (true) {
            List<String> names = showNameEntryDialog(numPlayers);
            if (names != null) return names;
        }
    }

    private List<String> showNameEntryDialog(int numPlayers) {
        final List<String>[] result = new List[]{null};

        BasicWindow window = new BasicWindow("MESOS – Enter Names");
        window.setHints(List.of(Window.Hint.CENTERED));

        Panel root = new Panel(new LinearLayout(Direction.VERTICAL));
        root.addComponent(new Label(""));
        root.addComponent(new Label("  Enter a name for each player:"));
        root.addComponent(new Label(""));

        List<TextBox> textBoxes = new ArrayList<>();
        for (int i = 1; i <= numPlayers; i++) {
            Panel row = new Panel(new LinearLayout(Direction.HORIZONTAL));
            row.addComponent(new Label("  Player " + i + ":  "));
            TextBox tb = new TextBox(new com.googlecode.lanterna.TerminalSize(20, 1));
            textBoxes.add(tb);
            row.addComponent(tb);
            root.addComponent(row);
        }

        root.addComponent(new Label(""));

        Panel buttons = new Panel(new LinearLayout(Direction.HORIZONTAL));
        buttons.addComponent(new Button("  Start Game  ", () -> {
            List<String> names = new ArrayList<>();
            for (TextBox tb : textBoxes) {
                names.add(tb.getText().trim());
            }

            String error = validateNames(names, numPlayers);
            if (error != null) {
                MessageDialog.showMessageDialog(gui, "Invalid Names", error, MessageDialogButton.OK);
                return;
            }

            result[0] = names;
            window.close();
        }));
        buttons.addComponent(new EmptySpace());
        buttons.addComponent(new Button("  Back  ", window::close));

        root.addComponent(buttons);
        root.addComponent(new Label(""));

        window.setComponent(root);
        gui.addWindowAndWait(window);

        return result[0];
    }

    private String validateNames(List<String> names, int expected) {
        if (names.size() != expected) {
            return "Expected " + expected + " names.";
        }
        for (String name : names) {
            if (name.isBlank()) {
                return "All player names must be non-empty.";
            }
            if (name.length() > 20) {
                return "Names must be 20 characters or fewer.";
            }
        }
        Set<String> seen = new HashSet<>();
        for (String name : names) {
            String lower = name.toLowerCase();
            if (!seen.add(lower)) {
                return "Duplicate name: \"" + name + "\". Each player must have a unique name.";
            }
        }
        return null; // valid
    }
}
