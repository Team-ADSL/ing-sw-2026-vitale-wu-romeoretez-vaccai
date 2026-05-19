package org.adsl.client.view.gui.screens;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.adsl.client.AppCoordinator;
import org.adsl.shared.model.DBRecord;

import java.io.IOException;
import java.util.List;

/**
 * GUI end-game screen. Displays the final leaderboard in a sortable table.
 * A "Return to home" button exits the current game view.
 */
public class EndGameScreen extends GUIScreen {

    @FXML private TableView<DBRecord> resultsTable;
    @FXML private TableColumn<DBRecord, Integer> rankCol;
    @FXML private TableColumn<DBRecord, String> nicknameCol;
    @FXML private TableColumn<DBRecord, Integer> scoreCol;
    @FXML private Label errorLabel;

    public EndGameScreen(AppCoordinator coordinator, String username, List<DBRecord> results) {
        super(coordinator, username);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/endgame.fxml"));
            loader.setController(this);
            this.root = loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load endgame.fxml", e);
        }
        rankCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().rank()));
        nicknameCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().nickname()));
        scoreCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().score()));
        if (results != null) {
            resultsTable.setItems(FXCollections.observableArrayList(results));
        }
        applyTheme(this.root);
    }

    @FXML
    private void onBackHome() {
        try {
            appCoordinator.createExitGameRequest();
        } catch (Exception ex) {
            errorLabel.setText("Failed to return home: " + ex.getMessage());
        }
    }

    @FXML
    private void onExit() {
        try {
            appCoordinator.disconnect();
        } catch (Exception ignored) {}
        javafx.application.Platform.exit();
    }
}
