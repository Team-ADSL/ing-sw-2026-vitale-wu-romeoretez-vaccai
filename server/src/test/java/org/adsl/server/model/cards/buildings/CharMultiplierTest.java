package org.adsl.server.model.cards.buildings;

import org.adsl.server.model.cards.characters.Hunter;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class CharMultiplierTest {

    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player("Tester");
    }

    @Test
    void testActiveEffect_endGameTrigger_awardsPPAsMultiplierTimesCount() {
        player.getCards().get(CardType.HUNTER).add(new Hunter("h1", false, 1, null));
        player.getCards().get(CardType.HUNTER).add(new Hunter("h2", false, 1, null));
        player.getCards().get(CardType.HUNTER).add(new Hunter("h3", false, 1, null));
        CharMultiplier building = new CharMultiplier("cm", 8, 8, 3, null, CardType.HUNTER, 3);
        building.activeEffect(Set.of(player), Trigger.END_GAME);
        assertEquals(9, player.getPp());
    }

    @Test
    void testActiveEffect_noCardsOfType_awardsZeroPP() {
        CharMultiplier building = new CharMultiplier("cm", 6, 6, 3, null, CardType.INVENTOR, 2);
        building.activeEffect(Set.of(player), Trigger.END_GAME);
        assertEquals(0, player.getPp());
    }

    @Test
    void testActiveEffect_wrongTrigger_doesNotAwardPP() {
        player.getCards().get(CardType.HUNTER).add(new Hunter("h1", false, 1, null));
        CharMultiplier building = new CharMultiplier("cm", 8, 8, 3, null, CardType.HUNTER, 4);
        building.activeEffect(Set.of(player), Trigger.END_ROUND);
        assertEquals(0, player.getPp());
    }

    @Test
    void testActiveEffect_emptyPlayerSet_doesNotThrow() {
        CharMultiplier building = new CharMultiplier("cm", 8, 8, 3, null, CardType.HUNTER, 4);
        assertDoesNotThrow(() -> building.activeEffect(Set.of(), Trigger.END_GAME));
    }
}
