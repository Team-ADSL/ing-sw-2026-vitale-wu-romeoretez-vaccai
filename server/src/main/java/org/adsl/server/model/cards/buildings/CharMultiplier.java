package org.adsl.server.model.cards.buildings;

import org.adsl.server.model.Player;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Trigger;
import org.adsl.shared.model.CardToken;

import java.util.Set;

/**
 * Building whose effect fires once at the end of the game ({@link Trigger#END_GAME}).
 * {@code END_CHARACTER_MULTIPLIER} – grants PP per card x multiplier of a specific character type.
 */
public class CharMultiplier extends Building {
    private final CardType typeMul;
    private final int multiplier;

    /**
     * Creates a CharMultiplier building card.
     *
     * @param id         unique card identifier
     * @param endGamePP  prestige points scored at end-game for owning this building
     * @param cost       food cost to acquire this building
     * @param era        the era this card belongs to (1-3)
     * @param numPlayers minimum number of players required for this card to be in play, or {@code null} if always included
     * @param typeMul    the character {@link CardType} whose count is multiplied
     * @param multiplier the per-card PP multiplier applied to {@code typeMul} cards at end-game
     */
    public CharMultiplier(String id, int endGamePP, int cost, int era, Integer numPlayers, CardType typeMul, int multiplier) {
        super(id, endGamePP, cost, era, numPlayers);
        this.typeMul = typeMul;
        this.multiplier = multiplier;
    }

    /**
     * At end-game ({@link Trigger#END_GAME}), grants the owner {@code multiplier}
     * prestige points for each card of type {@code typeMul} in their hand.
     *
     * @param players the affected players (expects a singleton containing the owner)
     * @param t       the trigger that fired
     */
    @Override
    public void activeEffect(Set<Player> players, Trigger t) {
        if(t == Trigger.END_GAME){
            players.stream()
                    .findFirst()
                    .ifPresent(p -> p.changePP(p.getCards().get(typeMul).size() * multiplier));
        }
    }

    @Override
    protected String getEffectsLabel() {
        return CardToken.ENDGAME + " +" + multiplier + "x" + "[" + typeMul + "]";
    }
}
