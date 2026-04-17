package org.adsl.server.exceptions;

import org.adsl.server.controller.ServerController;

public class ServerException extends RuntimeException{
    public ServerException(String  message) {
        super(message);
    }

    public void handle(ServerController serverController){}
}
