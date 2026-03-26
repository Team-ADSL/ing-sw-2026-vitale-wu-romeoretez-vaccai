package org.example.server.controller;

import org.example.server.controller.states.*;
import org.example.server.model.Game;
import org.example.shared.enums.Phase;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.example.shared.enums.Phase.*;

public class StateFactory {
    private static final Map<Phase, Function<Game, State>> mapper = new HashMap<>();

    static{
        mapper.put(INIT, InitGameState::new);
        mapper.put(RECOVER, RecoverState::new);
        mapper.put(TOTEM_PLACEMENT, TotemPlacementState::new);
        mapper.put(ACTION_EXECUTION, ActionExecutionState::new);
        mapper.put(EXTRA_MOVE, ExtraMoveState::new);
        mapper.put(EVENTS_EXECUTION, EventsState::new);
        mapper.put(END_ROUND, EndRoundState::new);
        mapper.put(END_GAME, EndGameState::new);
    }

    public static State recover(Game game){
        Function<Game, State> func = mapper.get(game.getPhase());
        return func.apply(game);
    }
}
