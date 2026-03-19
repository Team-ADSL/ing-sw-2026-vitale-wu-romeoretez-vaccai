package org.example.model.card.building;

import org.example.model.card.Card;
import org.example.model.card.CardType;
import org.example.model.card.Trigger;
import org.example.model.card.character.Character;
import org.example.model.game.Player;

import java.util.Optional;
import java.util.Set;

public class DuringSustenance extends DuringEvent{
    private CardType typeMultiplier;
    public CardType getTypeMultiplier() {
        return typeMultiplier;
    }

    public DuringSustenance (int endGamePP, int cost, Trigger trigger,
                        Set<Character> characterUse, int era, Optional<Integer> numPlayers, CardType typeMultiplier) {
        super(endGamePP, cost, trigger, characterUse, era, numPlayers);

        this.typeMultiplier = typeMultiplier;

    }
    @Override
    public void execute(Set<Player> players, Trigger t) {
        if (t != Trigger.SUSTENANCE){
            return;
        }
        for (Player p : players){
            p.getBuildingBonus().setSustenanceDiscount(p.getCards().get(typeMultiplier).size());
        }
    }
}
