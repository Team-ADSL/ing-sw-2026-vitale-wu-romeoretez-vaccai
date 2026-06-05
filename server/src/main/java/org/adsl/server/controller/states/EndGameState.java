package org.adsl.server.controller.states;

import org.adsl.server.controller.GameController;
import org.adsl.shared.exceptions.ServerException;
import org.adsl.server.persistence.GameDAO;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.cards.buildings.Building;
import org.adsl.server.model.cards.characters.Builder;
import org.adsl.server.model.cards.characters.Inventor;
import org.adsl.server.model.Game;
import org.adsl.server.model.Player;
import org.adsl.shared.model.DBRecord;
import org.adsl.shared.model.MatchResult;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/**
 * Automatic state that resolves end-of-game scoring and persists results.
 * <p>
 * On entry it applies all end-game building effects, tallies builder PP,
 * inventor icon bonuses, and artist pairs for each player, then saves the
 * match to the database and broadcasts the leaderboard to all clients.
 * Returns {@code null} to signal that the game is over and the controller
 * should be discarded.
 * </p>
 */
public class EndGameState extends ControllerState {
    public EndGameState(Game game, GameController context) {
        super(game, context);
    }

    @Override
    public ControllerState onEntry() throws ServerException {
        Set<Player> players = getGame().getPlayers();
        for(Player p : players){
            // Score each component separately and record the deltas so the final
            // tally is reported in the game log (it stays visible on the end-game
            // screen). The components mirror the Mesos end-game scoring rules.
            int basePP = p.getPp();

            // Building end-game effects (PP bonuses, multipliers). The builder
            // multiplier set here is consumed by the builder tally just below,
            // so this pass must run first.
            p.getCards().get(CardType.BUILDINGS)
                    .forEach(b -> b.activeEffect(Set.of(p), Trigger.END_GAME));
            int buildingEffectPP = p.getPp() - basePP;

            int builderPoints = p.getCards().get(CardType.BUILDER).stream()
                    .map(c -> (Builder)c)
                    .mapToInt(Builder::getPP)
                    .sum();
            int builderPP = builderPoints * p.getBuildingBonus().getBuilderMultiplierPP();
            p.changePP(builderPP);

            int inventorIcons = (int)p.getCards().get(CardType.INVENTOR).stream()
                    .map(c -> (Inventor)c)
                    .map(Inventor::getIcon)
                    .distinct()
                    .count();
            int numInventors = p.getCards().get(CardType.INVENTOR).size();
            int inventorPP = numInventors * inventorIcons;
            p.changePP(inventorPP);

            int numArtists = p.getCards().get(CardType.ARTIST).size();
            int artistPP = 10 * (numArtists / 2);
            p.changePP(artistPP);

            int buildingOwnPP = p.getCards().get(CardType.BUILDINGS).stream()
                    .map(c -> (Building)c)
                    .mapToInt(Building::getEndGamePP)
                    .sum();
            p.changePP(buildingOwnPP);

            // "Buildings" groups the cards' own end-game PP and their effects.
            int buildingsPP = buildingOwnPP + buildingEffectPP;
            // [TAG] at the start, signed tokens like the event log (see formatDeltas),
            // with " — " separating the player from the breakdown mid-message.
            String breakdown = "[FINAL SCORE] " + p.getName() + " — "
                    + signed(builderPP) + " builders "
                    + signed(inventorPP) + " inventors "
                    + signed(artistPP) + " artists "
                    + signed(buildingsPP) + " buildings "
                    + signed(basePP) + " base = " + p.getPp() + " PP";
            System.out.println(breakdown);
            getGame().sendUpdateGame(breakdown);
        }

        GameDAO gameDAO = getContext().getGameDAO();
        List<String> nicknames = players.stream()
                .map(Player::getName)
                .toList();
        List<Integer> scores = players.stream()
                .map(Player::getPp)
                .toList();

        List<MatchResult> results = players.stream()
                .map(p -> new MatchResult(p.getName(), p.getPp(), p.getFood()))
                .toList();

        try{
            gameDAO.saveMatch(getGame().getGameId(), players.size(), nicknames, scores);
            List<DBRecord> records = gameDAO.getLeaderboard(players.size());
            String log;
            if(records.isEmpty()){
                log = "Game launched without database — overall leaderboard unavailable, this match will not be recorded.";
            } else {
                log = "[END GAME] Game " + getGame().getGameId() + " results saved successfully.";
            }
            System.out.println(log);
            getGame().sendEndGameResults(results, records, log);
        } catch(SQLException e){
            System.err.println("ERROR: [END GAME] Failed to save results for game " + getGame().getGameId() + ": " + e.getMessage());
            throw new ServerException("Error during saving match results.");
        }
        return null;
    }
}
