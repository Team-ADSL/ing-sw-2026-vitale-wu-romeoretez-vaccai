package org.example.model.cards.buildings;

import org.example.server.model.cards.buildings.SinceBuilt;
import org.example.server.model.cards.characters.*;
import org.example.shared.enums.Icon;
import org.example.shared.enums.Trigger;
import org.example.server.model.cards.buildings.utils.BuildingEffect;
import org.example.shared.enums.Color;
import org.example.server.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class SinceBuiltTest {

    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player("Tester", 0, 0, Color.RED);
    }

    // ---- FOOD_COMPLETE_SET ----

    @Test
    void foodCompleteSet_lessThan6Types_noFoodAwarded() {
        SinceBuilt building = new SinceBuilt(5, 3, Trigger.DRAWING, 1, Optional.empty(), BuildingEffect.FOOD_COMPLETE_SET);

        // Add 5 different types, one card each
        player.setLastPick(new Hunter(false, 1, Optional.empty()));
        building.activeEffect(Set.of(player), Trigger.DRAWING);

        player.setLastPick(new Gatherer(0, 1, Optional.empty()));
        building.activeEffect(Set.of(player), Trigger.DRAWING);

        player.setLastPick(new Shaman(1, 1, Optional.empty()));
        building.activeEffect(Set.of(player), Trigger.DRAWING);

        player.setLastPick(new Builder(0, 0, 1, Optional.empty()));
        building.activeEffect(Set.of(player), Trigger.DRAWING);

        player.setLastPick(new Inventor(Icon.BOAT, 1, Optional.empty()));
        building.activeEffect(Set.of(player), Trigger.DRAWING);

        assertEquals(0, player.getFood());
    }

    @Test
    void foodCompleteSet_allSixTypes_awards5Food() {
        SinceBuilt building = new SinceBuilt(5, 3, Trigger.DRAWING, 1, Optional.empty(), BuildingEffect.FOOD_COMPLETE_SET);

        player.setLastPick(new Hunter(false, 1, Optional.empty()));
        building.activeEffect(Set.of(player), Trigger.DRAWING);

        player.setLastPick(new Gatherer(0, 1, Optional.empty()));
        building.activeEffect(Set.of(player), Trigger.DRAWING);

        player.setLastPick(new Shaman(1, 1, Optional.empty()));
        building.activeEffect(Set.of(player), Trigger.DRAWING);

        player.setLastPick(new Builder(0, 0, 1, Optional.empty()));
        building.activeEffect(Set.of(player), Trigger.DRAWING);

        player.setLastPick(new Inventor(Icon.BOAT, 1, Optional.empty()));
        building.activeEffect(Set.of(player), Trigger.DRAWING);

        // 6th type completes the set
        player.setLastPick(new Artist(1, Optional.empty()));
        building.activeEffect(Set.of(player), Trigger.DRAWING);

        assertEquals(5, player.getFood());
    }

    @Test
    void foodCompleteSet_triggeredTwice_awards5FoodEachTime() {
        SinceBuilt building = new SinceBuilt(5, 3, Trigger.DRAWING, 1, Optional.empty(), BuildingEffect.FOOD_COMPLETE_SET);

        // First complete set
        drawOneCardPerType(building);
        assertEquals(5, player.getFood());

        // Second complete set
        drawOneCardPerType(building);
        assertEquals(10, player.getFood());
    }

    @Test
    void foodCompleteSet_wrongTrigger_noEffect() {
        SinceBuilt building = new SinceBuilt(5, 3, Trigger.DRAWING, 1, Optional.empty(), BuildingEffect.FOOD_COMPLETE_SET);
        player.setLastPick(new Hunter(false, 1, Optional.empty()));
        building.activeEffect(Set.of(player), Trigger.END_ROUND);
        assertEquals(0, player.getFood());
    }

    @Test
    void foodCompleteSet_nullLastPick_noEffect() {
        SinceBuilt building = new SinceBuilt(5, 3, Trigger.DRAWING, 1, Optional.empty(), BuildingEffect.FOOD_COMPLETE_SET);
        player.setLastPick(null);
        assertDoesNotThrow(() -> building.activeEffect(Set.of(player), Trigger.DRAWING));
        assertEquals(0, player.getFood());
    }

    // ---- COUPLE_INVENTOR ----

    @Test
    void coupleInventor_oneInventor_noFoodAwarded() {
        SinceBuilt building = new SinceBuilt(5, 3, Trigger.DRAWING, 1, Optional.empty(), BuildingEffect.COUPLE_INVENTOR);
        player.setLastPick(new Inventor(Icon.BOAT, 1, Optional.empty()));
        building.activeEffect(Set.of(player), Trigger.DRAWING);
        assertEquals(0, player.getFood());
    }

    @Test
    void coupleInventor_twoInventors_awards3Food() {
        SinceBuilt building = new SinceBuilt(5, 3, Trigger.DRAWING, 1, Optional.empty(), BuildingEffect.COUPLE_INVENTOR);

        player.setLastPick(new Inventor(Icon.BOAT, 1, Optional.empty()));
        building.activeEffect(Set.of(player), Trigger.DRAWING);

        player.setLastPick(new Inventor(Icon.ROPE, 1, Optional.empty()));
        building.activeEffect(Set.of(player), Trigger.DRAWING);

        assertEquals(3, player.getFood());
    }

    @Test
    void coupleInventor_wrongTrigger_noEffect() {
        SinceBuilt building = new SinceBuilt(5, 3, Trigger.DRAWING, 1, Optional.empty(), BuildingEffect.COUPLE_INVENTOR);
        player.setLastPick(new Inventor(Icon.BOAT, 1, Optional.empty()));
        building.activeEffect(Set.of(player), Trigger.END_GAME);
        assertEquals(0, player.getFood());
    }

    // Helper to draw one card from each of the 6 types
    private void drawOneCardPerType(SinceBuilt building) {
        player.setLastPick(new Hunter(false, 1, Optional.empty()));
        building.activeEffect(Set.of(player), Trigger.DRAWING);
        player.setLastPick(new Gatherer(0, 1, Optional.empty()));
        building.activeEffect(Set.of(player), Trigger.DRAWING);
        player.setLastPick(new Shaman(1, 1, Optional.empty()));
        building.activeEffect(Set.of(player), Trigger.DRAWING);
        player.setLastPick(new Builder(0, 0, 1, Optional.empty()));
        building.activeEffect(Set.of(player), Trigger.DRAWING);
        player.setLastPick(new Inventor(Icon.BOAT, 1, Optional.empty()));
        building.activeEffect(Set.of(player), Trigger.DRAWING);
        player.setLastPick(new Artist(1, Optional.empty()));
        building.activeEffect(Set.of(player), Trigger.DRAWING);
    }
}