package com.tamekind.ai;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Covers the state added for territorial retaliation, body condition and alpha trails. */
class AnimalMemoryStateTest {

    // ── Territorial retaliation ───────────────────────────────────────────────

    @Test
    void cullsAccumulateInsideTheMemoryWindow() {
        AnimalMemory m = new AnimalMemory();
        assertEquals(1, m.recordCull(0L, 100));
        assertEquals(2, m.recordCull(50L, 100));
        assertEquals(3, m.recordCull(99L, 100));
    }

    @Test
    void cullsResetOnceTheWindowLapses() {
        AnimalMemory m = new AnimalMemory();
        m.recordCull(0L, 100);
        m.recordCull(10L, 100);
        // The 10-tick entry pushed decay out to 110; past that the count starts over,
        // so a herd culled slowly over hours never turns defiant.
        assertEquals(1, m.recordCull(500L, 100));
    }

    @Test
    void cullCountReadsZeroAfterExpiry() {
        AnimalMemory m = new AnimalMemory();
        m.recordCull(0L, 100);
        assertEquals(1, m.cullCount(50L));
        assertEquals(0, m.cullCount(1000L));
    }

    @Test
    void vengeanceIsTimeBoxedAndNeverShortensItself() {
        AnimalMemory m = new AnimalMemory();
        m.markVengeful(200L);
        assertTrue(m.isVengeful(199L));
        assertTrue(m.isVengeful(200L));
        assertFalse(m.isVengeful(201L));
        m.markVengeful(100L);
        assertTrue(m.isVengeful(200L), "a shorter mark must not cut an active one short");
    }

    // ── Body condition ────────────────────────────────────────────────────────

    @Test
    void conditionStartsFull() {
        assertEquals(1.0, new AnimalMemory().condition());
    }

    @Test
    void conditionDrainsOnlyOncePerInterval() {
        AnimalMemory m = new AnimalMemory();
        m.decayCondition(100L, 100, 0.1, 0.25);
        assertEquals(0.9, m.condition(), 1e-9);
        m.decayCondition(150L, 100, 0.1, 0.25);
        assertEquals(0.9, m.condition(), 1e-9, "too soon, must not drain again");
        m.decayCondition(200L, 100, 0.1, 0.25);
        assertEquals(0.8, m.condition(), 1e-9);
    }

    @Test
    void conditionNeverFallsBelowTheFloor() {
        AnimalMemory m = new AnimalMemory();
        for (int i = 1; i <= 100; i++) {
            m.decayCondition(i * 100L, 100, 0.1, 0.25);
        }
        assertEquals(0.25, m.condition(), 1e-9, "neglect must be non-lethal");
    }

    @Test
    void conditionRestoresAndCapsAtOne() {
        AnimalMemory m = new AnimalMemory();
        m.decayCondition(100L, 100, 0.5, 0.0);
        m.restoreCondition(0.2);
        assertEquals(0.7, m.condition(), 1e-9);
        m.restoreCondition(5.0);
        assertEquals(1.0, m.condition(), 1e-9);
    }

    // ── Alpha trail ───────────────────────────────────────────────────────────

    @Test
    void trailIsRateLimited() {
        AnimalMemory m = new AnimalMemory();
        m.pushTrail(new BlockPos(0, 64, 0), 0L, 20, 8);
        m.pushTrail(new BlockPos(50, 64, 50), 5L, 20, 8);
        // The second push was inside the rate limit, so only the first point exists.
        assertNotNull(m.trailPointFor(new BlockPos(0, 64, 0), 4.0));
        assertNull(m.trailPointFor(new BlockPos(50, 64, 50), 4.0));
    }

    @Test
    void trailSkipsPointsTooCloseToTheLast() {
        AnimalMemory m = new AnimalMemory();
        m.pushTrail(new BlockPos(0, 64, 0), 0L, 20, 8);
        assertEquals(1, m.trailSize());
        // Within 2 blocks of the previous point: not worth a second trail entry.
        m.pushTrail(new BlockPos(1, 64, 0), 100L, 20, 8);
        assertEquals(1, m.trailSize());
        // Far enough away: recorded.
        m.pushTrail(new BlockPos(9, 64, 0), 200L, 20, 8);
        assertEquals(2, m.trailSize());
    }

    @Test
    void firstTrailPointLandsEvenAtGameTimeZero() {
        AnimalMemory m = new AnimalMemory();
        m.pushTrail(new BlockPos(0, 64, 0), 0L, 20, 8);
        assertEquals(1, m.trailSize(), "the rate limit must not swallow the first point");
    }

    @Test
    void trailIsBoundedAndDropsOldestFirst() {
        AnimalMemory m = new AnimalMemory();
        for (int i = 0; i < 20; i++) {
            m.pushTrail(new BlockPos(i * 5, 64, 0), i * 100L, 20, 4);
        }
        // Cap is 4, so the earliest points are gone.
        assertNull(m.trailPointFor(new BlockPos(0, 64, 0), 1.0));
        assertNotNull(m.trailPointFor(new BlockPos(95, 64, 0), 1.0));
    }

    @Test
    void trailReturnsTheOldestPointInRange() {
        AnimalMemory m = new AnimalMemory();
        m.pushTrail(new BlockPos(0, 64, 0), 0L, 20, 8);
        m.pushTrail(new BlockPos(10, 64, 0), 100L, 20, 8);
        m.pushTrail(new BlockPos(20, 64, 0), 200L, 20, 8);
        // A follower that can reach all three should head for the earliest, so the
        // herd walks the leader's route rather than cutting to where it is now.
        BlockPos chosen = m.trailPointFor(new BlockPos(0, 64, 0), 10_000.0);
        assertEquals(new BlockPos(0, 64, 0), chosen);
    }

    @Test
    void trailReturnsNullWhenEverythingIsOutOfRange() {
        AnimalMemory m = new AnimalMemory();
        m.pushTrail(new BlockPos(500, 64, 500), 0L, 20, 8);
        assertNull(m.trailPointFor(new BlockPos(0, 64, 0), 100.0));
    }

    @Test
    void clearTrailEmptiesIt() {
        AnimalMemory m = new AnimalMemory();
        m.pushTrail(new BlockPos(0, 64, 0), 0L, 20, 8);
        m.clearTrail();
        assertNull(m.trailPointFor(new BlockPos(0, 64, 0), 10_000.0));
    }

    // ── forget() boundaries ───────────────────────────────────────────────────

    @Test
    void forgetClearsRetaliationButKeepsDescent() {
        AnimalMemory m = new AnimalMemory();
        m.setInheritedScale(1.2);
        m.recordCull(0L, 1000);
        m.markVengeful(1000L);
        m.forget();
        assertEquals(0, m.cullCount(10L));
        assertFalse(m.isVengeful(10L));
        assertEquals(1.2, m.inheritedScale(), 1e-9, "descent is not a memory to wipe");
    }

    @Test
    void inheritedScaleDefaultsToWild() {
        assertTrue(Double.isNaN(new AnimalMemory().inheritedScale()));
    }
}
