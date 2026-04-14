package org.adsl.shared.network.requests;

import org.adsl.server.exceptions.GameException;

import java.io.Serializable;

public abstract class ClientRequest implements Serializable {
    public abstract <T> void accept(RequestVisitor<T> visitor, T context) throws GameException;
}
