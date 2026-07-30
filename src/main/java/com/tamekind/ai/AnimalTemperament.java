package com.tamekind.ai;

import com.tamekind.config.TamekindConfig;
import net.minecraft.world.entity.animal.Animal;

/**
 * A per-spawn personality that modulates how jumpy an animal is and how readily it
 * bonds with a player.
 *
 * <p>Derived deterministically from the entity UUID, so it needs no storage, no
 * codec and no save data, and it is stable across reloads and across client/server.
 * The high UUID bits are used because {@code PassiveGoalInjector} seeds size
 * variance from the low bits — a shared seed would visually telegraph temperament,
 * making every skittish cow the same size.
 *
 * <p>This is the behaviour-only answer to the trait and genetics systems in the
 * livestock mods (see {@code docs/FEATURE-GAP.md}): those ship items, GUIs and
 * replacement entities to express per-animal variation. Tamekind expresses it
 * purely through AI, so vanilla farms, drops and breeding are untouched.
 */
public enum AnimalTemperament {
    /** Spooks early, freezes longer, takes longer to trust. */
    SKITTISH(1.35, 1.4, 0.7),
    /** Vanilla-ish baseline. */
    STEADY(1.0, 1.0, 1.0),
    /** Holds its ground, short freezes. */
    BOLD(0.7, 0.6, 1.0),
    /** Approachable and quick to bond. */
    CURIOUS(0.9, 0.8, 1.5);

    private final double alertRadiusMultiplier;
    private final double freezeMultiplier;
    private final double trustGainMultiplier;

    AnimalTemperament(double alertRadiusMultiplier, double freezeMultiplier, double trustGainMultiplier) {
        this.alertRadiusMultiplier = alertRadiusMultiplier;
        this.freezeMultiplier = freezeMultiplier;
        this.trustGainMultiplier = trustGainMultiplier;
    }

    public static AnimalTemperament forAnimal(Animal animal) {
        if (!TamekindConfig.temperamentEnabled) return STEADY;
        return fromUuid(animal.getUUID());
    }

    /**
     * The pure, entity-free election. Split out from {@link #forAnimal} so the
     * distribution and determinism can be unit-tested without a live world.
     */
    public static AnimalTemperament fromUuid(java.util.UUID uuid) {
        // Mix the high bits so neighbouring UUIDs do not land in the same bucket.
        long bits = uuid.getMostSignificantBits();
        int roll = Math.floorMod(Long.hashCode(bits * 0x9E3779B97F4A7C15L), 100);
        if (roll < 30) return SKITTISH;
        if (roll < 70) return STEADY;
        if (roll < 85) return BOLD;
        return CURIOUS;
    }

    /** Scales the alert and panic detection radius. */
    public double alertRadiusMultiplier() {
        return alertRadiusMultiplier;
    }

    /** Scales how long an alerted animal freezes before drifting away. */
    public double freezeMultiplier() {
        return freezeMultiplier;
    }

    /** Scales trust gained per feeding and per idle bond tick. */
    public double trustGainMultiplier() {
        return trustGainMultiplier;
    }

    public String lowerName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
