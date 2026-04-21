package org.adsl.shared.network.responses;

import org.adsl.client.exceptions.InvalidResponseException;

import java.io.Serializable;

public abstract class ServerResponse implements Serializable {
    public abstract void accept(ResponseVisitor visitor) throws InvalidResponseException;
}