package com.tamekind.ai;

import com.tamekind.config.TamekindConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SizeVarianceTest {

    @BeforeEach
    void resetConfig() {
        TamekindConfig.sizeVarianceRange = 0.25;
        TamekindConfig.heritableSizeJitter = 0.06;
    }

    @Test
    void wildRollIsDeterministicForASeed() {
        double first = SizeVariance.rolledScale(42L, 0.25);
        for (int i = 0; i < 50; i++) {
            assertEquals(first, SizeVariance.rolledScale(42L, 0.25));
        }
    }

    @Test
    void wildRollStaysInsideTheConfiguredRange() {
        for (long seed = -500; seed < 500; seed++) {
            double s = SizeVariance.rolledScale(seed, 0.25);
            assertTrue(s >= 0.75 && s <= 1.25, "seed " + seed + " produced " + s);
        }
    }

    @Test
    void zeroRangeProducesExactlyVanillaSize() {
        assertEquals(1.0, SizeVariance.rolledScale(12345L, 0.0));
    }

    @Test
    void childIsTheParentMidpointWithNeutralJitter() {
        // jitterRoll 0.5 is the no-jitter midpoint of the [0,1) roll.
        assertEquals(1.0, SizeVariance.blend(0.9, 1.1, 0.5), 1e-9);
        assertEquals(0.9, SizeVariance.blend(0.85, 0.95, 0.5), 1e-9);
    }

    @Test
    void jitterPushesBothDirections() {
        double low = SizeVariance.blend(1.0, 1.0, 0.0);
        double high = SizeVariance.blend(1.0, 1.0, 1.0);
        assertTrue(low < 1.0, "roll 0 should shrink, got " + low);
        assertTrue(high > 1.0, "roll 1 should grow, got " + high);
        assertEquals(TamekindConfig.heritableSizeJitter, high - 1.0, 1e-9);
    }

    @Test
    void breedingTwoLargeParentsCompoundsButCannotEscapeTheRange() {
        // Selective breeding for size should trend up and then hit the envelope,
        // never run away to absurd values across generations.
        double scale = 1.0;
        for (int generation = 0; generation < 200; generation++) {
            scale = SizeVariance.blend(scale, scale, 1.0);
        }
        assertEquals(1.25, scale, 1e-9);
    }

    @Test
    void breedingTwoSmallParentsBottomsOutAtTheFloor() {
        double scale = 1.0;
        for (int generation = 0; generation < 200; generation++) {
            scale = SizeVariance.blend(scale, scale, 0.0);
        }
        assertEquals(0.75, scale, 1e-9);
    }

    @Test
    void blendRespectsANarrowedRange() {
        TamekindConfig.sizeVarianceRange = 0.05;
        double scale = SizeVariance.blend(1.25, 1.25, 1.0);
        assertTrue(scale <= 1.05, "clamp must follow the configured range, got " + scale);
    }
}
