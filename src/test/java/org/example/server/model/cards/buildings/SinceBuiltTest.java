package org.example.server.model.cards.buildings;

import org.example.server.model.cards.characters.*;
import org.example.shared.enums.Icon;
import org.example.shared.enums.Trigger;
import org.example.server.model.cards.buildings.utils.BuildingEffect;
import org.example.server.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class SinceBuiltTest {

    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player("Tester");
    }

    @Test
    void getId_returnsCorrectId() {
        SinceBuilt building = new SinceBuilt("since_built_01", 5, 3, Trigger.DRAWING, 1, null, BuildingEffect.FOOD_COMPLETE_SET);
        assertEquals("since_built_01", building.getId());
    }

    @Test
    void foodCompleteSet_lessThan6Types_noFoodAwarded() {
        SinceBuilt building = new SinceBuilt("sb", 5, 3, Trigger.DRAWING, 1, null, BuildingEffect.FOOD_COMPLETE_SET);

        player.setLastPick(new Hunter("h", false, 1, null));
        building.activeEffect(Set.of(player), Trigger.DRAWING);

        player.setLastPick(new Gatherer("g", 0, 1, null));
        building.activeEffect(Set.of(player), Trigger.DRAWING);

        player.setLastPick(new Shaman("s", 1, 1, null));
        building.activeEffect(Set.of(player), Trigger.DRAWING);

        player.setLastPick(new Builder("b", 0, 0, 1, null));
        building.activeEffect(Set.of(player), Trigger.DRAWING);

        player.setLastPick(new Inventor("i", Icon.BOAT, 1, null));
        building.activeEffect(Set.of(player), Trigger.DRAWING);

        assertEquals(0, player.getFood());
    }

    @Test
    void foodCompleteSet_allSixTypes_awards5Food() {
        SinceBuilt building = new SinceBuilt("sb", 5, 3, Trigger.DRAWING, 1, null, BuildingEffect.FOOD_COMPLETE_SET);

        player.setLastPick(new Hunter("h", false, 1, null));
        building.activeEffect(Set.of(player), Trigger.DRAWING);

        player.setLastPick(new Gatherer("g", 0, 1, null));
        building.activeEffect(Set.of(player), Trigger.DRAWING);

        player.setLastPick(new Shaman("s", 1, 1, null));
        building.activeEffect(Set.of(player), Trigger.DRAWING);

        player.setLastPick(new Builder("b", 0, 0, 1, null));
        building.activeEffect(Set.of(player), Trigger.DRAWING);

        player.setLastPick(new Inventor("i", Icon.BOAT, 1, null));
        building.activeEffect(Set.of(player), Trigger.DRAWING);

        player.setLastPick(new Artist("a", 1, null));
        building.activeEffect(Set.of(player), Trigger.DRAWING);

        assertEquals(5, player.getFood());
    }

    @Test
    void foodCompleteSet_triggeredTwice_awards5FoodEachTime() {
        SinceBuilt building = new SinceBuilt("sb", 5, 3, Trigger.DRAWING, 1, null, BuildingEffect.FOOD_COMPLETE_SET);

        drawOneCardPerType(building);
        assertEquals(5, player.getFood());

        drawOneCardPerType(building);
        assertEquals(10, player.getFood());
    }

    @Test
    void foodCompleteSet_wrongTrigger_noEffect() {
        SinceBuilt building = new SinceBuilt("sb", 5, 3, Trigger.DRAWING, 1, null, BuildingEffect.FOOD_COMPLETE_SET);
        player.setLastPick(new Hunter("h", false, 1, null));
        building.activeEffect(Set.of(player), Trigger.END_ROUND);
        assertEquals(0, player.getFood());
    }

    @Test
    void foodCompleteSet_nullLastPick_noEffect() {
        SinceBuilt building = new SinceBuilt("sb", 5, 3, Trigger.DRAWING, 1, null, BuildingEffect.FOOD_COMPLETE_SET);
        player.setLastPick(null);
        assertDoesNotThrow(() -> building.activeEffect(Set.of(player), Trigger.DRAWING));
        assertEquals(0, player.getFood());
    }

    @Test
    void coupleInventor_oneInventor_noFoodAwarded() {
        SinceBuilt building = new SinceBuilt("sb", 5, 3, Trigger.DRAWING, 1, null, BuildingEffect.COUPLE_INVENTOR);
        player.setLastPick(new Inventor("i", Icon.BOAT, 1, null));
        building.activeEffect(Set.of(player), Trigger.DRAWING);
        assertEquals(0, player.getFood());
    }

    @Test
    void coupleInventor_twoInventors_awards3Food() {
        SinceBuilt building = new SinceBuilt("sb", 5, 3, Trigger.DRAWING, 1, null, BuildingEffect.COUPLE_INVENTOR);

        player.setLastPick(new Inventor("i1", Icon.BOAT, 1, null));
        building.activeEffect(Set.of(player), Trigger.DRAWING);

        player.setLastPick(new Inventor("i2", Icon.ROPE, 1, null));
        building.activeEffect(Set.of(player), Trigger.DRAWING);

        assertEquals(3, player.getFood());
    }

    @Test
    void coupleInventor_wrongTrigger_noEffect() {
        SinceBuilt building = new SinceBuilt("sb", 5, 3, Trigger.DRAWING, 1, null, BuildingEffect.COUPLE_INVENTOR);
        player.setLastPick(new Inventor("i", Icon.BOAT, 1, null));
        building.activeEffect(Set.of(player), Trigger.END_GAME);
        assertEquals(0, player.getFood());
    }

    private void drawOneCardPerType(SinceBuilt building) {
        player.setLastPick(new Hunter("h", false, 1, null));
        building.activeEffect(Set.of(player), Trigger.DRAWING);
        player.setLastPick(new Gatherer("g", 0, 1, null));
        building.activeEffect(Set.of(player), Trigger.DRAWING);
        player.setLastPick(new Shaman("s", 1, 1, null));
        building.activeEffect(Set.of(player), Trigger.DRAWING);
        player.setLastPick(new Builder("b", 0, 0, 1, null));
        building.activeEffect(Set.of(player), Trigger.DRAWING);
        player.setLastPick(new Inventor("i", Icon.BOAT, 1, null));
        building.activeEffect(Set.of(player), Trigger.DRAWING);
        player.setLastPick(new Artist("a", 1, null));
        building.activeEffect(Set.of(player), Trigger.DRAWING);
    }
}
