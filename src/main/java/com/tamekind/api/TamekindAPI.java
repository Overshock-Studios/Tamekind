package com.tamekind.api;

import com.tamekind.ai.AiLod;
import com.tamekind.ai.AnimalMemoryStore;
import com.tamekind.ai.AnimalTemperament;
import com.tamekind.ai.Disposition;
import com.tamekind.ai.HerdCoordinator;
import com.tamekind.ai.TamekindAnimalRules;
import com.tamekind.config.TamekindConfig;
import net.minecraft.world.entity.animal.Animal;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalDouble;
import java.util.UUID;

/**
 * Tamekind's stable, read-only API.
 *
 * <p>Tamekind is the only mod in its niche with a herd layer, so "who is this animal's
 * alpha" is a question nobody else can answer. That is the reason this exists: it turns
 * per-mod compatibility requests into something another author can do without waiting on
 * a release here.
 *
 * <p>Use it as a soft dependency. Compile against the mod and guard every call:
 *
 * <pre>{@code
 * if (FabricLoader.getInstance().isModLoaded("tamekind")) {
 *     Animal alpha = TamekindAPI.alphaOf(cow);
 * }
 * }</pre>
 *
 * <p><b>Everything outside the {@code com.tamekind.api} package is internal and may change
 * in any release.</b> This class is read-only on purpose: there is no setter anywhere in
 * it. Tamekind owns its own state, and a mod that could reach in and rewrite a herd's
 * alpha would make every behaviour here unpredictable. If you need to influence rather
 * than observe, the datapack tags are the supported route, and they need no code at all.
 *
 * <p>All methods are safe to call on any animal, on the server thread. Queries about
 * animals Tamekind does not manage return neutral answers rather than throwing, so a
 * caller never has to check {@link #isManaged} first unless it wants to.
 */
public final class TamekindAPI {

    private TamekindAPI() {
    }

    // ── Coverage ──────────────────────────────────────────────────────────────

    /**
     * Whether Tamekind is running its behaviour on this animal at all.
     *
     * <p>False for animals opted out by the {@code tamekind:disabled} tag, by a runtime
     * {@code /tamekind disable}, or by the farm-respect rules: leashed, mounted, named,
     * breeding and tamed animals keep their vanilla AI. Check this before assuming
     * Tamekind is the reason an animal is doing something.
     */
    public static boolean isManaged(Animal animal) {
        return TamekindConfig.enabled && !TamekindAnimalRules.skipMovementGoals(animal);
    }

    /** Whether this animal's type is in {@code tamekind:herdable}. */
    public static boolean isHerdable(Animal animal) {
        return HerdCoordinator.isHerdable(animal);
    }

    // ── Herd ──────────────────────────────────────────────────────────────────

    /**
     * The animal's herd alpha, or null if it has none.
     *
     * <p>May be the animal itself. Election is over herd-mates within the configured
     * search radius, so this is a live answer and can change as a herd moves.
     */
    @Nullable
    public static Animal alphaOf(Animal animal) {
        return HerdCoordinator.leaderFor(animal);
    }

    /** Whether this animal is its own herd's alpha. */
    public static boolean isAlpha(Animal animal) {
        return HerdCoordinator.leaderFor(animal) == animal;
    }

    /**
     * The herd-mate currently standing watch, or null if none.
     *
     * <p>Rotates on a shift timer, so unlike {@link #alphaOf} this changes by design even
     * when the herd has not moved.
     */
    @Nullable
    public static Animal sentinelOf(Animal animal) {
        return HerdCoordinator.sentinelFor(animal);
    }

    /** Whether this animal is the one currently on watch for its herd. */
    public static boolean isOnWatch(Animal animal) {
        return HerdCoordinator.sentinelFor(animal) == animal;
    }

    /** Herd size including the animal itself. 1 means it is alone. */
    public static int herdSize(Animal animal) {
        return HerdCoordinator.herdSize(animal);
    }

    /** Whether a herdable animal currently has no herd-mates in range. */
    public static boolean isIsolated(Animal animal) {
        return HerdCoordinator.isIsolated(animal);
    }

    // ── Individual state ──────────────────────────────────────────────────────

    /** This animal's disposition. {@link Temperament#STEADY} when temperament is off. */
    public static Temperament temperamentOf(Animal animal) {
        return switch (AnimalTemperament.forAnimal(animal)) {
            case SKITTISH -> Temperament.SKITTISH;
            case STEADY -> Temperament.STEADY;
            case BOLD -> Temperament.BOLD;
            case CURIOUS -> Temperament.CURIOUS;
        };
    }

    /**
     * How much this animal trusts a player, from 0 to 1.
     *
     * <p>Trust decays, so this is a point-in-time reading. 0 for an unknown player.
     */
    public static double trustOf(Animal animal, UUID player) {
        if (!TamekindConfig.trustEnabled) return 0.0;
        return AnimalMemoryStore.get(animal).trustScore(player, animal.level().getGameTime());
    }

    /**
     * Body condition from 0 to 1, or empty when the condition system is disabled.
     *
     * <p>Empty rather than 1.0 so a caller can tell "healthy" apart from "not tracked".
     */
    public static OptionalDouble conditionOf(Animal animal) {
        if (!TamekindConfig.conditionEnabled) return OptionalDouble.empty();
        return OptionalDouble.of(AnimalMemoryStore.get(animal).condition());
    }

    /**
     * The scale this animal inherited from its parents, or empty if it was not bred.
     *
     * <p>Empty means a wild roll, which is derived from the entity UUID rather than stored.
     */
    public static OptionalDouble inheritedScaleOf(Animal animal) {
        double scale = AnimalMemoryStore.get(animal).inheritedScale();
        return Double.isNaN(scale) ? OptionalDouble.empty() : OptionalDouble.of(scale);
    }

    // ── Threat state ──────────────────────────────────────────────────────────

    /**
     * Whether this animal is currently frightened of something.
     *
     * <p>True while it holds a live danger memory, which outlasts the threat being visible.
     */
    public static boolean isAlarmed(Animal animal) {
        return AnimalMemoryStore.get(animal).dangerPos(animal.level().getGameTime()) != null;
    }

    /** Ticks left on the current danger memory, or 0 if the animal is calm. */
    public static long alarmTicksRemaining(Animal animal) {
        return AnimalMemoryStore.get(animal).dangerTicksRemaining(animal.level().getGameTime());
    }

    /**
     * Whether this animal has stopped fleeing and is standing its ground.
     *
     * <p>Set after it has watched enough herd-mates die nearby. It never retaliates with
     * damage; only its panic is suppressed.
     */
    public static boolean isStandingGround(Animal animal) {
        return Disposition.standsGround(animal);
    }

    /**
     * Whether Tamekind is currently running full AI for this animal.
     *
     * <p>Useful for a mod deciding whether its own per-tick work is worth doing: if
     * Tamekind has decided this animal is too far from any player to simulate closely,
     * the same is probably true for the caller.
     */
    public static boolean isFullySimulated(Animal animal) {
        return AiLod.forAnimal(animal) == AiLod.FULL;
    }
}
