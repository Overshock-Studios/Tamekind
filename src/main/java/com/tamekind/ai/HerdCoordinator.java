package com.tamekind.ai;

import com.tamekind.compat.TamekindTags;
import com.tamekind.config.TamekindConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;

public final class HerdCoordinator {
    private HerdCoordinator() {
    }

    public static boolean isHerdable(Animal animal) {
        return BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(animal.getType()).is(TamekindTags.HERDABLE);
    }

    public static List<Animal> nearbyHerd(Animal animal) {
        if (!(animal.level() instanceof ServerLevel level) || !isHerdable(animal)) return List.of();
        double radius = TamekindConfig.herdSearchRadius;
        AABB box = animal.getBoundingBox().inflate(radius);
        return level.getEntitiesOfClass(Animal.class, box, other ->
                other.isAlive() && other != animal && sameHerd(animal, other));
    }

    /**
     * The herd's alpha: the lowest-UUID adult among the animal itself and its
     * herd-mates.
     *
     * <p>The candidate pool must include {@code animal}, otherwise no animal can
     * ever elect itself and every {@code leader == animal} branch in the goals is
     * dead code: that silently disabled the alpha size bonus and stopped leaders
     * from ever publishing a shared shelter, graze or water position. Including
     * self also makes the election agree across the herd: every member scores the
     * same candidate set and picks the same winner, instead of each animal naming
     * its lowest-UUID *neighbour* and mutually following a different one.
     */
    public static Animal leaderFor(Animal animal) {
        if (animal.isBaby() || !isHerdable(animal)) return null;
        Animal leader = animal;
        int best = animal.getUUID().hashCode();
        for (Animal other : nearbyHerd(animal)) {
            if (other.isBaby()) continue;
            int score = other.getUUID().hashCode();
            if (score < best) {
                best = score;
                leader = other;
            }
        }
        return leader;
    }

    public static Animal nearestAdultForBaby(Animal baby, double radius) {
        if (!(baby.level() instanceof ServerLevel level) || !baby.isBaby()) return null;
        AABB box = baby.getBoundingBox().inflate(radius);
        return level.getEntitiesOfClass(Animal.class, box, other ->
                        other.isAlive() && !other.isBaby() && other.getType() == baby.getType())
                .stream()
                .min(Comparator.comparingDouble(baby::distanceToSqr))
                .orElse(null);
    }

    public static int herdSize(Animal animal) {
        return nearbyHerd(animal).size() + 1;
    }

    /**
     * True when a herd animal has no herd-mates in range. Isolation is stressful:
     * callers widen its alert radius and make it slower to settle.
     */
    public static boolean isIsolated(Animal animal) {
        if (!TamekindConfig.isolationStressEnabled || !isHerdable(animal)) return false;
        return nearbyHerd(animal).isEmpty();
    }

    /**
     * The adult currently on watch, rotating through the herd on a fixed shift timer so
     * the alpha is not condemned to stand guard forever while everyone else eats.
     *
     * <p>Rotation is derived from game time rather than stored, which keeps it free of
     * save data and consistent without any handoff message. Members can disagree at the
     * edges of their scan radius: two lookouts is harmless, and strictly better than
     * none.
     *
     * <p>With rotation disabled this collapses to {@link #leaderFor}.
     */
    public static Animal sentinelFor(Animal animal) {
        Animal leader = leaderFor(animal);
        if (leader == null || !TamekindConfig.sentinelRotationEnabled) return leader;
        if (!(animal.level() instanceof ServerLevel level)) return leader;

        List<Animal> adults = new java.util.ArrayList<>();
        if (!animal.isBaby()) adults.add(animal);
        for (Animal other : nearbyHerd(animal)) {
            if (!other.isBaby()) adults.add(other);
        }
        if (adults.size() < 2) return leader;
        // Sort by UUID so every member builds the same ordering from the same roster.
        adults.sort(Comparator.comparingInt(a -> a.getUUID().hashCode()));
        int shift = Math.max(20, TamekindConfig.sentinelWatchTicks);
        int index = (int) Math.floorMod(level.getGameTime() / shift, adults.size());
        return adults.get(index);
    }

    private static boolean sameHerd(Animal first, Animal second) {
        if (!isHerdable(second)) return false;
        return first.getType() == second.getType();
    }
}
