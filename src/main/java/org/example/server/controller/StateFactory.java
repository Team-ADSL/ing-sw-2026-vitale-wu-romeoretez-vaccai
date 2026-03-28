package org.example.server.controller;

import org.example.server.controller.states.*;
import org.example.server.model.Game;
import org.example.shared.enums.Phase;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.example.shared.enums.Phase.*;

public class StateFactory {
    private static final Map<Phase, Function<Game, ControllerState>> mapper = new HashMap<>();

    static{
        mapper.put(INIT, InitGameControllerState::new);
        mapper.put(RECOVER, RecoverControllerState::new);
        mapper.put(TOTEM_PLACEMENT, TotemPlacementControllerState::new);
        mapper.put(ACTION_EXECUTION, ActionExecutionControllerState::new);
        mapper.put(EXTRA_MOVE, ExtraMoveControllerState::new);
        mapper.put(EVENTS_EXECUTION, EventsControllerState::new);
        mapper.put(END_ROUND, EndRoundControllerState::new);
        mapper.put(END_GAME, EndGameControllerState::new);
    }

    public static ControllerState recover(Game game){
        Function<Game, ControllerState> func = mapper.get(game.getPhase());
        return func.apply(game);
    }
}
