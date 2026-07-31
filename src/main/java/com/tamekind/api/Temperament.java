package com.tamekind.api;

/**
 * An animal's disposition, as exposed to other mods.
 *
 * <p>Deliberately a separate enum from the internal one. The internal type carries the
 * multipliers each temperament applies, and those are tuning that will change; this is
 * just the label. Mapping between them is Tamekind's problem, not a caller's.
 */
public enum Temperament {
    /** Spooks early, freezes longer, slower to trust. */
    SKITTISH,
    /** The vanilla-ish baseline. */
    STEADY,
    /** Holds its ground, short freezes. */
    BOLD,
    /** Approachable and quick to bond. */
    CURIOUS
}
