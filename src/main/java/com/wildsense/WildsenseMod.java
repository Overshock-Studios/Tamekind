package com.tamekind;

import com.tamekind.ai.PassiveGoalInjector;
import com.tamekind.ai.PassiveEventDirector;
import com.tamekind.config.TamekindConfig;
import com.tamekind.command.TamekindCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TamekindMod implements ModInitializer {
    public static final String MOD_ID = "tamekind";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        TamekindConfig.load(FabricLoader.getInstance().getConfigDir().resolve("tamekind.properties"));
        TamekindCommand.register();
        PassiveEventDirector.register();
        PassiveGoalInjector.register();
        LOGGER.info("Tamekind initialized.");
    }
}
