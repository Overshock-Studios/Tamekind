package com.tamekind.ai;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trips the attachment codec.
 *
 * <p>Persistence moved off a save-data mixin onto a Fabric attachment, which means a
 * hand-written codec is now the only thing standing between an animal and losing every
 * memory it has. A codec is also exactly the kind of code that compiles perfectly and
 * silently drops a field, so it gets tested rather than trusted. No world is needed:
 * codecs are pure, so JSON is a fine transport for the test.
 */
class AnimalMemoryCodecTest {

    private static AnimalMemory roundTrip(AnimalMemory in) {
        JsonElement json = AnimalMemory.CODEC
                .encodeStart(JsonOps.INSTANCE, in)
                .getOrThrow(msg -> new AssertionError("encode failed: " + msg));
        return AnimalMemory.CODEC
                .parse(JsonOps.INSTANCE, json)
                .getOrThrow(msg -> new AssertionError("decode failed: " + msg));
    }

    @Test
    void emptyMemorySurvivesRoundTrip() {
        AnimalMemory out = roundTrip(new AnimalMemory());
        assertNull(out.dangerPos(0L));
        assertNull(out.home());
        assertEquals(1.0, out.condition(), 1e-9);
        assertTrue(Double.isNaN(out.inheritedScale()));
        assertEquals(0, out.activeTrustCount(0L));
    }

    @Test
    void dangerSurvivesWithItsExpiry() {
        AnimalMemory in = new AnimalMemory();
        in.rememberDanger(new Vec3(12.5, 64.0, -30.25), 900L);
        AnimalMemory out = roundTrip(in);
        Vec3 danger = out.dangerPos(500L);
        assertNotNull(danger, "danger inside its window must survive");
        assertEquals(12.5, danger.x, 1e-9);
        assertEquals(64.0, danger.y, 1e-9);
        assertEquals(-30.25, danger.z, 1e-9);
        assertNull(out.dangerPos(1000L), "expiry must survive too, not just the position");
    }

    @Test
    void homeAndSharedPositionsSurvive() {
        AnimalMemory in = new AnimalMemory();
        in.setHome(new BlockPos(4, 65, -8));
        in.setSharedShelter(new BlockPos(10, 70, 11), 500L);
        in.setSharedGraze(new BlockPos(-3, 63, 7), 600L);
        in.setSharedWater(new BlockPos(20, 62, 20), 700L);
        AnimalMemory out = roundTrip(in);
        assertEquals(new BlockPos(4, 65, -8), out.home());
        assertEquals(new BlockPos(10, 70, 11), out.sharedShelter(100L));
        assertEquals(new BlockPos(-3, 63, 7), out.sharedGraze(100L));
        assertEquals(new BlockPos(20, 62, 20), out.sharedWater(100L));
        assertNull(out.sharedShelter(501L), "shared shelter must still expire");
    }

    @Test
    void trustSurvivesPerPlayer() {
        UUID a = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
        UUID b = UUID.fromString("00000000-0000-0000-0000-0000000000bb");
        AnimalMemory in = new AnimalMemory();
        in.addTrust(a, 0.6, 100_000L);
        in.addTrust(b, 0.2, 100_000L);
        AnimalMemory out = roundTrip(in);
        assertEquals(2, out.activeTrustCount(0L));
        assertTrue(out.trustScore(a, 0L) > out.trustScore(b, 0L),
                "per-player scores must not be flattened into one value");
    }

    @Test
    void descentSurvives() {
        AnimalMemory in = new AnimalMemory();
        in.setInheritedScale(1.1875);
        assertEquals(1.1875, roundTrip(in).inheritedScale(), 1e-9);
    }

    @Test
    void retaliationStateSurvives() {
        AnimalMemory in = new AnimalMemory();
        in.recordCull(0L, 1000);
        in.recordCull(10L, 1000);
        in.markVengeful(5000L);
        AnimalMemory out = roundTrip(in);
        assertEquals(2, out.cullCount(20L));
        assertTrue(out.isVengeful(4999L));
        assertFalse(out.isVengeful(5001L));
    }

    @Test
    void conditionSurvivesAndStaysClamped() {
        AnimalMemory in = new AnimalMemory();
        in.decayCondition(1000L, 1, 0.4, 0.0);
        AnimalMemory out = roundTrip(in);
        assertEquals(0.6, out.condition(), 1e-9);
    }

    @Test
    void guardFlagSurvives() {
        AnimalMemory in = new AnimalMemory();
        in.markGuarding(400L);
        AnimalMemory out = roundTrip(in);
        assertTrue(out.isGuarding(399L));
        assertFalse(out.isGuarding(401L));
    }

    @Test
    void theTrailIsDeliberatelyNotPersisted() {
        AnimalMemory in = new AnimalMemory();
        in.pushTrail(new BlockPos(0, 64, 0), 0L, 20, 8);
        in.pushTrail(new BlockPos(9, 64, 0), 100L, 20, 8);
        assertEquals(2, in.trailSize());
        // A route the leader walked minutes ago is worse than no route, so it is dropped.
        assertEquals(0, roundTrip(in).trailSize());
    }

    @Test
    void anEmptyObjectDecodes() {
        // Animals saved before this attachment existed, and animals that never had memory,
        // must decode rather than throw and take the entity down with them.
        AnimalMemory out = AnimalMemory.CODEC
                .parse(JsonOps.INSTANCE, new com.google.gson.JsonObject())
                .getOrThrow(msg -> new AssertionError("empty object must decode: " + msg));
        assertEquals(1.0, out.condition(), 1e-9);
        assertNull(out.home());
    }
}
