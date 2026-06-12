package org.adsl.client.view.tui.screens;

/**
 * Terminal state of the TUI state machine. When the main loop observes that
 * the current screen is this instance it stops the loop and shuts down.
 */
public final class ExitScreen extends TUIScreen {

    public static final ExitScreen INSTANCE = new ExitScreen();

    private ExitScreen() {
        super();
    }

    /** No-op: this screen is never actually drawn. */
    @Override
    public void render() {}

    /**
     * @return always {@code true}, signalling the TUI main loop to stop
     */
    @Override
    public boolean isExit() {
        return true;
    }
}