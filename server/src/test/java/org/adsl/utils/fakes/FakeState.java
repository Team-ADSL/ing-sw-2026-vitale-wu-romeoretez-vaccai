package org.adsl.utils.fakes;

import org.adsl.server.controller.GameController;
import org.adsl.server.controller.states.ControllerState;

public class FakeState extends ControllerState {
    public boolean requestHandled = false;
    public boolean onEntryCalled = false;

    public ControllerState autoTransitionTarget = null;

    public FakeState(GameController context) {
        super(null, context);
    }

    @Override
    public ControllerState onEntry() {
        this.onEntryCalled = true;
        return autoTransitionTarget;
    }

    @Override
    public ControllerState calcNextState() {
        return null;
    }
}
