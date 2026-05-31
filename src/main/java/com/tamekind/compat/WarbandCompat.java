package com.tamekind.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.raid.Raider;

public final class WarbandCompat {
    public static final boolean WARBAND_LOADED = FabricLoader.getInstance().isModLoaded("warband");

    private WarbandCompat() {
    }

    public static boolean activeRaidNear(ServerLevel level, BlockPos pos, int radius) {
        var raid = level.getRaidAt(pos);
        if (raid != null && raid.isActive()) return true;
        var box = new net.minecraft.world.phys.AABB(pos).inflate(radius);
        for (Raider r : level.getEntitiesOfClass(Raider.class, box,
                rr -> rr.isAlive() && rr.getCurrentRaid() != null && rr.getCurrentRaid().isActive())) {
            return true;
        }
        return false;
    }
}
