package org.example.server.controller.states;

import org.example.server.config.BoardConfigLoader;
import org.example.server.controller.GameController;
import org.example.server.model.cards.Card;
import org.example.server.model.cards.buildings.Building;
import org.example.server.model.Game;
import org.example.server.model.board.Board;
import org.example.server.model.board.CardRow;
import org.example.server.model.board.Deck;
import org.example.shared.enums.Phase;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class InitGameState extends ControllerState {

    public InitGameState(Game game, GameController context) {
        super(game, context);
    }

    @Override
    public ControllerState onEntry(){
        if(getGame().isInitialized()){
            return calcNextState();
        }

        int numPlayers = getGame().getPlayers().size();
        BoardConfigLoader loader = getContext().getBoardConfigLoader();

        Board board = new Board(
                calcNumLowCard(numPlayers),
                calcNumLowTribeCard(numPlayers),
                calcNumTopCard(numPlayers),
                calcNumTopTribeCard(numPlayers),
                loader.getOfferTrack(numPlayers),
                loader.getOrderTile(numPlayers),
                loader.getCards(numPlayers)
        );
        getGame().setBoard(board);

        makeBuildingDecks(loader.getBuildings(), numPlayers);
        fillLowRow(numPlayers);
        fillTopRow(numPlayers);
        board.orderTile().placePlayersRandom(getGame().getPlayers());

        getGame().setInitialized(true);
        getGame().addObserver(getContext().getPersistenceManager());
        setNextState(calcNextState());
        getGame().sendUpdateGame();
        return getNextState();
    }

    @Override
    public ControllerState calcNextState() {
        getGame().setPhase(Phase.TOTEM_PLACEMENT);
        return new TotemPlacementState(getGame(), getContext());
    }

    public int calcNumTopCard(int numPlayer){
        int maxBuildings = numPlayer == 4 || numPlayer == 5 ? numPlayer : numPlayer + 1;
        return calcNumTopTribeCard(numPlayer) + maxBuildings;
    }
    public int calcNumLowCard(int numPlayer){
        int maxBuildings = numPlayer == 4 || numPlayer == 5 ? numPlayer : numPlayer + 1;
        return calcNumLowTribeCard(numPlayer) + maxBuildings;
    }
    public int calcNumTopTribeCard(int numPlayer){
        return numPlayer + 4;
    }
    public int calcNumLowTribeCard(int numPlayer){
        return numPlayer + 1;
    }

    public void fillLowRow(int numPlayers){
        int targetSize = numPlayers + 1;
        int cardsDrawn = 0;

        while (cardsDrawn < targetSize) {
            CardRow topRow = getGame().getBoard().topRow();
            CardRow lowRow = getGame().getBoard().lowRow();

            Deck deck = getGame().getBoard().deck();
            if (deck.isEmpty()) {
                break;
            }

            Card drawn = deck.drawCard();
            if (!drawn.canBeDrawn(null)) {
                topRow.add(drawn);
            } else {
                lowRow.add(drawn);
                cardsDrawn++;
            }
        }
    }

    public void fillTopRow(int numPlayers){
        CardRow topRow = getGame().getBoard().topRow();
        Deck deck = getGame().getBoard().deck();

        int targetSize = numPlayers + 4;
        int cardsToDraw = targetSize - topRow.size();

        for (int i = 0; i < cardsToDraw; i++) {
            if (deck.isEmpty()) {
                break;
            }
            topRow.add(deck.drawCard());
        }
    }

    public void makeBuildingDecks(Set<Building> buildings, int numPlayers) {
        int[] eraCounts = switch (numPlayers) {
            case 2 -> new int[]{1, 2, 3};
            case 3 -> new int[]{2, 2, 4};
            case 4 -> new int[]{2, 3, 4};
            case 5 -> new int[]{2, 3, 5};
            default -> new int[]{0, 0, 0};
        };

        Map<Integer, List<Building>> buildingsByEra = buildings.stream()
                .collect(Collectors.groupingBy(Building::getEra));

        Function<Integer, Set<Card>> prepareEra = eraIndex -> {
            int eraNumber = eraIndex + 1;
            List<Building> eraList = buildingsByEra.getOrDefault(eraNumber, List.of());

            List<Building> shuffled = new ArrayList<>(eraList);
            Collections.shuffle(shuffled);

            return shuffled.stream()
                    .limit(eraCounts[eraIndex])
                    .map(b -> (Card) b)
                    .collect(Collectors.toSet());
        };

        Set<Card> deckEra1 = prepareEra.apply(0);
        Set<Card> deckEra2 = prepareEra.apply(1);
        Set<Card> deckEra3 = prepareEra.apply(2);

        Board board = getGame().getBoard();
        board.remainingBuildings().addAll(List.of(deckEra2, deckEra3));
        board.topRow().addBuildings(deckEra1);
    }
}
