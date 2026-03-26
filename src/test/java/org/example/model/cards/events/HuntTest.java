package org.example.model.cards.events;

import org.example.server.model.cards.events.Hunt;
import org.example.shared.enums.CardType;
import org.example.shared.enums.Trigger;
import org.example.server.model.cards.characters.Hunter;
import org.example.shared.enums.Color;
import org.example.server.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class HuntTest {

    private Player player;
    // multiplierPP = 2
    private Hunt hunt;

    @BeforeEach
    void setUp() {
        player = new Player("Tester", 5, 0, Color.RED);
        hunt = new Hunt(2, false, 1, Optional.empty());
    }

    @Test
    void canBeDrawn_alwaysFalse() {
        assertFalse(hunt.canBeDrawn(player));
    }

    @Test
    void activeEffect_wrongTrigger_noEffect() {
        player.getCards().get(CardType.HUNTER).add(new Hunter(false, 1, Optional.empty()));
        hunt.activeEffect(Set.of(player), Trigger.END_ROUND);
        assertEquals(0, player.getPp());
        assertEquals(5, player.getFood());
    }

    @Test
    void activeEffect_noHunters_noPPandNoFood() {
        hunt.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertEquals(0, player.getPp());
        assertEquals(5, player.getFood());
    }

    @Test
    void activeEffect_twoHunters_correctPPandFood() {
        // 2 hunters: PP += 2 * multiplierPP(2) = 4, food += 2
        player.getCards().get(CardType.HUNTER).add(new Hunter(false, 1, Optional.empty()));
        player.getCards().get(CardType.HUNTER).add(new Hunter(false, 1, Optional.empty()));
        hunt.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertEquals(4, player.getPp());
        assertEquals(7, player.getFood());
    }

    @Test
    void activeEffect_withHuntEventBonus_doublesExtraReward() {
        // 2 hunters + huntEventBonus: PP += 2*2 + 2 = 6, food += 2 + 2 = 4 extra
        player.getCards().get(CardType.HUNTER).add(new Hunter(false, 1, Optional.empty()));
        player.getCards().get(CardType.HUNTER).add(new Hunter(false, 1, Optional.empty()));
        player.getBuildingBonus().setHuntEventBonus(true);
        hunt.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertEquals(6, player.getPp());     // 4 base + 2 bonus
        assertEquals(9, player.getFood());   // 5 initial + 2 base + 2 bonus
    }

    @Test
    void activeEffect_resetsBonus() {
        player.getBuildingBonus().setHuntEventBonus(true);
        hunt.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertFalse(player.getBuildingBonus().isHuntEventBonus());
    }

    @Test
    void activeEffect_multiplePlayersAllGetReward() {
        Player p2 = new Player("Second", 0, 0, Color.BLUE);
        player.getCards().get(CardType.HUNTER).add(new Hunter(false, 1, Optional.empty()));
        p2.getCards().get(CardType.HUNTER).add(new Hunter(false, 1, Optional.empty()));
        p2.getCards().get(CardType.HUNTER).add(new Hunter(false, 1, Optional.empty()));
        hunt.activeEffect(Set.of(player, p2), Trigger.EVENT_EXECUTION);
        assertEquals(2, player.getPp());   // 1 hunter * 2
        assertEquals(4, p2.getPp());       // 2 hunters * 2
    }
}
