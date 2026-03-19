package org.example.model.card.event;

import org.example.model.card.Card;
import org.example.model.card.CardType;
import org.example.model.card.Trigger;
import org.example.model.game.Player;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ShamanicRitual extends Event {

    private int lostPP;
    private int gainedPP;

    public ShamanicRitual (int lostPP, int gainedPP, boolean isFinal,
                       int era, Optional<Integer> numPlayers) {

        super(isFinal, era, numPlayers);
        this.lostPP = lostPP;
        this.gainedPP = gainedPP;

    }

    @Override
    public void activeEffect(Set<Player> players, Trigger t) {
        // Check dei building
        // Player1 Starts = stars del player + bonusStars
        // Reset bousBuilding
    }
}
