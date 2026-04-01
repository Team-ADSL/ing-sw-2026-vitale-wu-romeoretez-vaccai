package org.example.shared.network.responses;

import org.example.shared.exceptions.InvalidResponseException;

import java.io.Serializable;

public abstract class ServerResponse implements Serializable {
    public abstract void accept(ResponseVisitor visitor) throws InvalidResponseException;
}
