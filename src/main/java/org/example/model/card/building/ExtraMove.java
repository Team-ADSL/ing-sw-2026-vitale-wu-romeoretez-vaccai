package org.example.model.card.building;

import org.example.model.card.Trigger;
import org.example.model.card.character.Character;
import org.example.model.game.Player;

import java.util.Optional;
import java.util.Set;

public class ExtraMove extends Building {

    public ExtraMove(int endGamePP, int cost, Trigger trigger,
                     Set<Character> characterUse, int era, Optional<Integer> numPlayers) {
        super(endGamePP, cost, era, numPlayers);
    }

    @Override
    public void activeEffect(Set<Player> players, Trigger t) {
        // Semplice set di un attributo di BuildingBonus a true (viene chiamato dal controller a fine round)
    }
}
