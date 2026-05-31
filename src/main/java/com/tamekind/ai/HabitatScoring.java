package com.tamekind.ai;

import com.tamekind.compat.TamekindTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

public final class HabitatScoring {
    private HabitatScoring() {
    }

    public static long surfaceBonus(Level level, BlockPos pos, boolean night) {
        long delta = 0;
        var below = level.getBlockState(pos.below());
        if (below.is(TamekindTags.COMFORT_BLOCKS)) delta -= 6;
        if (below.is(TamekindTags.GRAZING_BLOCKS)) delta -= 2;
        if (below.is(TamekindTags.SOFT_AVOID_BLOCKS) || level.getBlockState(pos).is(TamekindTags.SOFT_AVOID_BLOCKS)) delta += 10;
        if (night) {
            delta -= level.getBrightness(LightLayer.BLOCK, pos);
        }
        return delta;
    }

    public static boolean isOpenAirAbove(Level level, BlockPos pos) {
        return level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir();
    }
}
