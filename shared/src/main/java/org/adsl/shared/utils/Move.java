package org.adsl.shared.utils;

import org.adsl.shared.enums.Row;

import java.io.Serializable;

public record Move(int rowIndex, Row row) implements Serializable {}
