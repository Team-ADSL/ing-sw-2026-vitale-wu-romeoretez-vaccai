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

    /**
     * Parses a JSON string into the appropriate {@link ClientRequest} subtype.
     *
     * @param jsonMessage the JSON message to parse
     * @return the deserialised client request
     * @throws JsonProcessingException if the JSON is invalid or does not match a known type
     */
    public static ClientRequest deserializeClientRequest(String jsonMessage) throws JsonProcessingException {
        return mapper.readValue(jsonMessage, ClientRequest.class);
    }

    /**
     * Parses a JSON string into the appropriate {@link ServerResponse} subtype.
     *
     * @param jsonMessage the JSON message to parse
     * @return the deserialised server response
     * @throws JsonProcessingException if the JSON is invalid or does not match a known type
     */
    public static ServerResponse deserializeServerResponse(String jsonMessage) throws JsonProcessingException {
        return mapper.readValue(jsonMessage, ServerResponse.class);
    }

    /**
     * Converts a client request into its JSON representation.
     *
     * @param request the request to serialise
     * @return the JSON representation of the request
     * @throws JsonProcessingException if serialisation fails
     */
    public static String serializeClientRequest(ClientRequest request) throws JsonProcessingException {
        return mapper.writeValueAsString(request);
    }

    /**
     * Converts a server response into its JSON representation.
     *
     * @param response the response to serialise
     * @return the JSON representation of the response
     * @throws JsonProcessingException if serialisation fails
     */
    public static String serializeServerResponse(ServerResponse response) throws JsonProcessingException {
        return mapper.writeValueAsString(response);
    }
}
