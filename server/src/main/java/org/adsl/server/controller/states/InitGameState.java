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

/**
 * Automatic state that initialises the game board at the start of the first
 * round, executed once per game after all players have picked their totems.
 * <p>
 * Builds the {@link Board} from the loaded configuration, creates the three
 * era building decks, fills both card rows, places players on the order tile in
 * random order before transition to {@link TotemPlacementState}.
 * </p>
 * <p>
 * If the game is already marked as initialised (recovered from persistence) the
 * board setup is skipped and the state transitions immediately.
 * </p>
 */
public class InitGameState extends ControllerState {

    public InitGameState(Game game, GameController context) {
        super(game, context);
    }

    /**
     * Builds the {@link Board} (rows, decks, order tile) from the loaded
     * configuration, places players on the order tile in random order, grants
     * each player their starting food bonus based on order position, and marks
     * the game as initialised. Skipped if the game was recovered already
     * initialised.
     *
     * @return the next state, computed via {@link #calcNextState()}
     */
    @Override
    public ControllerState onEntry(){
        if(getGame().isInitialized()){
            return calcNextState();
        }

        int numPlayers = getGame().getPlayers().size();
        BoardConfigLoader loader = getContext().getBoardConfigLoader();
        GameSettings gameSettings = loader.getSettings(numPlayers);
        int maxBuildings = maxBuildings(gameSettings);
        int numRowCard = gameSettings.numTopTribeCard() + maxBuildings;

        Board board = new Board(
                numRowCard,
                gameSettings.numTopTribeCard(),
                numRowCard,
                gameSettings.numTopTribeCard(),
                loader.getOfferTrack(numPlayers),
                loader.getOrderTile(numPlayers),
                Deck.createDeck(loader.getCards(numPlayers))
        );
        getGame().setBoard(board);

        makeBuildingDecks(loader.getBuildings(), numPlayers, gameSettings);
        fillLowRow(gameSettings.numLowTribeCard());
        fillTopRow(gameSettings.numTopTribeCard());
        board.orderTile().placePlayersRandom(getGame().getPlayers());

        int[] foodBonus = {2, 3, 3, 4, 4};
        for (int i = 0; i < numPlayers; i++) {
            int bonus = foodBonus[i];
            board.orderTile().getPlayerAt(i).ifPresent(p -> p.changeFood(bonus));
        }

        getGame().setInitialized(true);
        setNextState(calcNextState());
        String log = "[INIT] Game config loaded.";
        System.out.println(log);
        getGame().sendUpdateGame(log);
        return getNextState();
    }

    /**
     * @return a new {@link TotemPlacementState}, always entered after board setup
     */
    @Override
    public ControllerState calcNextState() {
        getGame().setPhase(Phase.TOTEM_PLACEMENT);
        return new TotemPlacementState(getGame(), getContext());
    }

    /**
     * @param gameSettings the settings for the current player count
     * @return the largest building-deck size across the three eras
     */
    public int maxBuildings(GameSettings gameSettings){
        return Stream.of(
                        gameSettings.numBuildingEra1(),
                        gameSettings.numBuildingEra2(),
                        gameSettings.numBuildingEra3()
                )
                .max(Integer::compare)
                .orElse(0);
    }

    /**
     * Draws cards from the deck into the lower row until it reaches
     * {@code targetSize} or the deck runs out. Cards that cannot be drawn
     * (e.g. requiring a player presence) are placed in the top row instead and
     * do not count towards {@code targetSize}.
     *
     * @param targetSize desired number of tribe cards in the lower row
     */
    public void fillLowRow(int targetSize){
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

    /**
     * Draws cards from the deck to fill empty slots in the top row up to
     * {@code targetSize}, stopping early if the deck runs out.
     *
     * @param targetSize desired number of tribe cards in the top row
     */
    public void fillTopRow(int targetSize){
        CardRow topRow = getGame().getBoard().topRow();
        Deck deck = getGame().getBoard().deck();

        int i=0;
        while(topRow.getCardAt(i) != null){ i++; }
        while (i < targetSize) {
            if (deck.isEmpty()) {
                break;
            }
            topRow.add(deck.drawCard());
            i++;
        }
    }

    /**
     * Splits the full set of {@code buildings} into three per-era decks
     * (shuffled and trimmed to the configured size), places the era-1 deck face
     * up in the top row, and keeps the era-2 and era-3 decks for later refills.
     *
     * @param buildings    all building cards available for this player count
     * @param numPlayers   number of players in the game
     * @param gameSettings settings controlling how many buildings per era are used
     */
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
