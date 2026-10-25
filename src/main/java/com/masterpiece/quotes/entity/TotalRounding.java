package com.masterpiece.quotes.entity;

// How the suggested final total is derived from the subtotal.
public enum TotalRounding {
    // Round to the nearest step (can go down - a small built-in discount).
    NEAREST,
    // Always round up to the next step (never below the subtotal).
    UP,
    // No rounding: the final total equals the subtotal.
    NONE
}
