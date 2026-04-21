package org.adsl.server.controller.states;

import org.adsl.server.config.BoardConfigLoader;
import org.adsl.server.config.GameSettings;
import org.adsl.server.controller.GameController;
import org.adsl.server.model.cards.Card;
import org.adsl.server.model.cards.buildings.Building;
import org.adsl.server.model.Game;
import org.adsl.server.model.board.Board;
import org.adsl.server.model.board.CardRow;
import org.adsl.server.model.board.Deck;
import org.adsl.shared.enums.Phase;
import org.adsl.shared.enums.Totem;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        GameSettings gameSettings = loader.getSettings(numPlayers);
        int maxBuildings = maxBuildings(gameSettings);

        Board board = new Board(
                gameSettings.numLowTribeCard() + maxBuildings,
                gameSettings.numLowTribeCard(),
                gameSettings.numTopTribeCard() + maxBuildings,
                gameSettings.numTopTribeCard(),
                loader.getOfferTrack(numPlayers),
                loader.getOrderTile(numPlayers),
                Deck.createDeck(loader.getCards(numPlayers))
        );
        getGame().setBoard(board);

        makeBuildingDecks(loader.getBuildings(), numPlayers, gameSettings);
        fillLowRow(numPlayers);
        fillTopRow(numPlayers);

        List<Totem> shuffledTotems = new ArrayList<>(Arrays.asList(Totem.values()));
        Collections.shuffle(shuffledTotems);
        Iterator<Totem> totemIterator = shuffledTotems.iterator();
        getGame().getPlayers().forEach(p -> {
            if (totemIterator.hasNext()) {
                p.setColor(totemIterator.next());
            }
        });
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

    public int maxBuildings(GameSettings gameSettings){
        return Stream.of(
                        gameSettings.numBuildingEra1(),
                        gameSettings.numBuildingEra2(),
                        gameSettings.numBuildingEra3()
                )
                .max(Integer::compare)
                .orElse(0);
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
        for (int i = 0; i < targetSize; i++) {
            if (deck.isEmpty()) {
                break;
            }
            topRow.add(deck.drawCard());
        }
    }

    public void makeBuildingDecks(Set<Building> buildings, int numPlayers, GameSettings gameSettings) {
        int[] eraCounts = {gameSettings.numBuildingEra1(), gameSettings.numBuildingEra2(), gameSettings.numBuildingEra3()};
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
