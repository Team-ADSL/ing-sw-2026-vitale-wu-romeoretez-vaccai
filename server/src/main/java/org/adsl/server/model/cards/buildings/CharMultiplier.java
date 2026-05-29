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

    public CharMultiplier(String id, int endGamePP, int cost, int era, Integer numPlayers, CardType typeMul, int multiplier) {
        super(id, endGamePP, cost, era, numPlayers);
        this.typeMul = typeMul;
        this.multiplier = multiplier;
    }

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
