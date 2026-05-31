package com.tamekind.ai;

import com.tamekind.config.TamekindConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;

import java.util.Map;
import java.util.WeakHashMap;

public enum AiLod {
    FULL,
    SIMPLE,
    SLEEP,
    HIBERNATE;

    private static final Map<Animal, CachedLod> CACHE = new WeakHashMap<>();

    public static AiLod forAnimal(Animal animal) {
        if (!(animal.level() instanceof ServerLevel level)) return SLEEP;
        long now = level.getGameTime();
        CachedLod cached = CACHE.get(animal);
        if (cached != null && now < cached.expiresAt) {
            return cached.lod;
        }
        AiLod lod = compute(animal, level);
        int cacheTicks = Math.max(1, TamekindConfig.aiLodCacheTicks);
        int jitter = Math.floorMod(animal.getId(), cacheTicks);
        CACHE.put(animal, new CachedLod(lod, now + cacheTicks + jitter));
        return lod;
    }

    public static AiLod computeFresh(Animal animal) {
        if (!(animal.level() instanceof ServerLevel level)) return SLEEP;
        AiLod lod = compute(animal, level);
        CACHE.put(animal, new CachedLod(lod, level.getGameTime() + Math.max(1, TamekindConfig.aiLodCacheTicks)));
        return lod;
    }

    private static AiLod compute(Animal animal, ServerLevel level) {
        double nearest = Double.MAX_VALUE;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) continue;
            nearest = Math.min(nearest, player.distanceToSqr(animal));
        }
        int full = TamekindConfig.fullAiRange;
        if (nearest <= (double) full * full) return FULL;
        int simple = TamekindConfig.simpleAiRange;
        if (nearest <= (double) simple * simple) return SIMPLE;
        int hibernate = TamekindConfig.hibernateRange;
        if (nearest > (double) hibernate * hibernate) {
            float temp = level.getBiome(animal.blockPosition()).value().getBaseTemperature();
            if (temp <= 0.1f || temp >= 1.5f) return HIBERNATE;
        }
        return SLEEP;
    }

    private record CachedLod(AiLod lod, long expiresAt) {
    }
}
