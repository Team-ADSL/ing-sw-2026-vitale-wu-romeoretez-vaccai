package org.adsl.client.view.gui.screens;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.shape.Rectangle;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.adsl.client.AppCoordinator;
import org.adsl.client.serverEvents.ErrorEvent;
import org.adsl.client.serverEvents.HomeUpdateEvent;
import org.adsl.client.view.gui.FloatingLog;
import org.adsl.client.view.gui.ImageCatalog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * GUI home screen. Displays the list of open games as rich cards (showing
 * player slots, names, and capacity) and lets the player create a new game
 * (2–5 players) or join an existing one. Updates in-place when
 * {@code HomeUpdateEvent} arrives. A floating log overlay shows server messages.
 */
public class HomeScreen extends GUIScreen {

    @FXML private StackPane rootStack;
    @FXML private Label welcomeLabel;
    @FXML private Label errorLabel;
    @FXML private ListView<Integer> gamesList;
    @FXML private Button create2Button;
    @FXML private Button create3Button;
    @FXML private Button create4Button;
    @FXML private Button create5Button;
    @FXML private Button joinButton;
    @FXML private Button logoutButton;
    @FXML private VBox logBox;

    private List<Integer> activeGames;
    private Map<Integer, List<String>> gamePlayers;
    private Map<Integer, Integer> gameCapacity;
    private final FloatingLog floatingLog;
    private List<Image> rulesPages;
    private int rulesPageIndex = 0;
    private StackPane rulesOverlay;

    public HomeScreen(AppCoordinator coordinator, String username, List<Integer> activeGames,
                      Map<Integer, List<String>> gamePlayers, Map<Integer, Integer> gameCapacity) {
        super(coordinator, username);
        this.activeGames = activeGames != null ? new ArrayList<>(activeGames) : new ArrayList<>();
        this.gamePlayers = gamePlayers != null ? gamePlayers : Collections.emptyMap();
        this.gameCapacity = gameCapacity != null ? gameCapacity : Collections.emptyMap();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/home.fxml"));
            loader.setController(this);
            this.root = loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load home.fxml", e);
        }
        welcomeLabel.setText("Welcome, " + (username != null ? username : "") + "!");
        setupGamesList();
        installListKeyboardNav();
        decorateCreateButtons();
        setupButtonNav();
        applyTheme(this.root);
        setupRulesButton();

        floatingLog = new FloatingLog();
        logBox.getChildren().setAll(floatingLog.getFloatingNode());
        rootStack.getChildren().add(floatingLog.getFullPanel());

        // Navigation from joinButton and logoutButton to the log toggle button
        javafx.event.EventHandler<javafx.scene.input.KeyEvent> bottomDown = e -> {
            if (e.getCode() == KeyCode.DOWN) {
                floatingLog.getToggleButton().requestFocus();
                e.consume();
            }
        };
        if (joinButton != null) joinButton.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, bottomDown);
        if (logoutButton != null) logoutButton.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, bottomDown);

        // Navigation from the log toggle button back to the main buttons/list
        floatingLog.getToggleButton().addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.UP) {
                if (logoutButton != null) logoutButton.requestFocus();
                else if (joinButton != null) joinButton.requestFocus();
                else {
                    gamesList.requestFocus();
                    if (!gamesList.getItems().isEmpty()) {
                        gamesList.getSelectionModel().selectLast();
                    }
                }
                e.consume();
            } else if (e.getCode() == KeyCode.LEFT) {
                if (logoutButton != null) logoutButton.requestFocus();
                else if (joinButton != null) joinButton.requestFocus();
                e.consume();
            }
        });
    }

    private void setupGamesList() {
        gamesList.setCellFactory(_ -> new ListCell<>() {
            @Override
            protected void updateItem(Integer id, boolean empty) {
                super.updateItem(id, empty);
                if (empty || id == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setGraphic(buildGameCard(id));
                setText(null);
            }
        });
        gamesList.setItems(FXCollections.observableArrayList(activeGames));
    }

    private javafx.scene.Node buildGameCard(int gameId) {
        List<String> players = gamePlayers.getOrDefault(gameId, Collections.emptyList());
        int capacity = gameCapacity.getOrDefault(gameId, 0);
        int free = Math.max(0, capacity - players.size());

        // background color varies with capacity: 2p=maroon, 3p=teal, 4p=indigo, 5p=forest
        String[] bgColors = {
            "rgba(140,22,54,0.38)",
            "rgba(22,100,100,0.38)",
            "rgba(60,40,120,0.38)",
            "rgba(30,90,50,0.38)"
        };
        String bg = bgColors[Math.clamp(capacity - 2, 0, bgColors.length - 1)];

        HBox card = new HBox(10);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(8, 12, 8, 12));
        card.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 10; " +
                "-fx-border-color: rgba(242,176,53,0.22); -fx-border-radius: 10; -fx-border-width: 1;");

        // Game title
        Label title = new Label("#" + gameId);
        title.setFont(ImageCatalog.chalkFont(16));
        title.setStyle("-fx-text-fill: #F2B035; -fx-font-size: 16px;");
        title.setMinWidth(50);

        // Player slot dots
        HBox slots = new HBox(4);
        slots.setAlignment(Pos.CENTER_LEFT);
        for (String player : players) {
            Label p = new Label("●"); // filled circle
            p.setStyle("-fx-text-fill: #FDF3D3; -fx-font-size: 14px;");
            p.setFont(ImageCatalog.chalkFont(14));
            Tooltip.install(p, new Tooltip(player));
            slots.getChildren().add(p);
        }
        for (int i = 0; i < free; i++) {
            Label p = new Label("○"); // empty circle
            p.setStyle("-fx-text-fill: rgba(253,243,211,0.35); -fx-font-size: 14px;");
            p.setFont(ImageCatalog.chalkFont(14));
            slots.getChildren().add(p);
        }

        // Player names summary
        String summary = players.isEmpty() ? "empty" : String.join(", ", players);
        Label names = new Label(summary);
        names.setFont(ImageCatalog.chalkFont(13));
        names.setStyle("-fx-text-fill: rgba(253,243,211,0.70); -fx-font-size: 13px;");
        names.setMaxWidth(200);
        names.setEllipsisString("…");

        // Capacity tag
        Label cap = new Label(players.size() + "/" + (capacity > 0 ? capacity : "?"));
        cap.setFont(ImageCatalog.chalkFont(13));
        cap.setStyle("-fx-text-fill: #F2B035; -fx-font-size: 13px;");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(title, slots, names, spacer, cap);
        return card;
    }

    private void installListKeyboardNav() {
        gamesList.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            KeyCode code = e.getCode();
            if (code == KeyCode.ENTER) {
                onJoinSelected();
                e.consume();
                return;
            }
            if (code == KeyCode.DOWN) {
                int sel = gamesList.getSelectionModel().getSelectedIndex();
                int last = gamesList.getItems().size() - 1;
                if (sel >= last && joinButton != null) {
                    gamesList.getSelectionModel().clearSelection();
                    joinButton.requestFocus();
                    e.consume();
                }
            } else if (code == KeyCode.UP) {
                int sel = gamesList.getSelectionModel().getSelectedIndex();
                if (sel <= 0) {
                    gamesList.getSelectionModel().clearSelection();
                    if (create2Button != null) {
                        create2Button.requestFocus();
                        e.consume();
                    }
                }
            }
        });
    }

    private void setupButtonNav() {
        javafx.event.EventHandler<javafx.scene.input.KeyEvent> topDown = e -> {
            if (e.getCode() == KeyCode.DOWN) {
                gamesList.requestFocus();
                if (!gamesList.getItems().isEmpty()) {
                    gamesList.getSelectionModel().selectFirst();
                }
                e.consume();
            }
        };
        if (create2Button != null) create2Button.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, topDown);
        if (create3Button != null) create3Button.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, topDown);
        if (create4Button != null) create4Button.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, topDown);
        if (create5Button != null) create5Button.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, topDown);

        javafx.event.EventHandler<javafx.scene.input.KeyEvent> bottomUp = e -> {
            if (e.getCode() == KeyCode.UP) {
                gamesList.requestFocus();
                if (!gamesList.getItems().isEmpty()) {
                    gamesList.getSelectionModel().selectLast();
                }
                e.consume();
            }
        };
        if (joinButton != null) joinButton.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, bottomUp);
        if (logoutButton != null) logoutButton.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, bottomUp);
    }

    /**
     * Renders the create buttons as "N 👤" — text stays in chalk via the
     * usual applyChalkFonts pass, and the person emoji lives in a separate
     * Label graphic that intentionally has no setFont so JavaFX falls back
     * to the system emoji font (chalk has no glyph for U+1F464).
     * applyChalkFonts does not recurse into Button children, so the graphic
     * keeps its default font.
     */
    private void decorateCreateButtons() {
        attachPersonIcon(create2Button);
        attachPersonIcon(create3Button);
        attachPersonIcon(create4Button);
        attachPersonIcon(create5Button);
    }

    private static void attachPersonIcon(Button b) {
        Label icon = new Label("👤"); // 👤
        icon.setStyle("-fx-text-fill: inherit;");
        b.setGraphic(icon);
        b.setContentDisplay(ContentDisplay.RIGHT);
        b.setGraphicTextGap(3);
    }



    private void setupRulesButton() {
        Button btn = new Button("?");
        btn.setStyle(
            "-fx-font-size: 18px; -fx-font-weight: bold;" +
            "-fx-min-width: 40px; -fx-min-height: 40px;" +
            "-fx-max-width: 40px; -fx-max-height: 40px;" +
            "-fx-background-radius: 20; -fx-padding: 0;"
        );
        btn.setOnAction(_ -> openRulesOverlay());
        StackPane.setAlignment(btn, Pos.BOTTOM_LEFT);
        StackPane.setMargin(btn, new Insets(0, 0, 14, 14));
        rootStack.getChildren().add(btn);
    }

    private void openRulesOverlay() {
        if (rulesPages == null) rulesPages = ImageCatalog.rulesPages();
        if (rulesPages.isEmpty()) return;

        rulesPageIndex = 0;
        rulesOverlay = new StackPane();
        rulesOverlay.setStyle("-fx-background-color: rgba(0,0,0,0.88);");

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(rulesOverlay.widthProperty());
        clip.heightProperty().bind(rulesOverlay.heightProperty());
        rulesOverlay.setClip(clip);

        ImageView iv = new ImageView(rulesPages.getFirst());
        iv.setPreserveRatio(true);
        iv.fitWidthProperty().bind(rulesOverlay.widthProperty().multiply(0.88));
        iv.fitHeightProperty().bind(rulesOverlay.heightProperty().multiply(0.88));

        Button prev = new Button("◀");
        Button next = new Button("▶");
        Button close = new Button("✕");
        close.setStyle("-fx-font-size: 14px; -fx-min-width: 34px; -fx-min-height: 34px;" +
                       "-fx-max-width: 34px; -fx-max-height: 34px; -fx-background-radius: 17; -fx-padding: 0;");

        Label counter = new Label("1 / " + rulesPages.size());
        counter.setStyle("-fx-text-fill: #FDF3D3; -fx-font-size: 14px;");

        prev.setOnAction(_ -> {
            if (rulesPageIndex > 0) {
                rulesPageIndex--;
                iv.setImage(rulesPages.get(rulesPageIndex));
                counter.setText((rulesPageIndex + 1) + " / " + rulesPages.size());
            }
        });
        next.setOnAction(_ -> {
            if (rulesPageIndex < rulesPages.size() - 1) {
                rulesPageIndex++;
                iv.setImage(rulesPages.get(rulesPageIndex));
                counter.setText((rulesPageIndex + 1) + " / " + rulesPages.size());
            }
        });
        close.setOnAction(_ -> rootStack.getChildren().remove(rulesOverlay));
        rulesOverlay.setOnMouseClicked(e -> {
            if (e.getTarget() == rulesOverlay) rootStack.getChildren().remove(rulesOverlay);
        });

        HBox nav = new HBox(16, prev, counter, next);
        nav.setAlignment(Pos.CENTER);

        VBox content = new VBox(12, iv, nav);
        content.setAlignment(Pos.CENTER);
        content.setPickOnBounds(false);

        StackPane.setAlignment(close, Pos.TOP_RIGHT);
        StackPane.setMargin(close, new Insets(12, 12, 0, 0));

        rulesOverlay.getChildren().addAll(content, close);
        rootStack.getChildren().add(rulesOverlay);
    }

    @FXML private void onCreate2() { create(2); }
    @FXML private void onCreate3() { create(3); }
    @FXML private void onCreate4() { create(4); }
    @FXML private void onCreate5() { create(5); }

    private void create(int n) {
        errorLabel.setText("");
        try {
            appCoordinator.createGameRequest(n);
        } catch (Exception ex) {
            errorLabel.setText("Failed to create game: " + ex.getMessage());
        }
    }

    @FXML
    private void onJoinSelected() {
        errorLabel.setText("");
        Integer id = gamesList.getSelectionModel().getSelectedItem();
        if (id == null) {
            errorLabel.setText("Select a game to join.");
            return;
        }
        try {
            appCoordinator.enterGameRequest(id);
        } catch (Exception ex) {
            errorLabel.setText("Failed to join: " + ex.getMessage());
        }
    }

    @FXML
    private void onLogout() {
        errorLabel.setText("");
        try {
            appCoordinator.createLogoutRequest();
        } catch (Exception ex) {
            errorLabel.setText("Logout failed: " + ex.getMessage());
        }
    }

    @Override
    public GUIScreen visit(HomeUpdateEvent e) {
        this.activeGames = e.activeGames() != null ? new ArrayList<>(e.activeGames()) : new ArrayList<>();
        this.gamePlayers = e.gamePlayers() != null ? e.gamePlayers() : Collections.emptyMap();
        this.gameCapacity = e.gameCapacity() != null ? e.gameCapacity() : Collections.emptyMap();
        gamesList.setItems(FXCollections.observableArrayList(activeGames));
        if (e.message() != null && !e.message().isBlank()) floatingLog.append(e.message());
        return this;
    }

    @Override
    public GUIScreen visit(ErrorEvent e) {
        if (errorLabel != null) errorLabel.setText(e.message());
        return this;
    }
}
