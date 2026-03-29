package org.example.server.controller.states;

import org.example.shared.enums.Phase;
import org.example.shared.enums.CardType;
import org.example.shared.enums.Trigger;
import org.example.server.model.cards.buildings.Building;
import org.example.server.model.cards.characters.Builder;
import org.example.server.model.cards.characters.Inventor;
import org.example.server.model.Game;
import org.example.server.model.Player;

import java.util.Set;

public class EndGameState extends ControllerState {
    public EndGameState(Game game) {
        super(game);
    }

    @Override
    public ControllerState onEntry(){
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
        return nextState();
    }

    @Override
    public ControllerState nextState() {
        return null;
    }
}
