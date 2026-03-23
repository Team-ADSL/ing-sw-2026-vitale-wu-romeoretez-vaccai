package org.example.model.card.building;

import org.example.model.card.Trigger;
import org.example.model.game.Color;
import org.example.model.game.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ExtraMoveTest {

    private Player player;
    private ExtraMove extraMove;

    @BeforeEach
    void setUp() {
        player = new Player("Tester", 10, 0, Color.RED);
        extraMove = new ExtraMove(3, 2, Trigger.END_ROUND, Set.of(), 1, Optional.empty());
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