package org.adsl.client.serverEvents;

import org.adsl.client.view.Screen;

public class LoginNeededEvent implements ServerEvent {
    @Override
    public <S extends Screen<S>> S accept(EventVisitor<S> visitor) {
        return visitor.visit(this);
    }
}
