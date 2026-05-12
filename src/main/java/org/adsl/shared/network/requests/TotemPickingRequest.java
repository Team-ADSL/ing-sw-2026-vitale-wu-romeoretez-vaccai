package org.adsl.shared.network.requests;

import org.adsl.server.exceptions.ServerException;
import org.adsl.shared.enums.Totem;

public class TotemPickingRequest extends ClientRequest {
    private Totem totem;

    @Override
    public <T> void accept(RequestVisitor<T> visitor, T context) throws ServerException {
        visitor.visit(this, context);
    }

    public Totem getTotem() {
        return totem;
    }
}
