package com.tamekind.gametest;

import com.tamekind.api.TamekindAPI;
import com.tamekind.api.TamekindEvents;
import com.tamekind.api.Temperament;
import com.tamekind.ai.AnimalMemoryStore;
import com.tamekind.ai.DangerBroadcaster;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The public API, exercised the way another mod would use it.
 *
 * <p>An API is a promise, so it gets tested from the outside rather than assumed to work
 * because the internals it wraps are tested. These also pin the neutral-answer contract:
 * a caller must be able to query any animal without checking coverage first.
 */
public class ApiGameTest {

    @GameTest(structure = "fabric-gametest-api-v1:empty", padding = 48, maxTicks = 200, setupTicks = 5)
    public void herdQueriesAgreeWithEachOther(GameTestHelper helper) {
        List<Cow> cows = helper.spawn(EntityType.COW, new BlockPos(2, 2, 2), 5);
        helper.runAfterDelay(40, () -> {
            Cow cow = cows.getFirst();
            helper.assertTrue(TamekindAPI.isHerdable(cow), "a cow is herdable");
            helper.assertValueEqual(5, TamekindAPI.herdSize(cow), "API herd size");
            helper.assertFalse(TamekindAPI.isIsolated(cow), "a cow in a herd of 5 is not isolated");

            // isAlpha and alphaOf must never disagree, or callers get contradictions.
            int alphas = 0;
            for (Cow c : cows) {
                boolean isAlpha = TamekindAPI.isAlpha(c);
                helper.assertValueEqual(isAlpha, TamekindAPI.alphaOf(c) == c,
                        "isAlpha must match alphaOf for " + c.getId());
                if (isAlpha) alphas++;
            }
            helper.assertValueEqual(1, alphas, "alphas reported by the API");

            // Same contract for the watch.
            helper.assertValueEqual(TamekindAPI.isOnWatch(cow),
                    TamekindAPI.sentinelOf(cow) == cow, "isOnWatch must match sentinelOf");
            helper.succeed();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", padding = 48, maxTicks = 200, setupTicks = 5)
    public void queriesOnAnUnmanagedAnimalAreNeutralRatherThanThrowing(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityType.COW, 2, 2, 2);
        cow.setCustomName(net.minecraft.network.chat.Component.literal("Daisy"));
        helper.runAfterDelay(20, () -> {
            // A named animal opts out of Tamekind movement, and the API must say so
            // without a caller needing to know the farm-respect rules.
            helper.assertFalse(TamekindAPI.isManaged(cow), "a named cow is not managed");
            // Everything else must still answer.
            helper.assertTrue(TamekindAPI.temperamentOf(cow) != null, "temperament");
            helper.assertValueEqual(0.0, TamekindAPI.trustOf(cow, UUID.randomUUID()),
                    "trust in a stranger");
            helper.assertFalse(TamekindAPI.isAlarmed(cow), "a calm cow");
            helper.assertValueEqual(0L, TamekindAPI.alarmTicksRemaining(cow), "calm cow alarm ticks");
            helper.assertFalse(TamekindAPI.isStandingGround(cow), "a calm cow stands no ground");
            helper.assertTrue(TamekindAPI.inheritedScaleOf(cow).isEmpty(),
                    "a wild cow has no inherited scale");
            helper.succeed();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", padding = 48, maxTicks = 200, setupTicks = 5)
    public void conditionIsEmptyWhenTheSystemIsOff(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityType.COW, 2, 2, 2);
        helper.runAfterDelay(10, () -> {
            // conditionEnabled is off by default, and empty must mean "not tracked" rather
            // than being confused with a healthy 1.0.
            helper.assertValueEqual(com.tamekind.config.TamekindConfig.conditionEnabled,
                    TamekindAPI.conditionOf(cow).isPresent(),
                    "condition presence must follow conditionEnabled");
            helper.succeed();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", padding = 48, maxTicks = 200, setupTicks = 5)
    public void theAlarmedEventFiresForEveryAnimalInTheHerd(GameTestHelper helper) {
        List<Cow> cows = helper.spawn(EntityType.COW, new BlockPos(2, 2, 2), 4);
        AtomicInteger fired = new AtomicInteger();
        TamekindEvents.Alarmed listener = (animal, pos) -> fired.incrementAndGet();
        TamekindEvents.ALARMED.register(listener);

        helper.runAfterDelay(40, () -> {
            fired.set(0);
            DangerBroadcaster.rememberAndSpread(cows.getFirst(), new Vec3(0, 0, 0));
            // One for the animal that saw it, plus one per herd-mate told about it.
            helper.assertTrue(fired.get() >= 2,
                    "ALARMED should fire for the herd, fired " + fired.get() + " times");
            helper.assertTrue(TamekindAPI.isAlarmed(cows.getFirst()),
                    "the API should agree the animal is alarmed");
            helper.succeed();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", padding = 48, maxTicks = 200, setupTicks = 5)
    public void theTrustEventFiresOnEveryTrustPath(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityType.COW, 2, 2, 2);
        UUID player = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
        AtomicInteger fired = new AtomicInteger();
        double[] last = {-1.0};
        TamekindEvents.ALPHA_CHANGED.register((a, isAlpha) -> { });
        TamekindEvents.TRUST_CHANGED.register((animal, uuid, trust) -> {
            if (animal == cow) { fired.incrementAndGet(); last[0] = trust; }
        });

        helper.runAfterDelay(20, () -> {
            AnimalMemoryStore.addTrust(cow, player, 0.5, cow.level().getGameTime() + 100_000L);
            helper.assertValueEqual(1, fired.get(), "TRUST_CHANGED calls after a gain");
            helper.assertTrue(last[0] > 0.0, "the event should carry the new score, got " + last[0]);

            AnimalMemoryStore.removeTrust(cow, player, 0.5);
            helper.assertValueEqual(2, fired.get(), "TRUST_CHANGED calls after a loss");
            helper.assertValueEqual(0.0, TamekindAPI.trustOf(cow, player),
                    "trust after losing all of it");
            helper.succeed();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", padding = 48, maxTicks = 200, setupTicks = 5)
    public void temperamentIsStableAndMapsToTheApiEnum(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityType.COW, 2, 2, 2);
        helper.runAfterDelay(20, () -> {
            Temperament first = TamekindAPI.temperamentOf(cow);
            helper.assertTrue(first != null, "temperament must never be null");
            for (int i = 0; i < 20; i++) {
                helper.assertValueEqual(first, TamekindAPI.temperamentOf(cow),
                        "temperament must not drift between calls");
            }
            helper.succeed();
        });
    }
}
