package com.tamekind.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.Level;

import java.lang.reflect.Method;

public final class SereneSeasonsCompat {
    private static final boolean LOADED = FabricLoader.getInstance().isModLoaded("sereneseasons");
    private static Method getSeasonState;
    private static Method getSeason;
    private static Method seasonName;
    private static boolean initialized;

    private SereneSeasonsCompat() {
    }

    public static boolean isLoaded() {
        return LOADED;
    }

    public enum Season { SPRING, SUMMER, AUTUMN, WINTER, UNKNOWN }

    public static Season current(Level level) {
        if (!LOADED) return Season.UNKNOWN;
        try {
            init();
            Object state = getSeasonState.invoke(null, level);
            if (state == null) return Season.UNKNOWN;
            Object season = getSeason.invoke(state);
            if (season == null) return Season.UNKNOWN;
            String name = (String) seasonName.invoke(season);
            return Season.valueOf(name);
        } catch (Throwable t) {
            return Season.UNKNOWN;
        }
    }

    private static synchronized void init() throws Exception {
        if (initialized) return;
        Class<?> helper = Class.forName("sereneseasons.api.season.SeasonHelper");
        Class<?> iState = Class.forName("sereneseasons.api.season.ISeasonState");
        Class<?> seasonEnum = Class.forName("sereneseasons.api.season.Season");
        getSeasonState = helper.getMethod("getSeasonState", Level.class);
        getSeason = iState.getMethod("getSeason");
        seasonName = seasonEnum.getMethod("name");
        initialized = true;
    }
}
