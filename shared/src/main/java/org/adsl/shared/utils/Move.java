package org.adsl.shared.utils;

import org.adsl.shared.enums.Row;

import java.io.Serializable;

/**
 * Encodes a single card-pick or totem-placement action submitted by a client.
 *
 * @param rowIndex zero-based index within the target row or offer track
 * @param row      which row the move targets ({@code UPPER}, {@code LOWER},
 *                 or {@code OFFER} for totem placement)
 */
public record Move(int rowIndex, Row row) implements Serializable {}
