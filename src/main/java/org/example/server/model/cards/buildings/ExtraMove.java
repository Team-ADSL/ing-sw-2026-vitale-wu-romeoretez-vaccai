package org.example.server.model.cards.buildings;

import org.example.shared.enums.Trigger;
import org.example.server.model.cards.characters.Character;
import org.example.server.model.Player;

import java.util.Set;

public class ExtraMove extends Building {

    public ExtraMove(String id, int endGamePP, int cost, Trigger trigger,
                     Set<Character> characterUse, int era, Integer numPlayers) {
        super(id, endGamePP, cost, era, numPlayers);
    }

    @Override
    public void activeEffect(Set<Player> players, Trigger t) {
        if(t == Trigger.END_ROUND){
            players.stream().findFirst().ifPresent(p -> {
                p.getBuildingBonus().setExtraMove(true);
            });
        }
    }
}
