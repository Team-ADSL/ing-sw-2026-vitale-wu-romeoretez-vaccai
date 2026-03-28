package org.example.shared.model;

import java.util.List;

public record LobbyDTO(List<Integer> games) implements DatasourceDTO{
}
