package org.adsl.server.controller;

import org.adsl.server.controller.states.*;
import org.adsl.server.model.Game;
import org.adsl.shared.enums.Phase;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import static org.adsl.shared.enums.Phase.*;

public class StateFactory {
    private static final Map<Phase, BiFunction<Game, GameController, ControllerState>> mapper = new HashMap<>();

    static{
        mapper.put(TOTEM_PLACEMENT, TotemPlacementState::new);
        mapper.put(ACTION_EXECUTION, ActionExecutionState::new);
        mapper.put(EXTRA_MOVE, ExtraMoveState::new);
        mapper.put(EVENTS_EXECUTION, EventsState::new);
        mapper.put(END_ROUND, EndRoundState::new);
        mapper.put(END_GAME, EndGameState::new);
    }

    public static ControllerState recover(Game game, GameController context){
        BiFunction<Game, GameController, ControllerState> func = mapper.get(game.getPhase());
        return func.apply(game, context);
    }
}
