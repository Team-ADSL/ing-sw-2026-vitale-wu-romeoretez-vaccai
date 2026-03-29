package org.example.server.model;

public interface Datasource {
    void updateAll();
    void addObserver(ModelObserver o);
    void removeObserver(ModelObserver o);
}
