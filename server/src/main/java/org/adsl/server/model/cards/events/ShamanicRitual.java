package org.adsl.server.model.cards.events;

import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.cards.buildings.utils.BuildingBonus;
import org.adsl.server.model.cards.characters.Shaman;
import org.adsl.server.model.Player;
import org.adsl.shared.model.CardToken;

import java.util.*;

/**
 * Event card: players compare their total shaman-star count (sum of
 * {@link Shaman#getStarNum()} plus any {@link BuildingBonus#getExtraStars()}
 * bonus). The player(s) with the most stars gain {@code gainedPP} PP; the
 * player(s) with the fewest lose {@code lostPP} PP (unless immune via
 * {@code DuringRitual}). If all players are tied no PP changes occur.
 * A player who is alone at the top and owns a {@code DuringRitual}
 * {@code RITUAL_DOUBLE_PP} building doubles their gain.
 */
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
        return CardToken.RITUAL + " " + eraToRoman(getEra());
    }

    @Override
    protected String getEffectsLabel() {
        return ">" + CardToken.SHAMAN_STAR + ":" + gainedPP + CardToken.PP + "," + "<" + CardToken.SHAMAN_STAR + ":-" + lostPP + CardToken.PP;
    }

    @Override
    public String getEventTitle() {
        return "SHAMANIC RITUAL";
    }
}
