package org.example.server.controller.states;

import org.example.shared.network.request.ClientRequest;

public interface RequestVisitor {
    State visit(ClientRequest req); // Need to add only the subtype
}
