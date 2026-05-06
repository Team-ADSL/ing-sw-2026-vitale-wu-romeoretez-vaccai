package org.adsl.shared.network.requests;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.adsl.server.exceptions.ServerException;

import java.io.Serializable;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ClientConnection.class, name = "CONNECTION"),
        @JsonSubTypes.Type(value = ClientDisconnected.class, name = "DISCONNECTION"),
        @JsonSubTypes.Type(value = LoginRequest.class, name = "LOGIN"),
        @JsonSubTypes.Type(value = LogoutRequest.class, name = "LOGOUT"),
        @JsonSubTypes.Type(value = ClientPing.class, name = "PING"),
        @JsonSubTypes.Type(value = CreateGameRequest.class, name = "CREATE_GAME"),
        @JsonSubTypes.Type(value = EnterGameRequest.class, name = "ENTER_GAME"),
        @JsonSubTypes.Type(value = ExitLobbyRequest.class, name = "EXIT_LOBBY"),
        @JsonSubTypes.Type(value = StartGameRequest.class, name = "START_GAME"),
        @JsonSubTypes.Type(value = MoveRequest.class, name = "MOVE"),
        @JsonSubTypes.Type(value = ExitGameRequest.class, name = "EXIT_GAME"),
})
public abstract class ClientRequest implements Serializable {
    public abstract <T> void accept(RequestVisitor<T> visitor, T context) throws ServerException;
}
