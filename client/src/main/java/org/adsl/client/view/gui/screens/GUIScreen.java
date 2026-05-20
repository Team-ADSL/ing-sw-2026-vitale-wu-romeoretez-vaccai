package org.adsl.client.view.gui.screens;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import org.adsl.client.AppCoordinator;
import org.adsl.client.serverEvents.*;
import org.adsl.client.view.gui.ImageCatalog;
import org.adsl.client.view.Screen;

import java.util.Objects;

/**
 * Base class for every GUI screen. Mirrors {@link org.adsl.client.view.tui.screens.TUIScreen}
 * for the JavaFX side: each concrete screen owns its FXML controller role,
 * exposes the JavaFX {@link Parent} root and reacts to server events via the
 * {@link EventVisitor} contract. Default visit methods route events to the
 * "natural" next screen so concrete screens only override what differs.
 */
public abstract class GUIScreen extends Screen<GUIScreen> {

    protected String username;
    protected String error;
    protected Parent root;

    protected GUIScreen(AppCoordinator coordinator, String username) {
        this.username = username;
        this.error = null;
        super(coordinator);
    }

    protected GUIScreen(AppCoordinator coordinator) {
        this(coordinator, null);
    }

    /** JavaFX root node displayed by this screen. */
    public Parent getRoot() {
        return root;
    }

    /**
     * Called once when the GUI transitions to this screen, on the JavaFX
     * Application Thread. Returns {@code null} to stay, or a new
     * {@link GUIScreen} to redirect immediately.
     */
    public GUIScreen onEnter() {
        return null;
    }

    // ── Default server-event routing (mirrors TUIScreen defaults) ──────────────
    @Override
    public GUIScreen createLoginScreen(LoginNeededEvent e, AppCoordinator appCoordinator){
        return new LoginScreen(appCoordinator);
    }

    @Override
    public GUIScreen createHomeScreen(HomeUpdateEvent e, AppCoordinator appCoordinator){
        return new HomeScreen(appCoordinator, username, e.activeGames(), e.gamePlayers(), e.gameCapacity());
    }

    @Override
    public GUIScreen createLobbyScreen(LobbyUpdateEvent e, AppCoordinator appCoordinator){
        return new LobbyScreen(appCoordinator, username, e.gameId(), e.players(),
                e.numPlayersAllowed());
    }

    @Override
    public GUIScreen createTotemPickingScreen(TotemAvailableEvent e, AppCoordinator appCoordinator){
        return new TotemPickingScreen(appCoordinator, username, e.totemList());
    }

    @Override
    public GUIScreen createGameScreen(GameUpdateEvent e, AppCoordinator appCoordinator){
        return new GameScreen(appCoordinator, username, e.game());
    }

    @Override
    public GUIScreen createEndGameScreen(EndGameEvent e, AppCoordinator appCoordinator){
        return new EndGameScreen(appCoordinator, username, e.results(), e.records(), e.message());
    }

    @Override
    public GUIScreen createDisconnectedScreen(DisconnectedEvent e, AppCoordinator appCoordinator){
        return new DisconnectedScreen(appCoordinator, e.message());
    }

    @Override
    public GUIScreen getThis(){
        return this;
    }

    // ── Dark theme helper ──────────────────────────────────────────────────────

    protected static void applyTheme(Parent root) {
        applyTheme(root, true);
    }

    private static Image BG_IMAGE = null;

    protected static void applyTheme(Parent root, boolean withBackground) {
        String css = Objects.requireNonNull(
                GUIScreen.class.getResource("/assets/theme.css")).toExternalForm();
        if (!root.getStylesheets().contains(css)) {
            root.getStylesheets().add(css);
        }
        if (withBackground && root instanceof StackPane sp) {
            if (BG_IMAGE == null) {
                BG_IMAGE = new Image(Objects.requireNonNull(
                    GUIScreen.class.getResourceAsStream("/assets/general/initial_game_background.png")));
            }
            final double imgW = BG_IMAGE.getWidth();
            final double imgH = BG_IMAGE.getHeight();

            ImageView bg = new ImageView(BG_IMAGE);
            bg.setSmooth(true);
            bg.setPreserveRatio(false);

            // Anchor bg at TOP_LEFT so layoutX=0; translateX/Y are the sole positioning.
            StackPane.setAlignment(bg, Pos.TOP_LEFT);

            bg.fitWidthProperty().bind(Bindings.createDoubleBinding(() -> {
                double scale = Math.max(sp.getWidth() / imgW, sp.getHeight() / imgH);
                return imgW * scale;
            }, sp.widthProperty(), sp.heightProperty()));

            bg.fitHeightProperty().bind(Bindings.createDoubleBinding(() -> {
                double scale = Math.max(sp.getWidth() / imgW, sp.getHeight() / imgH);
                return imgH * scale;
            }, sp.widthProperty(), sp.heightProperty()));

            // Center horizontally and vertically.
            bg.translateXProperty().bind(Bindings.createDoubleBinding(() -> {
                double scale = Math.max(sp.getWidth() / imgW, sp.getHeight() / imgH);
                return (sp.getWidth() - imgW * scale) / 2.0;
            }, sp.widthProperty(), sp.heightProperty()));

            bg.translateYProperty().bind(Bindings.createDoubleBinding(() -> {
                double scale = Math.max(sp.getWidth() / imgW, sp.getHeight() / imgH);
                return (sp.getHeight() - imgH * scale) / 2.0;
            }, sp.widthProperty(), sp.heightProperty()));

            // Clip overflow so the scaled image doesn't bleed outside the pane.
            Rectangle clip = new Rectangle();
            clip.widthProperty().bind(sp.widthProperty());
            clip.heightProperty().bind(sp.heightProperty());
            sp.setClip(clip);

            sp.getChildren().addFirst(bg);
        }
        applyChalkFonts(root);
    }

    private static void applyChalkFonts(Parent node) {
        for (Node child : node.getChildrenUnmodifiable()) {
            switch (child) {
                case Label l     -> setFont(l);
                case Button b    -> setFont(b);
                case TextField t -> setFont(t);
                case Pane p      -> setFont(p);
                default          -> { /* node type with no chalk font */ }
            }
        }
    }

    private static void setFont(Label node){
        Font f = fontFor(node, extractFontSize(node.getStyle(), 15.0));
        node.setFont(f);
        pinFontFamily(node, f.getFamily());
    }
    private static void setFont(Button node){
        Font f = fontFor(node, extractFontSize(node.getStyle(), 16.0));
        node.setFont(f);
        pinFontFamily(node, f.getFamily());
    }
    private static void setFont(TextField node){
        Font f = fontFor(node, extractFontSize(node.getStyle(), 15.0));
        node.setFont(f);
        pinFontFamily(node, f.getFamily());
    }

    /** Pins font-family into the inline style so CSS layout pass cannot override setFont(). */
    private static void pinFontFamily(Node node, String family) {
        String s = node.getStyle() != null ? node.getStyle() : "";
        if (!s.contains("-fx-font-family")) {
            node.setStyle(s + "; -fx-font-family: '" + family + "';");
        }
    }
    private static void setFont(Pane node){
        applyChalkFonts(node);
    }

    private static javafx.scene.text.Font fontFor(Node node, double size) {
        return node.getStyleClass().contains("tech-text")
                ? ImageCatalog.robotoFont(size)
                : ImageCatalog.chalkFont(size);
    }

    private static double extractFontSize(String style, double fallback) {
        if (style == null || !style.contains("-fx-font-size")) return fallback;
        try {
            int idx = style.indexOf("-fx-font-size:");
            String sub = style.substring(idx + 14).trim();
            String num = sub.replaceAll("[^0-9.].*", "").trim();
            return Double.parseDouble(num);
        } catch (Exception e) {
            return fallback;
        }
    }
}
