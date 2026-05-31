package com.tamekind.ai;

import com.tamekind.config.TamekindConfig;
import net.minecraft.world.level.Level;

public final class BreedingSeason {
    public enum Season { SPRING, SUMMER, AUTUMN, WINTER }

    private BreedingSeason() {
    }

    public static Season current(Level level) {
        var ss = com.tamekind.compat.SereneSeasonsCompat.current(level);
        switch (ss) {
            case SPRING: return Season.SPRING;
            case SUMMER: return Season.SUMMER;
            case AUTUMN: return Season.AUTUMN;
            case WINTER: return Season.WINTER;
            default: break;
        }
        return fromGameTime(level.getGameTime(), TamekindConfig.seasonLengthDays);
    }

    public static Season fromGameTime(long gameTime, int seasonLengthDays) {
        long days = Math.max(0, gameTime) / 24000L;
        int seasonLength = Math.max(1, seasonLengthDays);
        int idx = (int) ((days / seasonLength) % 4);
        return Season.values()[idx];
    }

    public static boolean isBreedingSeason(Level level) {
        if (!TamekindConfig.seasonalBreedingEnabled) return true;
        String configured = TamekindConfig.breedingSeason;
        if (configured == null || configured.isEmpty() || configured.equals("any")) return true;
        return current(level).name().equalsIgnoreCase(configured);
    }
}
