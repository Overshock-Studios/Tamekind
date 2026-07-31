package com.tamekind.ai;

import com.tamekind.config.TamekindConfig;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;

import java.util.Random;

/**
 * Per-animal base size.
 *
 * <p>A wild-spawned animal rolls its size deterministically from its UUID, so the
 * value needs no storage and survives reloads. A *bred* animal instead inherits the
 * average of its parents plus a small wobble, and that inherited value is persisted:
 * which is what makes selective breeding compound across generations instead of
 * resetting every birth.
 *
 * <p>This writes the SCALE attribute's *base* value. {@code AgeScaleGoal} and
 * {@code AlphaPrideGoal} layer transient {@code ADD_MULTIPLIED_BASE} modifiers on top,
 * so growth and alpha pride multiply this rather than overwrite it.
 */
public final class SizeVariance {

    private SizeVariance() {
    }

    /** Applies the animal's base scale, inherited if it has one, rolled if it does not. */
    public static void apply(Animal animal) {
        if (!TamekindConfig.sizeVarianceEnabled) return;
        AttributeInstance attr = animal.getAttribute(Attributes.SCALE);
        if (attr == null) return;
        attr.setBaseValue(baseScaleFor(animal));
    }

    private static double baseScaleFor(Animal animal) {
        double inherited = AnimalMemoryStore.get(animal).inheritedScale();
        if (TamekindConfig.heritableSizeEnabled && !Double.isNaN(inherited)) {
            return clamp(inherited);
        }
        return rolledScale(animal.getUUID().getLeastSignificantBits(), TamekindConfig.sizeVarianceRange);
    }

    /** The wild roll: uniform within +/- {@code range} of 1.0, seeded for determinism. */
    public static double rolledScale(long seed, double range) {
        Random r = new Random(seed);
        return clamp(1.0 + (r.nextDouble() * 2.0 - 1.0) * range);
    }

    /**
     * A calf's scale: the midpoint of its parents, nudged by a little jitter so a
     * breeding line drifts instead of converging on one exact number forever.
     */
    public static double inheritFrom(Animal parentA, Animal parentB, double jitterRoll) {
        return blend(currentBase(parentA), currentBase(parentB), jitterRoll);
    }

    /**
     * The pure blend, split out from {@link #inheritFrom} so descent maths can be
     * unit-tested without two live parents.
     *
     * @param jitterRoll a roll in {@code [0,1)}; 0.5 means no jitter
     */
    public static double blend(double a, double b, double jitterRoll) {
        double jitter = (jitterRoll * 2.0 - 1.0) * TamekindConfig.heritableSizeJitter;
        return clamp((a + b) / 2.0 + jitter);
    }

    private static double currentBase(Animal animal) {
        AttributeInstance attr = animal.getAttribute(Attributes.SCALE);
        if (attr != null) return attr.getBaseValue();
        return baseScaleFor(animal);
    }

    /**
     * Keeps a bred line inside the same envelope as a wild roll. Without this, jitter
     * compounds over generations into unusably tiny or oversized animals.
     */
    private static double clamp(double scale) {
        double range = Math.max(0.0, TamekindConfig.sizeVarianceRange);
        return Math.min(1.0 + range, Math.max(1.0 - range, scale));
    }
}
