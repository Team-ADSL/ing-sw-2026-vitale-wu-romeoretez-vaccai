package org.adsl.server.model.cards.characters;

import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.Player;

import java.util.Set;

/**
 * Abstract base for all character cards (Hunter, Gatherer, Builder, Artist,
 * Inventor, Shaman). Characters can always be drawn (no food cost) and have no
 * active effect by default; subclasses override {@link #activeEffect} when needed.
 */
public abstract class Character extends Card {

    /**
     * Creates a character card.
     *
     * @param id         unique card identifier
     * @param era        the era this card belongs to (1-3)
     * @param numPlayers minimum number of players required for this card to be in play, or {@code null} if always included
     */
    public Character (String id, int era, Integer numPlayers){
        super(id, era, numPlayers);
    }

    /**
     * Character cards have no food cost and are always drawable.
     *
     * @param p the player attempting to draw (ignored)
     * @return always {@code true}
     */
    @Override
    public boolean canBeDrawn(Player p) {
        return true;
    }

    @Override
    public void activeEffect(Set<Player> players, Trigger t) {}

    @Override
    public int getCost(){ return 0; }
}