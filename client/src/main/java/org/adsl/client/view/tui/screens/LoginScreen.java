package org.adsl.client.view.tui.screens;

import org.adsl.client.AppCoordinator;
import org.adsl.client.view.tui.events.BackspaceEvent;
import org.adsl.client.view.tui.events.CharInputEvent;
import org.adsl.client.view.tui.events.ConfirmEvent;
import org.adsl.client.serverEvents.ErrorEvent;
import org.adsl.client.serverEvents.HomeUpdateEvent;
import org.adsl.client.serverEvents.LoginNeededEvent;
import org.adsl.client.view.tui.events.NavigateDownEvent;
import org.adsl.client.view.tui.events.NavigateLeftEvent;
import org.adsl.client.view.tui.events.NavigateRightEvent;
import org.adsl.client.view.tui.events.NavigateUpEvent;
import org.adsl.client.view.tui.render.TuiColor;
import org.adsl.client.view.tui.render.TuiSize;
import org.adsl.client.view.tui.render.TuiTerminal;
import org.adsl.client.view.tui.render.TuiTextGraphics;

import java.io.IOException;

/**
 * Collects the player's username inline in the TUI loop. The original
 * Lanterna implementation used a blocking widget dialog; that is replaced
 * here by manual ANSI rendering plus arrow-key focus management:
 * <ul>
 *   <li>Field 0 — text input that captures characters and backspace.</li>
 *   <li>Field 1 — Login button. Activated with ENTER while focused; also
 *       activated by ENTER on the text field as a convenience shortcut.</li>
 *   <li>Field 2 — Close Application button. Returns {@link ExitScreen}.</li>
 * </ul>
 *
 * Up/Down arrows cycle focus between fields. Once the username is submitted
 * the screen flips to a "waiting for server" state and the default visitors
 * route the eventual {@link HomeUpdateEvent} to the home screen.
 */
public class LoginScreen extends TUIScreen {

    private static final int MAX_USERNAME_LENGTH = 30;

    private static final int FIELD_TEXT  = 0;
    private static final int FIELD_LOGIN = 1;
    private static final int FIELD_EXIT  = 2;
    private static final int FIELD_COUNT = 3;

    private final String initialError;

    private final StringBuilder typed = new StringBuilder();
    private int focus = FIELD_TEXT;
    private boolean submitted = false;
    private String localValidation;

    /**
     * @param terminal    the TUI terminal used for rendering
     * @param coordinator the coordinator used to send the login request
     */
    public LoginScreen(TuiTerminal terminal, AppCoordinator coordinator) {
        this(terminal, coordinator, null);
    }

    /**
     * @param terminal    the TUI terminal used for rendering
     * @param coordinator the coordinator used to send the login request
     * @param initialError error message to show on first render (e.g. after a failed
     *                      login attempt), or {@code null} if none
     */
    public LoginScreen(TuiTerminal terminal,
                       AppCoordinator coordinator,
                       String initialError) {
        super(terminal, coordinator);
        this.initialError = initialError;
    }

    @Override
    public void render() throws IOException {
        terminal.clear();
        TuiTextGraphics tg = terminal.newTextGraphics();
        TuiSize size = terminal.getTerminalSize();
        int cols = size.getColumns();
        int rows = size.getRows();

        tg.setForegroundColor(TuiColor.WHITE);
        tg.setBackgroundColor(TuiColor.BLACK);

        if (submitted) {
            tg.setForegroundColor(TuiColor.CYAN);
            String msg = "Logged in as \"" + username + "\". Waiting for server...";
            tg.putString(Math.max(0, (cols - msg.length()) / 2), rows / 2, msg);
            tg.setForegroundColor(TuiColor.WHITE);
            terminal.refresh();
            return;
        }

        int boxWidth  = 44;
        int boxHeight = 14;
        int x0 = Math.max(0, (cols - boxWidth) / 2);
        int y0 = Math.max(0, (rows - boxHeight) / 2);

        drawBox(tg, x0, y0, boxWidth, boxHeight, " MESOS LOGIN ");

        int innerX = x0 + 2;
        int row = y0 + 2;

        tg.setForegroundColor(TuiColor.YELLOW);
        String subtitle = "MESOS  –  Ancient Tribe Strategy";
        tg.putString(x0 + Math.max(1, (boxWidth - subtitle.length()) / 2), row++, subtitle);
        tg.setForegroundColor(TuiColor.WHITE);
        row++;

        String activeError = (localValidation != null) ? localValidation
                : (initialError != null && !initialError.isBlank()) ? initialError
                : null;
        if (activeError != null) {
            tg.setForegroundColor(TuiColor.RED);
            tg.putString(innerX, row++, truncate(activeError, boxWidth - 4));
            tg.setForegroundColor(TuiColor.WHITE);
            row++;
        } else {
            row++;
        }

        String promptMsg = "Enter your username:";
        tg.putString(x0 + Math.max(1, (boxWidth - promptMsg.length()) / 2), row++, promptMsg);

        // Text input field: drawn as [............] with the typed text.
        int fieldWidth = MAX_USERNAME_LENGTH + 2;
        if (focus == FIELD_TEXT) {
            tg.setForegroundColor(TuiColor.YELLOW);
        }
        String fieldStr = "[" + padField(typed.toString(), MAX_USERNAME_LENGTH) + "]";
        tg.putString(x0 + Math.max(1, (boxWidth - fieldWidth) / 2), row, fieldStr);
        tg.setForegroundColor(TuiColor.WHITE);
        row += 2;

        int buttonsWidth = 9 + 2 + 21;
        int buttonsX = x0 + Math.max(1, (boxWidth - buttonsWidth) / 2);
        drawButton(tg, buttonsX,                   row, "  Login  ",            focus == FIELD_LOGIN);
        drawButton(tg, buttonsX + 11,              row, "  Close Application  ", focus == FIELD_EXIT);

        row = y0 + boxHeight - 2;
        tg.setForegroundColor(TuiColor.CYAN);
        tg.putString(innerX, row, "← → / ↑ ↓ to navigate · ENTER to confirm");
        tg.setForegroundColor(TuiColor.WHITE);

        terminal.refresh();
    }

    // ── Server events ─────────────────────────────────────────────────────────

    /** Already in the login flow: a redundant LoginNeeded must not reset state. */
    @Override
    public TUIScreen visit(LoginNeededEvent e) {
        return this;
    }

    @Override
    public TUIScreen visit(ErrorEvent e) {
        // Restart the form carrying the server error so the user can fix and retry.
        return new LoginScreen(terminal, appCoordinator, e.message());
    }

    // ── Input events ──────────────────────────────────────────────────────────

    @Override
    public TUIScreen visit(NavigateUpEvent e) {
        if (submitted) return this;
        focus = (focus - 1 + FIELD_COUNT) % FIELD_COUNT;
        return this;
    }

    @Override
    public TUIScreen visit(NavigateDownEvent e) {
        if (submitted) return this;
        focus = (focus + 1) % FIELD_COUNT;
        return this;
    }

    /** Vertical-only menu: lateral arrows mirror up/down. */
    @Override public TUIScreen visit(NavigateLeftEvent e)  { return visit(new NavigateUpEvent()); }
    @Override public TUIScreen visit(NavigateRightEvent e) { return visit(new NavigateDownEvent()); }

    @Override
    public TUIScreen visit(ConfirmEvent e) {
        if (submitted) return this;
        if (focus == FIELD_EXIT) {
            return ExitScreen.INSTANCE;
        }
        // FIELD_TEXT and FIELD_LOGIN both attempt submission.
        return trySubmit();
    }

    @Override
    public TUIScreen visit(CharInputEvent e) {
        if (submitted || focus != FIELD_TEXT) return this;
        if (typed.length() >= MAX_USERNAME_LENGTH) return this;
        char c = e.getCharacter();
        if (c < 0x20) return this;          // ignore non-printable
        typed.append(c);
        localValidation = null;
        return this;
    }

    @Override
    public TUIScreen visit(BackspaceEvent e) {
        if (submitted || focus != FIELD_TEXT) return this;
        if (typed.length() > 0) {
            typed.deleteCharAt(typed.length() - 1);
            localValidation = null;
        }
        return this;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private TUIScreen trySubmit() {
        String name = typed.toString().trim();
        if (name.isBlank()) {
            localValidation = "Username cannot be empty.";
            return this;
        }
        if (name.length() > MAX_USERNAME_LENGTH) {
            localValidation = "Username must be " + MAX_USERNAME_LENGTH + " characters or fewer.";
            return this;
        }
        username = name;
        submitted = true;
        try {
            appCoordinator.createLoginRequest(name);
        } catch (Exception ex) {
            localValidation = "Login failed: " + ex.getMessage();
            submitted = false;
        }
        return this;
    }

    private void drawBox(TuiTextGraphics tg, int x, int y, int w, int h, String title) {
        tg.setForegroundColor(TuiColor.WHITE);
        tg.setBackgroundColor(TuiColor.BLACK);
        StringBuilder top    = new StringBuilder("┌");
        StringBuilder bottom = new StringBuilder("└");
        for (int i = 0; i < w - 2; i++) {
            top.append('─');
            bottom.append('─');
        }
        top.append('┐');
        bottom.append('┘');

        tg.putString(x, y, top.toString());
        tg.putString(x, y + h - 1, bottom.toString());
        for (int i = 1; i < h - 1; i++) {
            tg.putString(x, y + i, "│");
            tg.putString(x + w - 1, y + i, "│");
            tg.putString(x + 1, y + i, " ".repeat(w - 2));
        }
        if (title != null && !title.isEmpty()) {
            int tx = x + Math.max(1, (w - title.length()) / 2);
            tg.setBackgroundColor(TuiColor.YELLOW);
            tg.setForegroundColor(TuiColor.BLACK);
            tg.putString(tx, y, title);
            tg.setBackgroundColor(TuiColor.BLACK);
            tg.setForegroundColor(TuiColor.WHITE);
        }
    }

    private void drawButton(TuiTextGraphics tg, int x, int y, String label, boolean focused) {
        if (focused) {
            tg.setBackgroundColor(TuiColor.WHITE);
            tg.setForegroundColor(TuiColor.BLACK);
        } else {
            tg.setForegroundColor(TuiColor.WHITE);
            tg.setBackgroundColor(TuiColor.BLACK);
        }
        tg.putString(x, y, label);
        tg.setForegroundColor(TuiColor.WHITE);
        tg.setBackgroundColor(TuiColor.BLACK);
    }

    private static String padField(String text, int width) {
        if (text == null) text = "";
        if (text.length() >= width) return text.substring(0, width);
        int totalSpaces = width - text.length();
        int leftSpaces = totalSpaces / 2;
        int rightSpaces = totalSpaces - leftSpaces;
        return " ".repeat(leftSpaces) + text + " ".repeat(rightSpaces);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return (s.length() <= max) ? s : s.substring(0, max - 1) + "…";
    }
}
