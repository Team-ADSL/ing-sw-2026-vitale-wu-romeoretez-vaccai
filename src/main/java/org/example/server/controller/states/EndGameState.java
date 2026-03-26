package org.example.server.controller.states;

import org.example.shared.utils.Move;
import org.example.shared.exceptions.InvalidMoveException;
import org.example.shared.enums.CardType;
import org.example.shared.enums.Trigger;
import org.example.server.model.cards.buildings.Building;
import org.example.server.model.cards.characters.Builder;
import org.example.server.model.cards.characters.Inventor;
import org.example.server.model.Game;
import org.example.server.model.Player;

import java.util.Set;

public class EndGameState extends State {
    public EndGameState(Game game) {
        super(game);
    }

    @Override
    public State onEntry(){
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
        return nextState();
    }

    @Override
    public void checkMove(Set<Move> moves, Player p) throws InvalidMoveException {
        throw new InvalidMoveException("Automatic state: EndGame execution. No action allowed");
    }

    @Override
    public void execute(Set<Move> moves, Player p) {}

    @Override
    public State nextState() {
        return null;
    }
}
