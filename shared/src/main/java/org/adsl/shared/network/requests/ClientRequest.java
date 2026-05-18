package org.adsl.shared.network.requests;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.adsl.shared.exceptions.ServerException;

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
        @JsonSubTypes.Type(value = TotemPickingRequest.class, name = "TOTEM_PICKING"),
})
/**
 * Abstract base for all requests sent from client to server over the network.
 * Serialised to JSON via Jackson using the {@code "type"} discriminator field.
 * The visitor pattern ({@link #accept}) dispatches each subtype to the correct
 * handler in {@link RequestVisitor} without instanceof checks.
 */
public abstract class ClientRequest implements Serializable {

    /**
     * Dispatches this request to the appropriate {@code visit} method on
     * {@code visitor}, passing {@code context} as the second argument.
     *
     * @param <T>     the context type (e.g. {@code VirtualClient} on the server)
     * @param visitor the visitor to dispatch to
     * @param context the context object accompanying the request
     * @throws ServerException if the visitor rejects the request
     */
    public abstract <T> void accept(RequestVisitor<T> visitor, T context) throws ServerException;
}
