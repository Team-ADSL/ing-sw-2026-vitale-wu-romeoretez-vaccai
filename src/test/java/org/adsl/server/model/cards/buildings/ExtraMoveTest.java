package org.adsl.server.model.cards.buildings;

import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ExtraMoveTest {

    private Player player;
    private ExtraMove extraMove;

    @BeforeEach
    void setUp() {
        player = new Player("Tester");
        extraMove = new ExtraMove("em_01", 3, 2, Trigger.END_ROUND, Set.of(), 1, null);
    }

    @Test
    void getId_returnsCorrectId() {
        assertEquals("em_01", extraMove.getId());
    }

    @Test
    void activeEffect_endRoundTrigger_setsExtraMoveTrue() {
        extraMove.activeEffect(Set.of(player), Trigger.END_ROUND);
        assertTrue(player.getBuildingBonus().isExtraMove());
    }

    @Test
    void activeEffect_wrongTrigger_doesNotSetExtraMove() {
        extraMove.activeEffect(Set.of(player), Trigger.END_TURN);
        assertFalse(player.getBuildingBonus().isExtraMove());
    }

    @Test
    void activeEffect_endGameTrigger_doesNotSetExtraMove() {
        extraMove.activeEffect(Set.of(player), Trigger.END_GAME);
        assertFalse(player.getBuildingBonus().isExtraMove());
    }

    @Test
    void activeEffect_emptyPlayerSet_doesNotThrow() {
        assertDoesNotThrow(() -> extraMove.activeEffect(Set.of(), Trigger.END_ROUND));
    }
}
