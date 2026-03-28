package org.example.server.model;

public interface Datasource {
    void updateAll(String error);
    void addObserver(ModelObserver o);
    void removeObserver(ModelObserver o);
}
