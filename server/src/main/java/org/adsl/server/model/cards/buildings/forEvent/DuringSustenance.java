package org.adsl.server.model.cards.buildings.forEvent;

import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.Player;
import org.adsl.shared.model.CardToken;

import java.util.Optional;
import java.util.Set;

/**
 * Building that reduces the food cost during the {@code Sustenance} event by
 * an amount equal to the number of cards the owner has of a specific character
 * type ({@code typeMultiplier}). The discount is accumulated in
 * {@code BuildingBonus.setSustenanceDiscount()} and applied in
 * {@code Sustenance.activeEffect()}.
 */
public class DuringSustenance extends DuringEvent {
    private final CardType typeMultiplier;

    public DuringSustenance(String id, int endGamePP, int cost, int era, Integer numPlayers, CardType typeMultiplier) {
        super(id, endGamePP, cost, era, numPlayers);
        this.typeMultiplier = typeMultiplier;
    }

    @Override
    public void execute(Set<Player> players, Trigger t) {
        if (t != Trigger.SUSTENANCE) {
            return;
        }
        Optional<Player> playerContainer = players.stream().findFirst();
        if (playerContainer.isEmpty()) {
            return; // ERRORE DA GESTIRE?
        }
        Player p = playerContainer.get();
        p.getBuildingBonus().setSustenanceDiscount(p.getCards().get(typeMultiplier).size());
    }

    public CardType getTypeMultiplier() {
        return typeMultiplier;
    }



    @Override
    protected String getEffectsLabel() {
        String charToken = switch (typeMultiplier) {
            case HUNTER   -> CardToken.HUNTER;
            case GATHERER -> CardToken.GATHERER;
            case BUILDER  -> CardToken.BUILDER;
            case SHAMAN   -> CardToken.SHAMAN;
            case ARTIST   -> CardToken.ARTIST;
            case INVENTOR -> CardToken.INVENTOR;
            default       -> "";
        };
        return CardToken.SUSTENANCE + ": -1" + CardToken.FOOD + "x" + charToken;
    }
}