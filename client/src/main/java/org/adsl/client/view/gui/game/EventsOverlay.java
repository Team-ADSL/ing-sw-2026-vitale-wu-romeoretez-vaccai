package org.adsl.client.view.gui.game;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Full-screen orange wash shown while end-of-round events resolve: a big title
 * plus one line per player parsed from the server log message. A child of the
 * game screen's root stack, centered, hidden until {@link #show}. Stays up until
 * the next response replaces it ({@link #show} again) or clears it ({@link #hide}).
 */
public final class EventsOverlay extends StackPane {

    private final Label title;
    private final VBox playerList;

    public EventsOverlay() {
        setStyle("-fx-background-color: rgba(239, 108, 0, 0.55);");
        setMouseTransparent(true);
        setVisible(false);

        title = new Label("");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 64px; -fx-font-weight: bold;"
                + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.85), 8, 0.4, 0, 0);");
        title.setAlignment(Pos.CENTER);

        playerList = new VBox(6);
        playerList.setAlignment(Pos.CENTER);

        VBox content = new VBox(24, title, playerList);
        content.setAlignment(Pos.CENTER);
        getChildren().add(content);
        StackPane.setAlignment(content, Pos.CENTER);
    }

    /**
     * Shows the event {@code title} plus per-player delta lines parsed from the
     * server-side {@code logMessage}. AppCoordinator's pacer ensures a minimum
     * gap between consecutive arrivals.
     */
    public void show(String title, String logMessage) {
        this.title.setText(title.toUpperCase());
        populate(logMessage);
        setVisible(true);
    }

    public void hide() {
        setVisible(false);
    }

    private void populate(String logMessage) {
        playerList.getChildren().clear();
        if (logMessage == null || logMessage.isBlank()) return;
        int sep = logMessage.indexOf(" — ");
        if (sep < 0) return;
        String body = logMessage.substring(sep + " — ".length());
        for (String seg : body.split("\\s\\|\\s")) {
            String text = seg.trim();
            if (text.isEmpty()) continue;
            Label line = new Label(text);
            line.setStyle("-fx-text-fill: white; -fx-font-size: 26px; -fx-font-weight: 600;"
                    + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.75), 4, 0.4, 0, 0);");
            playerList.getChildren().add(line);
        }
    }
}
