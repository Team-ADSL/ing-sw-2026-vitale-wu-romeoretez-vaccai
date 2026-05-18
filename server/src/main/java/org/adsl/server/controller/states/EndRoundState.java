package org.adsl.server.controller.states;

import org.adsl.server.controller.GameController;
import org.adsl.shared.enums.Phase;
import org.adsl.server.model.cards.Card;
import org.adsl.server.model.Game;
import org.adsl.server.model.board.CardRow;
import org.adsl.server.model.board.Deck;

import java.util.ArrayList;
import java.util.Set;

/**
 * Automatic state that executes end-of-round board maintenance.
 * <p>
 * Moves cards from the upper row to the lower row, refills the upper row from
 * the deck, and handles era transitions (swapping building rows when the deck
 * crosses an era boundary). Always transitions to {@link TotemPlacementState}
 * when done.
 * </p>
 */
public class EndRoundState extends ControllerState {

    public EndRoundState(Game game, GameController context) {
        super(game, context);
    }

    /** Mesos has exactly three eras; advancing past era 3 is invalid. */
    private static final int MAX_ERA = 3;

    @Override
    public ControllerState onEntry() {
        CardRow lowRow = getGame().getBoard().lowRow();
        CardRow topRow = getGame().getBoard().topRow();

        // Move cards from top to low row
        lowRow.clearTribeCards();
        ArrayList<Card> cardsToMove = topRow.getTribeCards();
        topRow.clearTribeCards();
        lowRow.addTribeCards(cardsToMove);

        // Re-fill top row, but never draw past the bottom of the deck.
        Deck deck = getGame().getBoard().deck();
        for (int i = 0; i < topRow.getNumTribeCard() && !deck.isEmpty(); i++) {
            topRow.add(deck.drawCard());
        }

        // Handle new era. Skip the whole branch if the deck is empty (nothing
        // left to inspect) or if we are already in the last era — there is no
        // era 4 in Mesos and no further building deck to swap in.
        if (!deck.isEmpty()
                && getGame().getEra() < MAX_ERA
                && deck.isNewEra(getGame().getEra())) {
            getGame().changeEra();
            System.out.println("[END ROUND] New era started: " + getGame().getEra());

            if (getGame().getEra() == 3) {
                lowRow.clearBuildings();
            }

            // Move top buildings
            Set<Card> buildingToMove = topRow.getBuildings();
            topRow.clearBuildings();
            lowRow.addBuildings(buildingToMove);

            // Refill top buildings from the next era's pile, when available.
            if (!getGame().getBoard().remainingBuildings().isEmpty()) {
                Set<Card> newBuildings = getGame().getBoard().remainingBuildings().removeFirst();
                topRow.addBuildings(newBuildings);
            }
        }
        getGame().changeRound();
        String log = "[END ROUND] Round " + getGame().getRound() + ", Era " + getGame().getEra();
        System.out.println(log);
        setNextState(calcNextState());
        getGame().sendUpdateGame(log);
        return getNextState();
    }

    @Override
    public ControllerState calcNextState() {
        getGame().setPhase(Phase.TOTEM_PLACEMENT);
        return new TotemPlacementState(getGame(), getContext());
    }
}
