package com.tamekind.ai;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimalTemperamentTest {

    @Test
    void isDeterministicForTheSameUuid() {
        UUID id = UUID.fromString("6f6d4e0a-1c3b-4a55-9d21-8e7c5b4a3f10");
        AnimalTemperament first = AnimalTemperament.fromUuid(id);
        for (int i = 0; i < 100; i++) {
            assertEquals(first, AnimalTemperament.fromUuid(id),
                    "temperament must not drift between reloads");
        }
    }

    @Test
    void producesEveryTemperamentAcrossManySpawns() {
        Map<AnimalTemperament, Integer> counts = new EnumMap<>(AnimalTemperament.class);
        Random random = new Random(1234L);
        for (int i = 0; i < 20_000; i++) {
            UUID id = new UUID(random.nextLong(), random.nextLong());
            counts.merge(AnimalTemperament.fromUuid(id), 1, Integer::sum);
        }
        for (AnimalTemperament t : AnimalTemperament.values()) {
            assertTrue(counts.getOrDefault(t, 0) > 0, t + " never appeared");
        }
    }

    @Test
    void roughlyMatchesTheIntendedDistribution() {
        Map<AnimalTemperament, Integer> counts = new EnumMap<>(AnimalTemperament.class);
        Random random = new Random(98765L);
        int total = 40_000;
        for (int i = 0; i < total; i++) {
            UUID id = new UUID(random.nextLong(), random.nextLong());
            counts.merge(AnimalTemperament.fromUuid(id), 1, Integer::sum);
        }
        // Intended: 30 / 40 / 15 / 15. Allow a wide band; this guards a bucket
        // collapsing to zero or swallowing everything, not exact proportions.
        assertShare(counts, total, AnimalTemperament.SKITTISH, 0.25, 0.35);
        assertShare(counts, total, AnimalTemperament.STEADY, 0.35, 0.45);
        assertShare(counts, total, AnimalTemperament.BOLD, 0.10, 0.20);
        assertShare(counts, total, AnimalTemperament.CURIOUS, 0.10, 0.20);
    }

    @Test
    void steadyIsTheNeutralBaseline() {
        assertEquals(1.0, AnimalTemperament.STEADY.alertRadiusMultiplier());
        assertEquals(1.0, AnimalTemperament.STEADY.freezeMultiplier());
        assertEquals(1.0, AnimalTemperament.STEADY.trustGainMultiplier());
    }

    @Test
    void skittishSpooksEarlierThanBold() {
        assertTrue(AnimalTemperament.SKITTISH.alertRadiusMultiplier()
                > AnimalTemperament.BOLD.alertRadiusMultiplier());
        assertTrue(AnimalTemperament.SKITTISH.freezeMultiplier()
                > AnimalTemperament.BOLD.freezeMultiplier());
    }

    @Test
    void curiousBondsFastestAndSkittishSlowest() {
        assertTrue(AnimalTemperament.CURIOUS.trustGainMultiplier() > 1.0);
        assertTrue(AnimalTemperament.SKITTISH.trustGainMultiplier() < 1.0);
    }

    @Test
    void everyMultiplierIsPositive() {
        for (AnimalTemperament t : AnimalTemperament.values()) {
            assertTrue(t.alertRadiusMultiplier() > 0.0, t + " alert radius must be positive");
            assertTrue(t.freezeMultiplier() > 0.0, t + " freeze must be positive");
            assertTrue(t.trustGainMultiplier() > 0.0, t + " trust gain must be positive");
        }
    }

    @Test
    void lowerNameIsCommandFriendly() {
        assertEquals("skittish", AnimalTemperament.SKITTISH.lowerName());
        assertEquals("curious", AnimalTemperament.CURIOUS.lowerName());
    }

    private static void assertShare(Map<AnimalTemperament, Integer> counts, int total,
                                    AnimalTemperament t, double min, double max) {
        double share = counts.getOrDefault(t, 0) / (double) total;
        assertTrue(share >= min && share <= max,
                t + " share was " + share + ", expected between " + min + " and " + max);
    }
}
