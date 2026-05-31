package com.tamekind.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TamekindProfileTest {

    @Test
    void recognisesPrimaryNames() {
        assertEquals(TamekindConfig.Profile.VANILLA_PLUS, TamekindConfig.Profile.fromString("vanilla+"));
        assertEquals(TamekindConfig.Profile.REALISM, TamekindConfig.Profile.fromString("realism"));
        assertEquals(TamekindConfig.Profile.SIMULATION, TamekindConfig.Profile.fromString("simulation"));
    }

    @Test
    void recognisesAliases() {
        assertEquals(TamekindConfig.Profile.VANILLA_PLUS, TamekindConfig.Profile.fromString("quiet"));
        assertEquals(TamekindConfig.Profile.REALISM, TamekindConfig.Profile.fromString("survival"));
        assertEquals(TamekindConfig.Profile.SIMULATION, TamekindConfig.Profile.fromString("high"));
    }

    @Test
    void isCaseInsensitive() {
        assertEquals(TamekindConfig.Profile.REALISM, TamekindConfig.Profile.fromString("Realism"));
        assertEquals(TamekindConfig.Profile.SIMULATION, TamekindConfig.Profile.fromString("SIMULATION"));
    }

    @Test
    void unknownAndNullReturnNull() {
        assertNull(TamekindConfig.Profile.fromString("nonsense"));
        assertNull(TamekindConfig.Profile.fromString(null));
    }
}
