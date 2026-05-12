package org.adsl.server.model.cards.events;

import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.cards.buildings.utils.BuildingBonus;
import org.adsl.server.model.Player;
import org.adsl.shared.model.CardToken;

import java.util.Map;
import java.util.Set;

public class Sustenance extends Event {

    private final int lostPP;

    public Sustenance(String id, int lostPP, boolean isFinal, int era, Integer numPlayers) {
        super(id, isFinal, era, numPlayers);
        this.lostPP = lostPP;
    }

    public int getLostPP() {
        return lostPP;
    }

    @Override
    public void insert(Map<CardType, Set<Card>> cards) {
        if(cards.containsKey(CardType.SUSTENANCE)) cards.get(CardType.SUSTENANCE).add(this);
    }

    @Override
    public void activeEffect(Set<Player> players, Trigger t) {
        if (t == Trigger.EVENT_EXECUTION) {
            for (Player p : players) {
                activateBuildings(p, Trigger.SUSTENANCE);
                BuildingBonus bonus = p.getBuildingBonus();
                int totalCharacters = p.getCards().entrySet().stream()
                        .filter(e -> e.getKey() != CardType.BUILDINGS)
                        .mapToInt(e -> e.getValue().size()).sum();
                int discount = p.getCards().get(CardType.GATHERER).size() * 3 + bonus.getSustenanceDiscount();
                int foodToPay = Math.max(0, totalCharacters - discount);
                int foodAvailable = p.getFood();
                int actualFoodPaid = Math.min(foodAvailable, foodToPay);
                p.changeFood(-actualFoodPaid);
                int unfedCharacters = foodToPay - actualFoodPaid;
                p.changePP(-(unfedCharacters * lostPP));
                bonus.reset();
            }
        }
    }

    @Override
    protected String getTypeLabel() {
        return CardToken.SUSTENANCE + " " + eraToRoman(getEra());
    }

    @Override
    protected String getEffectsLabel() {
        return "-" + CardToken.FOOD + "/" + lostPP + CardToken.PP + "x" + CardToken.SET;
    }
}
