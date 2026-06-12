package org.adsl.server.model.board;

import org.adsl.server.model.cards.Card;
import org.adsl.shared.model.CardDTO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A fixed-size row of cards on the game board, split into two logical regions:
 * <ul>
 *   <li>Indices {@code 0 .. numTribeCards-1} — tribe cards (characters and events).</li>
 *   <li>Indices {@code numTribeCards .. size-1} — building cards.</li>
 * </ul>
 * Null slots represent empty positions.
 */
public class CardRow implements Serializable {
    private final ArrayList<Card> cards;
    private final int numTribeCards;

    /**
     * Creates a row backed by the given list of cards.
     *
     * @param cards         initial card slots (may contain {@code null}s for empty positions)
     * @param numTribeCards number of leading slots reserved for tribe cards
     */
    public CardRow(ArrayList<Card> cards, int numTribeCards) {
        this.cards = cards;
        this.numTribeCards = numTribeCards;
    }

    /**
     * Creates an empty row of {@code numCard} slots, all initially {@code null}.
     *
     * @param numCard       total number of slots in the row
     * @param numTribeCards number of leading slots reserved for tribe cards
     */
    public CardRow(int numCard, int numTribeCards) {
        this.cards = new ArrayList<>(numCard);
        for (int i = 0; i < numCard; i++) cards.add(null);
        this.numTribeCards = numTribeCards;
    }

    public int size(){
        return cards.size();
    }

    /**
     * Removes and returns the card at {@code index}, leaving a {@code null} slot.
     *
     * @param index zero-based position in the row
     * @return the card that was at that position (may be {@code null})
     */
    public Card pickCardAt(int index){
        Card toReturn = cards.get(index);
        cards.set(index, null);
        return toReturn;
    }

    public Card getCardAt(int index) {
        return cards.get(index);
    }

    /**
     * Places {@code card} in the first empty (null) slot of the row, scanning
     * from index 0. Does nothing if the row is already full.
     *
     * @param card the card to add
     */
    public void add(Card card) {
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i) == null) {
                cards.set(i, card);
                return;
            }
        }
    }

    /**
     * Returns a copy of the tribe-card slots (indices {@code 0 .. numTribeCards-1}).
     *
     * @return a new list containing the current tribe cards (may include {@code null}s)
     */
    public ArrayList<Card> getTribeCards() {
        return cards.stream()
                .limit(numTribeCards)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Replaces the tribe-card slots with {@code newCards}, used when refreshing
     * the row at the start of a round.
     *
     * @param newCards new cards for indices {@code 0 .. numTribeCards-1}
     */
    public void addTribeCards(ArrayList<Card> newCards) {
        for(int i=0; i < numTribeCards; i++){
            cards.set(i, newCards.get(i));
        }
    }

    /** Sets all tribe-card slots to {@code null}, e.g. before a row refresh. */
    public void clearTribeCards(){
        for (int i = 0; i < numTribeCards; i++) {
            cards.set(i, null);
        }
    }

    /**
     * Returns the set of non-null building cards currently in the row
     * (indices {@code numTribeCards .. size-1}).
     *
     * @return the building cards present on this row
     */
    public Set<Card> getBuildings(){
        Set<Card> buildings = new HashSet<>();
        for (int i = numTribeCards; i < cards.size(); i++) {
            Card newCard = cards.get(i);
            if(newCard != null){
                buildings.add(newCard);
            }
        }
        return buildings;
    }

    /** Sets all building slots to {@code null}, e.g. when buildings move to the lower row. */
    public void clearBuildings(){
        for (int i = numTribeCards; i < cards.size(); i++) {
            cards.set(i, null);
        }
    }

    /**
     * Fills the building slots (starting at {@code numTribeCards}) with
     * {@code newCards}, one card per slot in iteration order.
     *
     * @param newCards building cards to place on the row
     */
    public void addBuildings(Set<Card> newCards) {
        int i = numTribeCards;
        for(Card c : newCards){
            cards.set(i, c);
            i++;
        }
    }

    /**
     * Converts this row to its DTO representation for sending to clients.
     *
     * @return a list of {@link CardDTO}s (or {@code null} entries for empty slots),
     *         in the same order as the row's slots
     */
    public ArrayList<CardDTO> createDTO(){
        return cards.stream()
                .map(c -> c != null ? c.createDTO() : null)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public int getNumTribeCard() {
        return numTribeCards;
    }
}
