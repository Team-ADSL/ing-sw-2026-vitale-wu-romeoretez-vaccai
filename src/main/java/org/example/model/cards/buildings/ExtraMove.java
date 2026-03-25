package org.example.model.cards.buildings;

import org.example.shared.enums.Trigger;
import org.example.model.cards.characters.Character;
import org.example.model.Player;

import java.util.Optional;
import java.util.Set;

public class ExtraMove extends Building {

    public ExtraMove(int endGamePP, int cost, Trigger trigger,
                     Set<Character> characterUse, int era, Optional<Integer> numPlayers) {
        super(endGamePP, cost, era, numPlayers);
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
