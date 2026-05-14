package org.adsl.server.model.cards.events;

import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.cards.characters.Hunter;
import org.adsl.server.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class HuntTest {

    private Player player;
    private Hunt hunt;

    @BeforeEach
    void setUp() {
        player = new Player("Tester");
        player.changeFood(5);
        hunt = new Hunt("hunt_01", 2, false, 1, null);
    }

    @Test
    void testCanBeDrawn_alwaysFalse() {
        assertFalse(hunt.canBeDrawn(player));
    }

    @Test
    void testActiveEffect_wrongTrigger_noEffect() {
        player.getCards().get(CardType.HUNTER).add(new Hunter("h", false, 1, null));
        hunt.activeEffect(Set.of(player), Trigger.END_ROUND);
        assertEquals(0, player.getPp());
        assertEquals(5, player.getFood());
    }

    @Test
    void testActiveEffect_noHunters_noPPandNoFood() {
        hunt.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertEquals(0, player.getPp());
        assertEquals(5, player.getFood());
    }

    @Test
    void testActiveEffect_twoHunters_correctPPandFood() {
        player.getCards().get(CardType.HUNTER).add(new Hunter("h1", false, 1, null));
        player.getCards().get(CardType.HUNTER).add(new Hunter("h2", false, 1, null));
        hunt.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertEquals(4, player.getPp());
        assertEquals(7, player.getFood());
    }

    @Test
    void testActiveEffect_withHuntEventBonus_doublesExtraReward() {
        player.getCards().get(CardType.HUNTER).add(new Hunter("h1", false, 1, null));
        player.getCards().get(CardType.HUNTER).add(new Hunter("h2", false, 1, null));
        player.getBuildingBonus().setHuntEventBonus(true);
        hunt.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertEquals(6, player.getPp());
        assertEquals(9, player.getFood());
    }

    @Test
    void testActiveEffect_resetsBonus() {
        player.getBuildingBonus().setHuntEventBonus(true);
        hunt.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertFalse(player.getBuildingBonus().isHuntEventBonus());
    }

    @Test
    void testActiveEffect_multiplePlayersAllGetReward() {
        Player p2 = new Player("Second");
        player.getCards().get(CardType.HUNTER).add(new Hunter("h1", false, 1, null));
        p2.getCards().get(CardType.HUNTER).add(new Hunter("h2", false, 1, null));
        p2.getCards().get(CardType.HUNTER).add(new Hunter("h3", false, 1, null));
        hunt.activeEffect(Set.of(player, p2), Trigger.EVENT_EXECUTION);
        assertEquals(2, player.getPp());
        assertEquals(4, p2.getPp());
    }
}
