package org.example.server.controller.states;

import org.example.server.controller.GameController;
import org.example.server.persistence.GameDAO;
import org.example.server.persistence.SqlGameDAO;
import org.example.shared.enums.Phase;
import org.example.shared.enums.CardType;
import org.example.shared.enums.Trigger;
import org.example.server.model.cards.buildings.Building;
import org.example.server.model.cards.characters.Builder;
import org.example.server.model.cards.characters.Inventor;
import org.example.server.model.Game;
import org.example.server.model.Player;
import org.example.shared.model.MatchResult;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public class EndGameState extends ControllerState {
    public EndGameState(Game game, GameController context) {
        super(game, context);
    }

    @Override
    public ControllerState onEntry() throws Exception {
        getGame().setPhase(Phase.END_GAME);
        Set<Player> players = getGame().getPlayers();
        for(Player p : players){
            p.getCards().get(CardType.BUILDINGS)
                    .forEach(b -> b.activeEffect(Set.of(p), Trigger.END_GAME));

            int builderPoints = p.getCards().get(CardType.BUILDER).stream()
                    .map(c -> (Builder)c)
                    .mapToInt(Builder::getPP)
                    .sum();
            p.changePP(builderPoints);

            int inventorIcons = (int)p.getCards().get(CardType.INVENTOR).stream()
                    .map(c -> (Inventor)c)
                    .map(Inventor::getIcon)
                    .distinct()
                    .count();
            int numInventors = p.getCards().get(CardType.INVENTOR).size();
            p.changePP(numInventors * inventorIcons);

            int numArtists = p.getCards().get(CardType.ARTIST).size();
            p.changePP(10 * numArtists / 2);

            int buildingPoints = p.getCards().get(CardType.BUILDINGS).stream()
                    .map(c -> (Building)c)
                    .mapToInt(Building::getEndGamePP)
                    .sum();
            p.changePP(buildingPoints * p.getBuildingBonus().getBuilderMultiplierPP());
        }

        GameDAO gameDAO = getContext().getGameDAO();
        List<String> nicknames = players.stream()
                .map(Player::getName)
                .toList();
        List<Integer> scores = players.stream()
                .map(Player::getPp)
                .toList();
        gameDAO.saveMatch(getGame().getGameId(), players.size(), nicknames, scores);
        List<MatchResult> matchResults = gameDAO.getLeaderboard(players.size());

        getGame().notifyEndGame(matchResults);
        return nextState();
    }

    @Override
    public ControllerState nextState() {
        return this;
    }
}
