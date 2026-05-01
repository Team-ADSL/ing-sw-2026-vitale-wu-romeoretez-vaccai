package org.adsl.client.view.tui.screens;

/**
 * Terminal state of the TUI state machine. When the main loop observes that
 * the current screen is this instance it stops the loop and shuts down.
 */
public final class ExitScreen implements Screen {

    public static final ExitScreen INSTANCE = new ExitScreen();

    private ExitScreen() {}

    @Override
    public void render() {}

    @Override
    public void setToRender(boolean value) {

    }

    @Override
    public boolean isToRender() {
        return true;
    }
}
