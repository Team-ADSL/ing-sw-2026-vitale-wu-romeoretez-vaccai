package org.example.server.model;

import org.example.shared.model.DatasourceDTO;

public interface Datasource {
    void updateAll();
    void addObserver(ModelObserver o);
    void removeObserver(ModelObserver o);
    DatasourceDTO createDTO();
}
