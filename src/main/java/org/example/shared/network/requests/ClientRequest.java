package org.example.shared.network.requests;

import org.example.server.exceptions.InvalidRequestException;

import java.io.Serializable;

public abstract class ClientRequest implements Serializable {
    public abstract <T> void accept(RequestVisitor<T> visitor, T context) throws InvalidRequestException;
}
