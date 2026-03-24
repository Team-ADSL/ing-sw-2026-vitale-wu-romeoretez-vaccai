package org.example.controller.utils;

public class Move {

    private int rowIndex;
    private Row row;

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
