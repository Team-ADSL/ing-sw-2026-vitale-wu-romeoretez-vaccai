package org.adsl.server.model.game;
import org.adsl.server.model.board.CardRow;
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
        row = new CardRow(3, 3);
        card1 = new FakeCard();
        card2 = new FakeCard();
        card3 = new FakeCard();
    }

    // ──────────────────────────────────────────────
    // TEST ADD
    // ──────────────────────────────────────────────

    @Test
    void testAdd_insertsInFirstEmptySlot() {
        row.add(card1);
        assertEquals(card1, row.pickCardAt(0));
    }

    @Test
    void testAdd_fillsSlotsInOrder() {
        row.add(card1);
        row.add(card2);
        assertEquals(card1, row.pickCardAt(0));
        assertEquals(card2, row.pickCardAt(1));
    }

    @Test
    void testAdd_doesNothingWhenFull() {
        row.add(card1);
        row.add(card2);
        row.add(card3);
        row.add(new FakeCard());
        assertEquals(3, row.size());
    }

    // ──────────────────────────────────────────────
    // TEST PICK CARD AT
    // ──────────────────────────────────────────────

    @Test
    void testPickCardAt_returnsCorrectCard() {
        row.add(card1);
        assertEquals(card1, row.pickCardAt(0));
    }

    @Test
    void testPickCardAt_leavesOtherCardsIntact() {
        row.add(card1);
        row.add(card2);
        row.pickCardAt(0);
        assertEquals(card2, row.pickCardAt(1));
    }
}
