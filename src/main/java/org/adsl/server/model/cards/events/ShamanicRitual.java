package org.adsl.server.model.cards.events;

import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.cards.buildings.utils.BuildingBonus;
import org.adsl.server.model.cards.characters.Shaman;
import org.adsl.server.model.Player;

import java.util.*;

public class ShamanicRitual extends Event {

    private final int lostPP;
    private final int gainedPP;

    public ShamanicRitual (String id, int lostPP, int gainedPP, boolean isFinal,
                       int era, Integer numPlayers) {
        super(id, isFinal, era, numPlayers);
        this.lostPP = lostPP;
        this.gainedPP = gainedPP;
    }

    @Override
    public void insert(Map<CardType, Set<Card>> cards) {
        if(cards.containsKey(CardType.SHAMANIC_RITUAL)) cards.get(CardType.SHAMANIC_RITUAL).add(this);
    }

    @Override
    public void activeEffect(Set<Player> players, Trigger t) {
        if (t == Trigger.EVENT_EXECUTION) {
            Map<Player, Integer> starMap = new HashMap<>();
            for (Player p : players) {
                activateBuildings(p, Trigger.SHAMANIC_RITUAL);
                BuildingBonus bonus = p.getBuildingBonus();
                int totalStars = p.getCards().get(CardType.SHAMAN).stream()
                        .map(card -> (Shaman) card)
                        .mapToInt(Shaman::getStarNum)
                        .sum() + bonus.getExtraStars();
                starMap.put(p, totalStars);
            }
            int maxStars = Collections.max(starMap.values());
            int minStars = Collections.min(starMap.values());
            if (maxStars == minStars) {
                players.forEach(p -> p.getBuildingBonus().reset());
                return;
            }
            long playersAtMax = starMap.values().stream().filter(s -> s == maxStars).count();
            for (Player p : players) {
                BuildingBonus bonus = p.getBuildingBonus();
                int stars = starMap.get(p);
                if (stars == maxStars) {
                    boolean aloneAtTop = playersAtMax == 1;
                    int multiplier = aloneAtTop ? bonus.getShamanMulitiplierPP() : 1;
                    p.changePP(gainedPP * multiplier);
                } else if (stars == minStars) {
                    if (!bonus.isNoRitualLostPP()) {
                        p.changePP(-lostPP);
                    }
                }
                bonus.reset();
            }
        }
    }

    @Override
    protected String getTypeLabel() {
        return "🎭 " + eraToRoman(getEra());
    }

    @Override
    protected String getEffectsLabel() {
        return "🌟-" + lostPP + "/+" + gainedPP;
    }
}
