package org.example.model.card.building;

import org.example.model.card.Trigger;
import org.example.model.card.character.Character;
import org.example.model.game.Player;

import java.util.Optional;
import java.util.Set;

public class SinceBuilt extends Building {

    private Set<Character> characterInUse;
    //private BuiltingTypeCoutner (distinguere tra coppie di artist e full set)

    public SinceBuilt (int endGamePP, int cost, Trigger trigger,
                   Set<Character> characterInUse, int era, Optional<Integer> numPlayers) {
        super(endGamePP, cost, era, numPlayers);
        this.characterInUse = characterInUse;
    }


    @Override
    public void activeEffect(Set<Player> players, Trigger t) {
        // if per distringuere i due casi, poi conta i character nel set per capire se dare i punti
    }
}
