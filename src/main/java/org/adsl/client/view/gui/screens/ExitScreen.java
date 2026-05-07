package org.adsl.client.view.gui.screens;

/**
 * Marker screen that signals the GUI loop to terminate. Holds no JavaFX
 * resources; the {@link org.adsl.client.view.gui.GUI} top-level class checks
 * for this type after every transition and shuts down.
 */
public final class ExitScreen extends GUIScreen {

    public static final ExitScreen INSTANCE = new ExitScreen();

    private ExitScreen() {
        super(null);
    }
}
