package org.example.shared.model;

import java.io.Serializable;

public class CardDTO implements Serializable, Renderable {

    private String id;

    public CardDTO() {}

    public CardDTO(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
