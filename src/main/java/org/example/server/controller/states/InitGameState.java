package org.example.server.controller.states;

import org.example.server.controller.GameController;
import org.example.server.model.cards.Card;
import org.example.server.model.cards.buildings.Building;
import org.example.server.model.Game;
import org.example.server.model.board.CardRow;
import org.example.server.model.board.Deck;
import org.example.server.config.BoardConfigLoader;
import org.example.shared.enums.Phase;

import java.util.*;
import java.util.stream.Collectors;

public class InitGameState extends ControllerState {

    public InitGameState(Game game, GameController context) {
        super(game, context);
    }

    @Override
    public ControllerState onEntry(){
        getGame().setPhase(Phase.INIT);
        if(getGame().isInitialized()){
            return nextState();
        }

        BoardConfigLoader boardConfigLoader = getContext().getBoardConfigLoader();
        // Initialization logic
        getGame().addObserver(getContext().getPersistenceManager());
        getGame().setInitialized(true);
        getGame().sendUpdateGame();
        return nextState();
    }

    @Override
    public ControllerState nextState() {
        return new TotemPlacementState(getGame(), getContext());
    }

    public int calcNumTopCard(int numPlayer){
        return numPlayer + 4;
    }
    public int calcNumLowCard(int numPlayer){
        return numPlayer + 1;
    }

    // init only
    public void fillLowRow(int numPlayers){
        int targetSize = numPlayers + 1;
        int cardsDrawn = 0;

        while (cardsDrawn < targetSize) {
            CardRow topRow = getGame().getBoard().getTopRow();
            CardRow lowRow = getGame().getBoard().getLowRow();

            Deck deck = getGame().getBoard().getDeck();

            if (deck.isEmpty()) {
                break;
            }

            // draw from the top of the deck and save in 'drawn'
            Card drawn = deck.drawCard();

            if (!drawn.canBeDrawn(null)) {
                // if event (canBeDrawn = false), send it to the top row
                topRow.add(drawn);
            } else {
                lowRow.add(drawn);
                cardsDrawn++;
            }
        }
    }

    // init only
    public void fillTopRow(int numPlayers){
        CardRow topRow = getGame().getBoard().getTopRow();
        Deck deck = getGame().getBoard().getDeck();

        int targetSize = numPlayers + 4;
        // cards
        int cardsToDraw = targetSize - topRow.size();

        for (int i = 0; i < cardsToDraw; i++) {
            // Stop drawing if the deck is empty
            if (deck.isEmpty()) {
                break;
            }
            // Draw from the top of the deck and add to the upper row
            topRow.add(deck.drawCard());
        }
    }

    public void makeBuildingDecks(Set<Building> buildings, int numPlayers){
        // Cards per era based on player count (rulebook step 6)
        int[] eraCounts = {0, 0, 0}; // index 0 = era1, 1 = era2, 2 = era3
        if (numPlayers == 2) { eraCounts = new int[]{1, 2, 3}; }
        else if (numPlayers == 3) { eraCounts = new int[]{2, 2, 4}; }
        else if (numPlayers == 4) { eraCounts = new int[]{2, 3, 4}; }
        else if (numPlayers == 5) { eraCounts = new int[]{2, 3, 5}; }

        // Separate buildings by era
        ArrayList<Building> era1 = new ArrayList<>();
        ArrayList<Building> era2 = new ArrayList<>();
        ArrayList<Building> era3 = new ArrayList<>();

        for (Building b : buildings) {
            if (b.getEra() == 1) era1.add(b);
            else if (b.getEra() == 2) era2.add(b);
            else if (b.getEra() == 3) era3.add(b);
        }

        // Shuffle each era independently
        Collections.shuffle(era1);
        Collections.shuffle(era2);
        Collections.shuffle(era3);

        // Pick only the required number of cards per era (rulebook step 6)
        ArrayList<Building> buildingDeckEra1 = new ArrayList<>(era1.subList(0, eraCounts[0]));
        Set<Building> buildingDeckEra2 = new HashSet<>(era2.subList(0, eraCounts[1]));
        Set<Building> buildingDeckEra3 = new HashSet<>(era3.subList(0, eraCounts[2]));
        Set<Card> buildingCardsEra2 = buildingDeckEra2.stream()
                .map(b -> (Card)b).collect(Collectors.toSet());
        Set<Card> buildingCardsEra3 = buildingDeckEra3.stream()
                .map(b -> (Card)b).collect(Collectors.toSet());

        // Keep era 2 (index 0) and era 3 (index 1) aside for later (rulebook step 6b)
        ArrayList<Set<Card>> remainingBuildings = getGame().getBoard().getRemainingBuildings();
        remainingBuildings.add(buildingCardsEra2); // index 0 = era 2
        remainingBuildings.add(buildingCardsEra3); // index 1 = era 3

        // Place era 1 buildings face up at the end of the top row (rulebook step 6a)
        CardRow topRow = getGame().getBoard().getTopRow();
        for (Building b : buildingDeckEra1) {
            topRow.add(b);
        }
    }
}
