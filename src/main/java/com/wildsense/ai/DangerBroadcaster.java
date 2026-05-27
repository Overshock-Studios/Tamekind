package com.tamekind.ai;

import com.tamekind.config.TamekindConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class DangerBroadcaster {
    private DangerBroadcaster() {
    }

    public static void rememberAndSpread(Animal source, Vec3 danger) {
        long now = source.level().getGameTime();
        long until = now + TamekindConfig.memoryTicks;
        AnimalMemory memory = AnimalMemoryStore.get(source);
        memory.rememberDanger(danger, until);
        if (!memory.canSpreadDanger(now, TamekindConfig.herdDangerSpreadCooldownTicks)) {
            return;
        }
        spread(source, danger, until);
    }

    private static void spread(Animal source, Vec3 danger, long until) {
        if (!TamekindConfig.herdEnabled || !(source.level() instanceof ServerLevel level)) return;
        double radius = TamekindConfig.herdSearchRadius;
        AABB box = source.getBoundingBox().inflate(radius);
        for (Animal herdMate : level.getEntitiesOfClass(Animal.class, box, other ->
                other.isAlive() && other != source && other.getType() == source.getType())) {
            AnimalMemoryStore.get(herdMate).rememberDanger(danger, until);
        }
    }
}
