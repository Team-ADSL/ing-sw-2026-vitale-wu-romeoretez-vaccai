package org.adsl.server.model.cards.events;

import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.cards.buildings.utils.BuildingBonus;
import org.adsl.server.model.Player;
import org.adsl.shared.model.CardToken;

import java.util.Map;
import java.util.Set;

public class CavePaintings extends Event {

    private final int minArtists;
    private final int lostPP; //just use 2 instead of variable???
    private final int multiplierPP;

    public CavePaintings (String id, int minArtists, int lostPP, int multiplierPP,
                      boolean isFinal, int era, Integer numPlayers) {
        super(id, isFinal, era, numPlayers);
        this.minArtists = minArtists;
        this.lostPP = lostPP;
        this.multiplierPP = multiplierPP;
    }

    @Override
    public void insert(Map<CardType, Set<Card>> cards) {
        if(cards.containsKey(CardType.CAVE_PAINTINGS)) cards.get(CardType.CAVE_PAINTINGS).add(this);
    }

    @Override
    public void activeEffect(Set<Player> players, Trigger t) {
        if (t == Trigger.EVENT_EXECUTION) {
            for (Player p : players) {
                activateBuildings(p, Trigger.CAVE_PAINTINGS);
                BuildingBonus bonus = p.getBuildingBonus();
                int artistCount = p.getCards().get(CardType.ARTIST).size();
                if (artistCount >= minArtists) {
                    p.changePP(artistCount * multiplierPP);
                }
                else {
                    p.changePP(-lostPP);
                }
                if (p.getBuildingBonus().isArtistFood()) {
                    p.changeFood(artistCount);
                }
                bonus.reset();
            }
        }
    }

    @Override
    protected String getTypeLabel() {
        return CardToken.PAINTINGS + " " + eraToRoman(getEra());
    }

    @Override
    protected String getEffectsLabel() {
        return ">" + minArtists + "A:+" + multiplierPP + CardToken.PP + "xA" + "|-" + lostPP + CardToken.PP + "xA";
    }
}