package com.tamekind.ai;

import com.tamekind.config.TamekindConfig;
import net.minecraft.world.entity.animal.Animal;

/**
 * One place where the per-animal modifiers compose.
 *
 * <p>Temperament, isolation stress and body condition all pull on the same two
 * numbers — how far an animal notices things, and how fast it moves. Multiplying them
 * at each call site invited drift (one goal applying two of the three, another
 * applying a different two), so every caller goes through here instead.
 */
public final class Disposition {

    private Disposition() {
    }

    /**
     * Detection-radius multiplier: jumpy temperaments and lone animals notice threats
     * further out.
     */
    public static double alertMultiplier(Animal animal) {
        double m = AnimalTemperament.forAnimal(animal).alertRadiusMultiplier();
        if (HerdCoordinator.isIsolated(animal)) m *= TamekindConfig.isolationAlertMultiplier;
        return m;
    }

    /**
     * Movement-speed multiplier from body condition. Returns 1.0 when the condition
     * system is off, and never returns 0 — a neglected animal is slow, never stuck.
     */
    public static double speedMultiplier(Animal animal) {
        if (!TamekindConfig.conditionEnabled) return 1.0;
        double condition = AnimalMemoryStore.get(animal).condition();
        double penalty = TamekindConfig.conditionSpeedPenalty * (1.0 - condition);
        return Math.max(0.1, 1.0 - penalty);
    }

    /** True when body condition is too low for this animal to want to breed. */
    public static boolean tooWornToBreed(Animal animal) {
        if (!TamekindConfig.conditionEnabled) return false;
        return AnimalMemoryStore.get(animal).condition() < TamekindConfig.conditionBreedThreshold;
    }

    /**
     * True when the animal should hold its ground instead of fleeing, having watched
     * too many herd-mates die nearby.
     */
    public static boolean standsGround(Animal animal) {
        if (!TamekindConfig.territorialRetaliationEnabled) return false;
        return AnimalMemoryStore.get(animal).isVengeful(animal.level().getGameTime());
    }
}
