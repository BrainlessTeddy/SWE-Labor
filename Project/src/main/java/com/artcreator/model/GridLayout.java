package com.artcreator.model;

/**
 * Geometric arrangement of the stick grid.
 *
 * <ul>
 *   <li>{@link #SQUARE}: aligned rows and columns.</li>
 *   <li>{@link #HEXAGONAL}: every other row offset by half a pitch (denser packing).</li>
 * </ul>
 */
public enum GridLayout {
    SQUARE,
    HEXAGONAL
}
