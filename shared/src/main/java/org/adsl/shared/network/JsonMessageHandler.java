package org.adsl.shared.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import org.adsl.shared.network.requests.ClientRequest;
import org.adsl.shared.network.responses.ServerResponse;

/**
 * Utility class for JSON serialisation and deserialisation of network messages.
 * Uses a shared Jackson {@code ObjectMapper} with polymorphic type annotations
 * on {@code ClientRequest} and {@code ServerResponse}. All methods are stateless
 * and thread-safe.
 */
public class JsonMessageHandler {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static ClientRequest deserializeClientRequest(String jsonMessage) throws JsonProcessingException {
        return mapper.readValue(jsonMessage, ClientRequest.class);
    }

    public static ServerResponse deserializeServerResponse(String jsonMessage) throws JsonProcessingException {
        return mapper.readValue(jsonMessage, ServerResponse.class);
    }

    public static String serializeClientRequest(ClientRequest request) throws JsonProcessingException {
        return mapper.writeValueAsString(request);
    }

    public static String serializeServerResponse(ServerResponse response) throws JsonProcessingException {
        return mapper.writeValueAsString(response);
    }
}
