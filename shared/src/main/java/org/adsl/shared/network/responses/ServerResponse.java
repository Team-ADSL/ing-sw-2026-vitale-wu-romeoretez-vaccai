package org.adsl.shared.network.responses;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.adsl.shared.exceptions.InvalidResponseException;
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
        @JsonSubTypes.Type(value = TotemAvailableUpdate.class, name = "TOTEM_AVAILABLE_UPDATE"),
        @JsonSubTypes.Type(value = GameUpdate.class, name = "GAME_UPDATE"),
        @JsonSubTypes.Type(value = ServerPing.class, name = "PING"),
        @JsonSubTypes.Type(value = ServerDisconnected.class, name = "DISCONNECTED"),
        @JsonSubTypes.Type(value = GameEnded.class, name = "GAME_ENDED"),
        @JsonSubTypes.Type(value = ErrorResponse.class, name = "ERROR_RESPONSE"),
        @JsonSubTypes.Type(value = EventsTriggered.class, name = "EVENTS_TRIGGERED"),
})
/**
 * Abstract base for all responses sent from server to client over the network.
 * Serialised to JSON via Jackson using the {@code "type"} discriminator field.
 * An optional {@code message} field carries a log-line string shown in the
 * client game log. The visitor pattern ({@link #accept}) dispatches each
 * subtype to the correct UI handler in {@link ResponseVisitor}.
 */
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

    /**
     * Heartbeat responses bypass the client-side dispatch pacer (they are not
     * user-visible and any delay would break the timeout watchdog). Defaults to
     * {@code false}; the ping subtype overrides it. {@link JsonIgnore} keeps
     * this flag off the wire — it is derivable from the runtime subtype.
     */
    @JsonIgnore
    public boolean isHeartbeat() {
        return false;
    }
}