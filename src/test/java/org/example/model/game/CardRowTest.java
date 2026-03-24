package org.example.model.game;
import org.example.model.game.boardComponent.CardRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CardRowTest {

    private CardRow row;
    private FakeCard card1;
    private FakeCard card2;
    private FakeCard card3;

    @BeforeEach
    void setUp() {
        // Arrange - create a fresh CardRow and cards before each test
        // AGGIORNAMENTO: Aggiunto il secondo parametro 'nonBuildingCard' richiesto dal nuovo costruttore
        row = new CardRow(3, 3);
        card1 = new FakeCard();
        card2 = new FakeCard();
        card3 = new FakeCard();
    }

    // --- add() tests ---

    @Test
    void testAddInsertsInFirstEmptySlot() {
        // card should be placed at index 0 first
        row.add(card1);
        assertEquals(card1, row.pickCardAt(0));
    }

    @Test
    void testAddFillsSlotsInOrder() {
        // cards should fill slots sequentially
        row.add(card1);
        row.add(card2);
        assertEquals(card1, row.pickCardAt(0));
        assertEquals(card2, row.pickCardAt(1));
    }

    @Test
    void testAddDoesNothingWhenFull() {
        // adding to a full row should not crash and size stays 3
        row.add(card1);
        row.add(card2);
        row.add(card3);
        row.add(new FakeCard()); // should be ignored
        assertEquals(3, row.size());
    }

    // --- pickCardAt() tests ---

    @Test
    void testPickCardAtReturnsCorrectCard() {
        // should return the card at the given index
        row.add(card1);
        assertEquals(card1, row.pickCardAt(0));
    }

    @Test
    void testPickCardAtLeavesOtherCardsIntact() {
        // picking one card should not affect the others
        row.add(card1);
        row.add(card2);
        row.pickCardAt(0);
        assertEquals(card2, row.pickCardAt(1));
    }
}