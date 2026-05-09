package org.adsl.server.model.cards.buildings;

import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.cards.characters.Character;
import org.adsl.server.model.Player;
import org.adsl.shared.model.CardToken;

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



    @Override
    protected String getEffectsLabel() {
        return "+1 " + CardToken.EXTRA_MOVE + " " + CardToken.PP + getEndGamePP();
    }
}
