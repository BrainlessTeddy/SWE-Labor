package com.artcreator.model;

public enum QuantizationMethod {
    K_MEANS,
    MEDIAN_CUT,
    /** Snap directly to the locked palette without computing a new one. */
    PALETTE_SNAP
}
