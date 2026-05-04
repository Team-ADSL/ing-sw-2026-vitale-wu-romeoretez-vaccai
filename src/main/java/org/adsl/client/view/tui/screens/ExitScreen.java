package org.adsl.client.view.tui.screens;

/**
 * Terminal state of the TUI state machine. When the main loop observes that
 * the current screen is this instance it stops the loop and shuts down.
 */
public final class ExitScreen extends Screen {

    public static final ExitScreen INSTANCE = new ExitScreen();

    private ExitScreen() {
        super();
    }

    @Override
    public void render() {}
}