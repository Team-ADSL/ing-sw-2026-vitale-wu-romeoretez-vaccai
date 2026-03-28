package org.example.server.model;

import org.example.shared.model.DatasourceDTO;

public interface ModelObserver {
    void update(DatasourceDTO datasource);
}
