package org.example.shared.model;

import java.io.Serializable;

public record CardDTO(String id) implements Serializable, Renderable {}