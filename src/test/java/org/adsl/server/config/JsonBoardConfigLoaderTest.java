package org.adsl.server.config;

import org.adsl.server.model.board.OfferTrack;
import org.adsl.server.model.board.OrderTile;
import org.adsl.server.model.cards.Card;
import org.adsl.server.model.cards.buildings.Building;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class JsonBoardConfigLoaderTest {

    private JsonBoardConfigLoader loader;

    @BeforeEach
    void setUp() {
        loader = new JsonBoardConfigLoader();
    }

    // ──────────────────────────────────────────────
    // getCards
    // ──────────────────────────────────────────────

    @Test
    void testGetCards_2p_returnsThreeErasAndFinalEvents() {
        ArrayList<Set<Card>> cards = loader.getCards(2);
        assertEquals(4, cards.size());
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5})
    void testGetCards_allPlayerCounts_returnsThreeErasAndFinalEvents(int n) {
        assertEquals(4, loader.getCards(n).size());
    }

    @Test
    void testGetCards_2p_eachEraIsNonEmpty() {
        ArrayList<Set<Card>> cards = loader.getCards(2);
        for (Set<Card> era : cards) {
            assertFalse(era.isEmpty(), "Era set should not be empty for 2 players");
        }
    }

    @Test
    void testGetCards_5p_hasMoreCardsThan2p() {
        int total2p = loader.getCards(2).stream().mapToInt(Set::size).sum();
        int total5p = loader.getCards(5).stream().mapToInt(Set::size).sum();
        assertTrue(total5p > total2p, "5-player game should have more cards than 2-player");
    }

    @Test
    void testGetCards_allCardsHaveValidEra() {
        ArrayList<Set<Card>> cards = loader.getCards(2);
        for (int i = 0; i < cards.size() - 1; i++) {
            int era = i + 1;
            for (Card c : cards.get(i)) {
                assertEquals(era, c.getEra(), "Card era mismatch in era set " + era);
            }
        }
        for (Card c : cards.get(3)) {
            assertEquals(3, c.getEra(), "Card era mismatch in final events set ");
        }
    }

    @Test
    void testGetCards_2p_numPlayersIsSetOnCards() {
        ArrayList<Set<Card>> cards = loader.getCards(2);
        for (Set<Card> era : cards) {
            for (Card c : era) {
                assertTrue(c.getNumPlayers().isPresent(), "2p cards should have numPlayers set");
                assertEquals(2, c.getNumPlayers().get());
            }
        }
    }

    @Test
    void testGetCards_allIdsAreNonNull() {
        ArrayList<Set<Card>> cards = loader.getCards(3);
        for (Set<Card> era : cards) {
            for (Card c : era) {
                assertNotNull(c.getId(), "Card id should not be null");
                assertFalse(c.getId().isBlank(), "Card id should not be blank");
            }
        }
    }

    // ──────────────────────────────────────────────
    // getOfferTrack
    // ──────────────────────────────────────────────

    @Test
    void testGetOfferTrack_2p_returnsNonNull() {
        assertNotNull(loader.getOfferTrack(2));
    }

    @Test
    void testGetOfferTrack_2p_hasAtLeastOneTile() {
        OfferTrack track = loader.getOfferTrack(2);
        assertTrue(track.size() > 0);
    }

    @Test
    void testGetOfferTrack_5p_hasMoreTilesThan2p() {
        int size2p = loader.getOfferTrack(2).size();
        int size5p = loader.getOfferTrack(5).size();
        assertTrue(size5p > size2p, "5p should have more offer tiles than 2p");
    }

    @Test
    void testGetOfferTrack_2p_allTilesStartWithNoPlayer() {
        OfferTrack track = loader.getOfferTrack(2);
        for (int i = 0; i < track.size(); i++) {
            assertTrue(track.getTileAt(i).getPlayer().isEmpty(),
                    "Tile " + i + " should have no player initially");
        }
    }

    // ──────────────────────────────────────────────
    // getOrderTile
    // ──────────────────────────────────────────────

    @Test
    void testGetOrderTile_2p_returnsNonNull() {
        assertNotNull(loader.getOrderTile(2));
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5})
    void testGetOrderTile_allPlayerCounts_returnsNonNull(int n) {
        assertNotNull(loader.getOrderTile(n));
    }

    @Test
    void testGetOrderTile_2p_hasCorrectCellCount() {
        OrderTile tile = loader.getOrderTile(2);
        assertEquals(2, tile.size());
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5})
    void testGetOrderTile_cellCountMatchesPlayerCount(int n) {
        OrderTile tile = loader.getOrderTile(n);
        assertEquals(n, tile.size());
    }

    @Test
    void testGetOrderTile_allCellsStartWithNoPlayer() {
        OrderTile tile = loader.getOrderTile(3);
        for (int i = 0; i < tile.size(); i++) {
            assertTrue(tile.getPlayerAt(i).isEmpty(),
                    "Cell " + i + " should have no player initially");
        }
    }

    // ──────────────────────────────────────────────
    // getBuildings
    // ──────────────────────────────────────────────

    @Test
    void testGetBuildings_returnsNonEmptySet() {
        assertFalse(loader.getBuildings().isEmpty());
    }

    @Test
    void testGetBuildings_allBuildingsHaveValidEra() {
        for (Building b : loader.getBuildings()) {
            assertTrue(b.getEra() >= 1 && b.getEra() <= 3,
                    "Building " + b.getId() + " has invalid era " + b.getEra());
        }
    }

    @Test
    void testGetBuildings_coversAllThreeEras() {
        Set<Building> buildings = loader.getBuildings();
        boolean hasEra1 = buildings.stream().anyMatch(b -> b.getEra() == 1);
        boolean hasEra2 = buildings.stream().anyMatch(b -> b.getEra() == 2);
        boolean hasEra3 = buildings.stream().anyMatch(b -> b.getEra() == 3);
        assertAll(
                () -> assertTrue(hasEra1, "Should have era-1 buildings"),
                () -> assertTrue(hasEra2, "Should have era-2 buildings"),
                () -> assertTrue(hasEra3, "Should have era-3 buildings")
        );
    }

    @Test
    void testGetBuildings_allIdsAreNonNull() {
        for (Building b : loader.getBuildings()) {
            assertNotNull(b.getId());
            assertFalse(b.getId().isBlank());
        }
    }
}
