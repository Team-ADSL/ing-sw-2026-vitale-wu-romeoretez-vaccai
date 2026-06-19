package org.adsl.client.view.gui.screens;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.adsl.client.AppCoordinator;
import org.adsl.client.view.gui.FloatingLog;
import org.adsl.shared.model.DBRecord;
import org.adsl.shared.model.MatchResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * GUI end-game screen. Splits the view into a single-match standings table
 * (left) and the cumulative DB leaderboard (right). When the server reports
 * an empty DB leaderboard (NoGameDAO fallback) the right pane shows a red
 * alert instead.
 */
public class EndGameScreen extends GUIScreen {

    @FXML private TableView<RankedMatchRow> matchTable;
    @FXML private TableColumn<RankedMatchRow, Integer> matchRankCol;
    @FXML private TableColumn<RankedMatchRow, String>  matchPlayerCol;
    @FXML private TableColumn<RankedMatchRow, Integer> matchPpCol;
    @FXML private TableColumn<RankedMatchRow, Integer> matchFoodCol;

    @FXML private TableView<DBRecord> recordsTable;
    @FXML private TableColumn<DBRecord, Integer> dbRankCol;
    @FXML private TableColumn<DBRecord, String>  dbPlayerCol;
    @FXML private TableColumn<DBRecord, Integer> dbScoreCol;

    @FXML private VBox  leaderboardBox;
    @FXML private Label noDbAlert;
    @FXML private Label errorLabel;

    private final FloatingLog floatingLog;

    public EndGameScreen(AppCoordinator coordinator, String username,
                         List<MatchResult> results, List<DBRecord> records, String message) {
        super(coordinator, username);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/endgame.fxml"));
            loader.setController(this);
            this.root = loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load endgame.fxml", e);
        }
        wireMatchTable(results);
        wireRecordsTable(records, message);
        applyTheme(this.root);

        floatingLog = new FloatingLog();
        if (this.root instanceof StackPane sp) {
            sp.getChildren().add(floatingLog.getFloatingNode());
        }
    }

    private void wireMatchTable(List<MatchResult> results) {
        matchRankCol.setCellValueFactory(c   -> new ReadOnlyObjectWrapper<>(c.getValue().rank()));
        matchPlayerCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().nickname()));
        matchPpCol.setCellValueFactory(c     -> new ReadOnlyObjectWrapper<>(c.getValue().pp()));
        matchFoodCol.setCellValueFactory(c   -> new ReadOnlyObjectWrapper<>(c.getValue().food()));

        if (results == null) return;
        List<MatchResult> sorted = results.stream()
                .sorted(Comparator.comparingInt(MatchResult::pp).reversed()
                        .thenComparing(Comparator.comparingInt(MatchResult::food).reversed()))
                .toList();
        List<RankedMatchRow> rows = new ArrayList<>();
        int rank = 0;
        int prevPp = Integer.MIN_VALUE;
        int prevFood = Integer.MIN_VALUE;
        for (int i = 0; i < sorted.size(); i++) {
            MatchResult r = sorted.get(i);
            if (r.pp() != prevPp || r.food() != prevFood) {
                rank = i + 1;
                prevPp = r.pp();
                prevFood = r.food();
            }
            rows.add(new RankedMatchRow(rank, r.nickname(), r.pp(), r.food()));
        }
        matchTable.setItems(FXCollections.observableArrayList(rows));
    }

    private void wireRecordsTable(List<DBRecord> records, String message) {
        dbRankCol.setCellValueFactory(c   -> new ReadOnlyObjectWrapper<>(c.getValue().rank()));
        dbPlayerCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().nickname()));
        dbScoreCol.setCellValueFactory(c  -> new ReadOnlyObjectWrapper<>(c.getValue().score()));

        boolean dbAvailable = records != null && !records.isEmpty();
        leaderboardBox.setVisible(dbAvailable);
        leaderboardBox.setManaged(dbAvailable);
        noDbAlert.setVisible(!dbAvailable);
        noDbAlert.setManaged(!dbAvailable);

        if (dbAvailable) {
            recordsTable.setItems(FXCollections.observableArrayList(records));
        } else if (message != null && !message.isBlank()) {
            noDbAlert.setText(message);
        }
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
        appCoordinator.requestShutdown();
    }

    /** Adapter that adds a client-side rank to {@link MatchResult} for the table. */
    public record RankedMatchRow(int rank, String nickname, int pp, int food) {}
}
