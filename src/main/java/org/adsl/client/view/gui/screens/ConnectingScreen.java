package org.adsl.client.view.gui.screens;

import javafx.fxml.FXMLLoader;
import org.adsl.client.AppCoordinator;
import org.adsl.client.serverEvents.LoginNeededEvent;

import java.io.IOException;

/**
 * Initial screen shown while waiting for the server to ask for login.
 * Default {@link GUIScreen#visit(LoginNeededEvent)}
 * already routes to {@link LoginScreen}, so no overrides are needed here.
 */
public class ConnectingScreen extends GUIScreen {

    public ConnectingScreen(AppCoordinator coordinator) {
        super(coordinator);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/connecting.fxml"));
            loader.setController(this);
            this.root = loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load connecting.fxml", e);
        }
        applyTheme(this.root);
    }
}
