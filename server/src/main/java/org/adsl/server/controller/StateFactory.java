package org.adsl.server.controller;

import org.adsl.server.controller.states.*;
import org.adsl.server.model.Game;
import org.adsl.shared.enums.Phase;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import static org.adsl.shared.enums.Phase.*;

/**
 * Factory for reconstructing a {@link ControllerState} from a persisted
 * {@link Game} after a server crash or restart.
 * <p>
 * Maps each {@link Phase} to the concrete state class that should be active
 * when the game is in that phase. Used exclusively by {@link RecoverState}.
 * </p>
 */
public class StateFactory {
    private static final Map<Phase, BiFunction<Game, GameController, ControllerState>> mapper = new HashMap<>();

    static{
        mapper.put(TOTEM_PICKING, TotemPickingState::new);
        mapper.put(TOTEM_PLACEMENT, TotemPlacementState::new);
        mapper.put(ACTION_EXECUTION, ActionExecutionState::new);
        mapper.put(EXTRA_MOVE, ExtraMoveState::new);
        mapper.put(EVENTS_EXECUTION, EventsState::new);
        mapper.put(END_ROUND, EndRoundState::new);
        mapper.put(END_GAME, EndGameState::new);
    }

    /**
     * Creates the {@link ControllerState} that corresponds to the phase stored
     * in {@code game}. Throws {@link NullPointerException} if the game's phase
     * has no registered mapping (e.g. {@code LOBBY} or {@code TOTEM_PICKING},
     * which are not recoverable mid-session).
     *
     * @param game    the recovered game model
     * @param context the {@link GameController} that will own the new state
     * @return the concrete state to resume the game from
     */
    public static ControllerState recover(Game game, GameController context){
        BiFunction<Game, GameController, ControllerState> func = mapper.get(game.getPhase());
        return func.apply(game, context);
    }
}
