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

    public static Animal leaderFor(Animal animal) {
        return nearbyHerd(animal).stream()
                .filter(other -> !other.isBaby())
                .min(Comparator.comparingInt(a -> a.getUUID().hashCode()))
                .orElse(null);
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

    private static boolean sameHerd(Animal first, Animal second) {
        if (!isHerdable(second)) return false;
        return first.getType() == second.getType();
    }
}
