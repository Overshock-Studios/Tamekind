package com.tamekind.ai;

import com.tamekind.TamekindMod;
import com.tamekind.compat.TamekindTags;
import com.tamekind.config.TamekindConfig;
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
                        Identifier.fromNamespaceAndPath(TamekindMod.MOD_ID, "predators_of/" + id.getNamespace() + "/" + id.getPath())));
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
        if (best != null && best instanceof net.minecraft.world.entity.Mob mob) {
            mob.setShiftKeyDown(true);
        }
        return best;
    }

    private static boolean isThreat(Animal animal, Entity entity) {
        if (!entity.isAlive()) return false;
        if (entity instanceof net.minecraft.world.entity.TamableAnimal tame
                && (tame.isTame() || tame.isOrderedToSit())) return false;
        if (entity instanceof net.minecraft.world.entity.Mob mob && mob.isBaby()
                && entity.distanceToSqr(animal) < 9.0) return false;
        var holder = BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entity.getType());
        if (holder.is(predatorsOf(animal))) return true;
        if (holder.is(TamekindTags.PREDATORS)) return true;
        if (entity instanceof net.minecraft.world.entity.raid.Raider raider
                && raider.getCurrentRaid() != null && raider.getCurrentRaid().isActive()) return true;
        if (entity instanceof Player player) {
            long gameTime = animal.level().getGameTime();
            double trust = TamekindConfig.trustEnabled
                    ? AnimalMemoryStore.get(animal).trustScore(player.getUUID(), gameTime)
                    : 0.0;
            double fleeScale = 1.0 - trust * TamekindConfig.trustedPlayerFleeReduction;
            double fleeDistance = 6.0 * Math.max(0.2, fleeScale);
            return player.isSprinting() && player.distanceToSqr(animal) < fleeDistance * fleeDistance;
        }
        return false;
    }
}
