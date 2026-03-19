package org.example.model.card.building;

import org.example.model.card.Trigger;
import org.example.model.card.character.Character;
import org.example.model.game.Player;

import java.util.Optional;
import java.util.Set;

public class DuringHunt extends DuringEvent{
    public DuringHunt(int endGamePP, int cost, Trigger trigger, Set<Character> characterUse, int era, Optional<Integer> numPlayers) {
        super(endGamePP, cost, trigger, characterUse, era, numPlayers);
    }

    @Override
    public void execute(Set<Player> players, Trigger t) {
        if (t!=Trigger.HUNT){
            return;
        }
        for (Player p : players) {
            p.getBuildingBonus().setHunterFood(1);
            p.getBuildingBonus().setHunterPP(1);
        }
    }
}
