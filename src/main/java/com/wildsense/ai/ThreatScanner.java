package com.wildsense.ai;

import com.wildsense.WildsenseMod;
import com.wildsense.compat.WildsenseTags;
import com.wildsense.config.WildsenseConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;

public final class ThreatScanner {
    private static final Map<Identifier, TagKey<EntityType<?>>> PREY_TAG_CACHE = new HashMap<>();

    private ThreatScanner() {
    }

    private static TagKey<EntityType<?>> predatorsOf(Animal prey) {
        Identifier preyId = BuiltInRegistries.ENTITY_TYPE.getKey(prey.getType());
        return PREY_TAG_CACHE.computeIfAbsent(preyId, id ->
                TagKey.create(Registries.ENTITY_TYPE,
                        Identifier.fromNamespaceAndPath(WildsenseMod.MOD_ID, "predators_of/" + id.getNamespace() + "/" + id.getPath())));
    }

    public static Entity nearestThreat(Animal animal, double radius) {
        if (!(animal.level() instanceof ServerLevel level)) return null;
        AABB box = animal.getBoundingBox().inflate(radius);
        Entity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity entity : level.getEntities(animal, box, entity -> isThreat(animal, entity))) {
            double dist = entity.distanceToSqr(animal);
            if (dist < bestDist) {
                bestDist = dist;
                best = entity;
            }
        }
        return best;
    }

    private static boolean isThreat(Animal animal, Entity entity) {
        if (!entity.isAlive()) return false;
        var holder = BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entity.getType());
        if (holder.is(predatorsOf(animal))) return true;
        if (holder.is(WildsenseTags.PREDATORS)) return true;
        if (entity instanceof Player player) {
            long gameTime = animal.level().getGameTime();
            double trust = WildsenseConfig.trustEnabled
                    ? AnimalMemoryStore.get(animal).trustScore(player.getUUID(), gameTime)
                    : 0.0;
            double fleeScale = 1.0 - trust * WildsenseConfig.trustedPlayerFleeReduction;
            double fleeDistance = 6.0 * Math.max(0.2, fleeScale);
            return player.isSprinting() && player.distanceToSqr(animal) < fleeDistance * fleeDistance;
        }
        return false;
    }
}
