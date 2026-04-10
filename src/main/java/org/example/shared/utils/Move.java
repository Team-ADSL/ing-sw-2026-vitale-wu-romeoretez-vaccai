package org.example.shared.utils;

import org.example.shared.enums.Row;

public class Move {

    private final int rowIndex;
    private final Row row;

    public Move(int rowIndex, Row row) {
        this.rowIndex = rowIndex;
        this.row = row;
    }

    public int getRowIndex(){
        return rowIndex;
    }

    public Row getRow() {
        return row;
    }
}
