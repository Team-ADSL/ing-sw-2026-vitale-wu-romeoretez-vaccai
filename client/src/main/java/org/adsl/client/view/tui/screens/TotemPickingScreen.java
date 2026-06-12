package org.adsl.client.view.tui.screens;

import org.adsl.client.AppCoordinator;
import org.adsl.client.serverEvents.ErrorEvent;
import org.adsl.client.serverEvents.TotemAvailableEvent;
import org.adsl.client.view.tui.events.*;
import org.adsl.client.view.tui.render.TuiColor;
import org.adsl.client.view.tui.render.TuiSize;
import org.adsl.client.view.tui.render.TuiTerminal;
import org.adsl.client.view.tui.render.TuiTextGraphics;
import org.adsl.shared.enums.Totem;

import java.io.IOException;
import java.util.List;

/**
 * TUI screen for the totem-picking phase. Displays all available totems in a
 * grid; the player navigates with arrow keys and confirms with ENTER. Each
 * pick sends a {@code TotemPickingRequest} and the screen updates when
 * {@code TotemAvailableEvent} arrives showing the remaining choices.
 */
public class TotemPickingScreen extends TUIScreen {

    private static final Totem[] ALL_TOTEMS = Totem.values();

    private List<Totem> availableTotems;
    private int cursor = 0;
    private Totem selectedTotem = null;
    private boolean pendingPick = false;
    private boolean hasPicked = false;

    /**
     * @param terminal        the TUI terminal used for rendering
     * @param appCoordinator  the coordinator used to send the totem-picking request
     * @param username        the current player's username
     * @param availableTotems totems still free to choose from, or {@code null} if none
     */
    public TotemPickingScreen(TuiTerminal terminal, AppCoordinator appCoordinator,
                              String username, List<Totem> availableTotems) {
        super(terminal, appCoordinator, username);
        this.availableTotems = availableTotems != null ? availableTotems : List.of();
    }

    @Override
    public void render() throws IOException {
        terminal.clear();
        TuiTextGraphics tg = terminal.newTextGraphics();
        TuiSize size = terminal.getTerminalSize();
        int cols = size.getColumns();

        tg.setForegroundColor(TuiColor.BLACK);
        tg.setBackgroundColor(TuiColor.YELLOW);
        String title = "  M E S O S  –  Choose Your Totem  ";
        tg.putString(Math.max(0, (cols - title.length()) / 2), 0, title);

        tg.setForegroundColor(TuiColor.WHITE);
        tg.setBackgroundColor(TuiColor.BLACK);

        int row = 2;
        tg.putString(4, row++, "Player: " + username);
        row++;

        if (hasPicked) {
            tg.setForegroundColor(TuiColor.CYAN);
            tg.putString(4, row++, "Waiting for other players to choose...");
            row++;
            tg.setForegroundColor(TuiColor.WHITE);
            tg.putString(4, row, "You chose: ");
            tg.setForegroundColor(totemColor(selectedTotem));
            tg.putString(15, row, "● " + selectedTotem.name());
            tg.setForegroundColor(TuiColor.WHITE);
        } else {
            tg.putString(4, row++, "Select a totem:  (↑↓ navigate, SPACE select, ENTER confirm)");
            row++;

            for (int i = 0; i < ALL_TOTEMS.length; i++) {
                Totem t = ALL_TOTEMS[i];
                boolean available = availableTotems.contains(t);
                boolean isCursor = (i == cursor);
                boolean isSelected = (t == selectedTotem);

                tg.setForegroundColor(isCursor ? TuiColor.YELLOW : TuiColor.WHITE);
                tg.putString(2, row, isCursor ? " > " : "   ");

                if (isSelected) {
                    tg.setForegroundColor(available ? totemColor(t) : TuiColor.DARK_GRAY);
                    tg.putString(5, row, "✓ ");
                } else if (available) {
                    tg.setForegroundColor(totemColor(t));
                    tg.putString(5, row, "● ");
                } else {
                    tg.setForegroundColor(TuiColor.DARK_GRAY);
                    tg.putString(5, row, "○ ");
                }

                if (available) {
                    tg.setForegroundColor(TuiColor.WHITE);
                    tg.putString(7, row, t.name());
                    if (isSelected) {
                        tg.setForegroundColor(TuiColor.CYAN);
                        tg.putString(7 + t.name().length() + 2, row, "(selected)");
                    }
                } else {
                    tg.setForegroundColor(TuiColor.DARK_GRAY);
                    tg.putString(7, row, t.name() + (isSelected ? "  (selected, taken)" : "  (taken)"));
                }

                tg.setForegroundColor(TuiColor.WHITE);
                row++;
            }

            row++;
            if (pendingPick) {
                tg.setForegroundColor(TuiColor.YELLOW);
                tg.putString(4, row, "Waiting for server confirmation...");
            } else if (selectedTotem != null) {
                tg.setForegroundColor(TuiColor.CYAN);
                tg.putString(4, row, "Press ENTER to confirm selection.");
            } else {
                tg.setForegroundColor(TuiColor.DARK_GRAY);
                tg.putString(4, row, "No totem selected — press SPACE to select.");
            }
            tg.setForegroundColor(TuiColor.WHITE);
        }

        if (error != null) {
            int errRow = size.getRows() - 2;
            tg.setForegroundColor(TuiColor.RED);
            tg.putString(2, errRow, "! " + error);
            tg.setForegroundColor(TuiColor.WHITE);
            error = null;
        }

        terminal.refresh();
    }

    // ── Input events ──────────────────────────────────────────────────────────

    /**
     * Moves the cursor onto the first available totem if it currently sits on
     * a taken one.
     *
     * @return {@code null} always, staying on this screen
     */
    @Override
    public TUIScreen onEnter() {
        ensureCursorOnAvailable(1);
        return null;
    }

    @Override
    public TUIScreen visit(NavigateUpEvent e) {
        if (!hasPicked) moveCursor(-1);
        return this;
    }

    @Override
    public TUIScreen visit(NavigateDownEvent e) {
        if (!hasPicked) moveCursor(1);
        return this;
    }

    @Override public TUIScreen visit(NavigateLeftEvent e)  { return visit(new NavigateUpEvent()); }
    @Override public TUIScreen visit(NavigateRightEvent e) { return visit(new NavigateDownEvent()); }

    @Override
    public TUIScreen visit(SelectEvent e) {
        if (hasPicked) return this;
        Totem t = ALL_TOTEMS[cursor];
        if (!availableTotems.contains(t)) return this;
        selectedTotem = (selectedTotem == t) ? null : t;
        return this;
    }

    private void moveCursor(int dir) {
        int n = ALL_TOTEMS.length;
        int next = (cursor + dir + n) % n;
        for (int i = 0; i < n; i++) {
            if (availableTotems.contains(ALL_TOTEMS[next])) {
                cursor = next;
                return;
            }
            next = (next + dir + n) % n;
        }
    }

    private void ensureCursorOnAvailable(int dir) {
        if (availableTotems.contains(ALL_TOTEMS[cursor])) return;
        moveCursor(dir);
    }

    @Override
    public TUIScreen visit(ConfirmEvent e) {
        if (hasPicked || pendingPick) return this;
        if (selectedTotem == null) {
            error = "Select a totem with SPACE first.";
            return this;
        }
        try {
            appCoordinator.createTotemPickingRequest(selectedTotem);
            pendingPick = true;
        } catch (Exception ex) {
            error = "Request failed: " + ex.getMessage();
        }
        return this;
    }

    // ── Server events ─────────────────────────────────────────────────────────

    @Override
    public TUIScreen visit(TotemAvailableEvent event) {
        List<Totem> newList = event.totemList() != null ? event.totemList() : List.of();
        if (pendingPick && selectedTotem != null && !newList.contains(selectedTotem)) {
            hasPicked = true;
            pendingPick = false;
        }
        this.availableTotems = newList;
        if (!hasPicked) ensureCursorOnAvailable(1);
        return this;
    }

    @Override
    public TUIScreen visit(ErrorEvent e) {
        pendingPick = false;
        error = e.message();
        return this;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private TuiColor totemColor(Totem t) {
        return switch (t) {
            case RED    -> TuiColor.RED;
            case WHITE  -> TuiColor.WHITE;
            case BLACK  -> TuiColor.DARK_PURPLE;
            case BLUE   -> TuiColor.BLUE;
            case YELLOW -> TuiColor.YELLOW;
        };
    }
}
