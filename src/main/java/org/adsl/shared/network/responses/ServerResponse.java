package org.adsl.shared.network.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.adsl.client.exceptions.InvalidResponseException;
import org.adsl.shared.network.requests.*;

import java.io.Serializable;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = LoginNeeded.class, name = "LOGIN_NEEDED"),
        @JsonSubTypes.Type(value = HomeUpdate.class, name = "HOME_UPDATE"),
        @JsonSubTypes.Type(value = LobbyUpdate.class, name = "LOBBY_UPDATE"),
        @JsonSubTypes.Type(value = GameUpdate.class, name = "GAME_UPDATE"),
        @JsonSubTypes.Type(value = ServerPing.class, name = "PING"),
        @JsonSubTypes.Type(value = ServerDisconnected.class, name = "DISCONNECTED"),
        @JsonSubTypes.Type(value = GameEnded.class, name = "GAME_ENDED"),
        @JsonSubTypes.Type(value = ErrorResponse.class, name = "ERROR_RESPONSE"),
})
public abstract class ServerResponse implements Serializable {
    @JsonProperty("message")
    private String message;

    protected ServerResponse() {
        this.message = null;
    }

    protected ServerResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public abstract void accept(ResponseVisitor visitor) throws InvalidResponseException;
}