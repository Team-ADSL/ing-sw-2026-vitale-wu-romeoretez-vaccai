package org.adsl.server.model.cards.events;

import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.cards.buildings.utils.BuildingBonus;
import org.adsl.server.model.Player;
import org.adsl.shared.model.CardToken;

import java.util.Map;
import java.util.Set;

/**
 * Event card: players with at least {@code minArtists} {@code Artist} cards gain
 * {@code multiplierPP} PP per Artist; players below the threshold lose
 * {@code lostPP} PP. Players with a {@code DuringPaintings} building additionally
 * gain 1 food per Artist.
 */
public class CavePaintings extends Event {

    private final int minArtists;
    private final int lostPP; //just use 2 instead of variable???
    private final int multiplierPP;

    /**
     * Creates a Cave Paintings event card.
     *
     * @param id           unique card identifier
     * @param minArtists   minimum number of Artist cards a player must own to gain PP
     *                      instead of losing it
     * @param lostPP       PP lost by players below the threshold
     * @param multiplierPP PP gained per Artist by players at or above the threshold
     * @param isFinal      {@code true} if this is the era-3/round-10 copy of the event
     * @param era          era this card belongs to
     * @param numPlayers   number of players in the match
     */
    public CavePaintings (String id, int minArtists, int lostPP, int multiplierPP,
                      boolean isFinal, int era, Integer numPlayers) {
        super(id, isFinal, era, numPlayers);
        this.minArtists = minArtists;
        this.lostPP = lostPP;
        this.multiplierPP = multiplierPP;
    }

    /**
     * Adds this card to the {@link CardType#CAVE_PAINTINGS} set, if present in
     * {@code cards}.
     *
     * @param cards the deck/board map of cards keyed by type
     */
    @Override
    public void insert(Map<CardType, Set<Card>> cards) {
        if(cards.containsKey(CardType.CAVE_PAINTINGS)) cards.get(CardType.CAVE_PAINTINGS).add(this);
    }

    /**
     * Resolves the Cave Paintings event for each player when {@code t} is
     * {@link Trigger#EVENT_EXECUTION}. For each player, first activates any
     * {@code DuringPaintings} building (via {@link #activateBuildings}), then
     * compares the player's Artist count to {@code minArtists}: players above
     * the threshold gain {@code multiplierPP} PP per Artist, others lose
     * {@code lostPP} PP. If the player has the {@code DuringPaintings} bonus
     * active, they additionally gain 1 food per Artist. The per-player
     * {@code BuildingBonus} is reset at the end of processing.
     *
     * @param players the players affected by the event
     * @param t       the trigger that fired; only {@link Trigger#EVENT_EXECUTION} is handled
     */
    @Override
    public void activeEffect(Set<Player> players, Trigger t) {
        if (t == Trigger.EVENT_EXECUTION) {
            for (Player p : players) {
                activateBuildings(p, Trigger.CAVE_PAINTINGS);
                BuildingBonus bonus = p.getBuildingBonus();
                int artistCount = p.getCards().get(CardType.ARTIST).size();
                if (artistCount > minArtists) {
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

    @Override
    public String getEventTitle() {
        return "CAVE PAINTINGS";
    }
}