package org.adsl.client.view.tui.screens;

import org.adsl.client.AppCoordinator;
import org.adsl.client.serverEvents.*;
import org.adsl.client.view.Screen;
import org.adsl.client.view.tui.TuiGameLog;
import org.adsl.client.view.tui.screens.TotemPickingScreen;
import org.adsl.client.view.tui.events.*;
import org.adsl.client.view.tui.render.TuiColor;
import org.adsl.client.view.tui.render.TuiSize;
import org.adsl.client.view.tui.render.TuiTerminal;
import org.adsl.client.view.tui.render.TuiTextGraphics;
import org.adsl.shared.network.responses.TotemAvailableUpdate;

import java.io.IOException;
import java.util.List;

/**
 * Base abstract class for all TUI screens. Centralises the attributes and
 * visit defaults shared by every screen so concrete subclasses only need to
 * override what differs.
 * <p>
 * Each concrete screen represents one phase of interaction in the TUI state
 * machine. Server responses and key presses are both delivered as
 * {@link Event} objects and dispatched through the visitor methods inherited
 * from {@link EventVisitor}: returning {@code this} keeps the current screen,
 * returning a new {@link TUIScreen} instance triggers a transition.
 */
public abstract class TUIScreen extends Screen<TUIScreen> implements InputEventVisitor {

    protected final TuiTerminal terminal;
    protected String username;
    protected boolean toRender;
    /** When true, the game-log overlay window is rendered on top of the screen. */
    protected boolean showLog = false;

    private static final int LOG_PREVIEW_COUNT = 3;
    private static final int LOG_PREVIEW_WIDTH = 52;

    protected TUIScreen(TuiTerminal terminal,
            AppCoordinator appCoordinator,
            String username) {
        this.terminal = terminal;
        this.username = username;
        this.toRender = true;
        super(appCoordinator);
    }

    protected TUIScreen(TuiTerminal terminal,
            AppCoordinator coordinator) {
        this(terminal, coordinator, null);
    }

    protected TUIScreen(TuiTerminal terminal) {
        this(terminal, null, null);
    }

    /**
     * No-arg constructor for screens with no resources (e.g. {@link ExitScreen}).
     */
    protected TUIScreen() {
        this(null, null, null);
    }

    /**
     * Server reported an error: jump back to the {@link LoginScreen} carrying the
     * error message.
     */

    @Override
    public TUIScreen createLoginScreen(LoginNeededEvent e, AppCoordinator appCoordinator){
        return new LoginScreen(terminal, appCoordinator);
    }

    @Override
    public TUIScreen createHomeScreen(HomeUpdateEvent e, AppCoordinator appCoordinator){
        return new HomeScreen(terminal, appCoordinator, username, e.activeGames());
    }

    @Override
    public TUIScreen createLobbyScreen(LobbyUpdateEvent e, AppCoordinator appCoordinator){
        return new LobbyScreen(terminal, appCoordinator, username, e.gameId(), e.players(),
                e.numPlayersAllowed());
    }

    @Override
    public TUIScreen createTotemPickingScreen(TotemAvailableEvent e, AppCoordinator appCoordinator){
        return new TotemPickingScreen(terminal, appCoordinator, username, e.totemList());
    }

    @Override
    public TUIScreen createGameScreen(GameUpdateEvent e, AppCoordinator appCoordinator){
        return new GameScreen(terminal, appCoordinator, username, e.game());
    }

    @Override
    public TUIScreen createEndGameScreen(EndGameEvent e, AppCoordinator appCoordinator){
        return new EndGameScreen(terminal, appCoordinator, username, e.results());
    }

    @Override
    public TUIScreen createDisconnectedScreen(DisconnectedEvent e, AppCoordinator appCoordinator){
        return new DisconnectedScreen(terminal, appCoordinator, e.message());
    }

    @Override
    public TUIScreen getThis(){
        return this;
    }

    @Override
    public TUIScreen visit(ConfirmEvent e) {
        return this;
    }

    @Override
    public TUIScreen visit(SelectEvent e) {
        return this;
    }

    @Override
    public TUIScreen visit(NavigateLeftEvent e) {
        return this;
    }

    @Override
    public TUIScreen visit(NavigateRightEvent e) {
        return this;
    }

    @Override
    public TUIScreen visit(NavigateUpEvent e) {
        return this;
    }

    @Override
    public TUIScreen visit(NavigateDownEvent e) {
        return this;
    }

    @Override
    public TUIScreen visit(CharInputEvent e) {
        return this;
    }

    @Override
    public TUIScreen visit(BackspaceEvent e) {
        return this;
    }

    // ── Lifecycle / framework hooks ──────────────────────────────────────────

    public TUIScreen handleEvent(ServerEvent event) {
        captureLogMessage(event);
        TUIScreen newScreen = event.accept(this);
        setToRender(true);
        return newScreen;
    }

    /**
     * Extracts the user-facing {@code message} field from any server event that
     * carries one and appends it to the shared {@link TuiGameLog}. Centralised
     * here so every concrete screen automatically contributes to the transcript
     * without needing its own append logic.
     */
    private void captureLogMessage(ServerEvent event) {
        String msg = switch (event) {
            case GameUpdateEvent e       -> e.message();
            case LobbyUpdateEvent e      -> e.message();
            case HomeUpdateEvent e       -> e.message();
            case TotemAvailableEvent e   -> e.message();
            default                      -> null;
        };
        if (msg != null && !msg.isBlank()) {
            TuiGameLog.INSTANCE.append(msg);
        }
    }

    /**
     * Toggles the full-history log window. Concrete screens should call this on
     * the 'M' key and render {@link #drawLogPreview} when off / {@link #drawLogWindow}
     * when on. Mirrors the legend overlay pattern in {@link GameScreen}.
     */
    protected void toggleLog() {
        showLog = !showLog;
    }

    /**
     * Renders the last few messages bottom-right with a brightness gradient
     * (older = dimmer). Pure no-op when the log is empty.
     */
    protected void drawLogPreview(TuiTextGraphics tg, TuiSize sz) {
        List<String> recent = TuiGameLog.INSTANCE.recent(LOG_PREVIEW_COUNT);
        if (recent.isEmpty()) return;

        int cols = sz.getColumns();
        int rows = sz.getRows();
        int width = Math.min(LOG_PREVIEW_WIDTH, cols - 2);
        int xRight = cols - 1;
        int xLeft = xRight - width;
        int baseY = rows - 3;

        int n = recent.size();
        TuiColor[] palette = { TuiColor.DARK_GRAY, TuiColor.WHITE, TuiColor.YELLOW };

        tg.setBackgroundColor(TuiColor.BLACK);
        for (int i = 0; i < n; i++) {
            String raw = recent.get(i);
            String line = truncateRight(raw, width);
            int y = baseY - (n - 1 - i);
            if (y < 0) break;
            int paletteIdx = palette.length - n + i;
            if (paletteIdx < 0) paletteIdx = 0;
            tg.setForegroundColor(palette[Math.min(paletteIdx, palette.length - 1)]);
            int x = xRight - line.length();
            if (x < xLeft) x = xLeft;
            tg.putString(x, y, line);
        }
        tg.setForegroundColor(TuiColor.WHITE);
    }

    /**
     * Renders the full log history as a boxed overlay anchored bottom-right.
     * Shows the most recent {@code height-3} messages so the latest entry is
     * always on the last visible line.
     */
    protected void drawLogWindow(TuiTextGraphics tg, TuiSize sz) {
        int cols = sz.getColumns();
        int rows = sz.getRows();
        int w = Math.min(60, cols - 4);
        int h = Math.min(18, rows - 4);
        if (w < 20 || h < 5) return;
        int x = cols - w - 2;
        int y = rows - h - 2;

        tg.setBackgroundColor(TuiColor.BLACK);
        tg.setForegroundColor(TuiColor.CYAN);
        String top = "┌" + "─".repeat(w - 2) + "┐";
        String bot = "└" + "─".repeat(w - 2) + "┘";
        tg.putString(x, y, top);
        tg.putString(x, y + h - 1, bot);
        for (int r = 1; r < h - 1; r++) {
            tg.putString(x, y + r, "│" + " ".repeat(w - 2) + "│");
        }

        tg.setForegroundColor(TuiColor.YELLOW);
        tg.putString(x + 2, y, " GAME LOG (M to close) ");

        List<String> all = TuiGameLog.INSTANCE.all();
        int innerW = w - 4;
        int innerH = h - 2;
        List<String> wrapped = wrapAll(all, innerW);
        int from = Math.max(0, wrapped.size() - innerH);

        tg.setForegroundColor(TuiColor.WHITE);
        int lineY = y + 1;
        for (int i = from; i < wrapped.size(); i++) {
            tg.putString(x + 2, lineY, padRight(wrapped.get(i), innerW));
            lineY++;
            if (lineY >= y + h - 1) break;
        }

        if (all.isEmpty()) {
            tg.setForegroundColor(TuiColor.DARK_GRAY);
            tg.putString(x + 2, y + 1, padRight("(no messages yet)", innerW));
            tg.setForegroundColor(TuiColor.WHITE);
        }
    }

    private static String truncateRight(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        if (max <= 1) return s.substring(0, max);
        return s.substring(0, max - 1) + "…";
    }

    private static String padRight(String s, int len) {
        if (s == null) s = "";
        if (s.length() >= len) return s.substring(0, len);
        return s + " ".repeat(len - s.length());
    }

    private static List<String> wrapAll(List<String> messages, int width) {
        List<String> out = new java.util.ArrayList<>();
        for (String m : messages) {
            if (m == null || m.isEmpty()) continue;
            int i = 0;
            while (i < m.length()) {
                int end = Math.min(m.length(), i + width);
                out.add(m.substring(i, end));
                i = end;
            }
        }
        return out;
    }

    public TUIScreen handleEvent(InputEvent event) {
        TUIScreen newScreen = event.accept(this);
        setToRender(true);
        return newScreen;
    }

    public boolean isToRender() {
        return toRender;
    }

    public void setToRender(boolean value) {
        this.toRender = value;
    }

    /**
     * Called once when the TUI transitions to this screen.
     * Returns {@code null} to stay on this screen, or a new {@link TUIScreen}
     * instance to redirect immediately (e.g. back navigation, exit).
     */
    public TUIScreen onEnter() throws Exception {
        return null;
    }

    public abstract void render() throws IOException;
}
