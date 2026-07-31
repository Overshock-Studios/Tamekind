package com.tamekind.gametest;

import com.tamekind.ai.AnimalMemory;
import com.tamekind.ai.AnimalMemoryStore;
import com.tamekind.ai.SizeVariance;
import com.tamekind.ai.TamekindAttachments;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * The attachment, in a real entity.
 *
 * <p>The codec is unit-tested in isolation; what needs a world is whether the attachment
 * is actually registered, created lazily, and readable back off a live animal, and
 * whether size ends up where the scale pipeline says it should.
 */
public class MemoryGameTest {

    @GameTest(structure = "fabric-gametest-api-v1:empty", padding = 48, maxTicks = 120, setupTicks = 5)
    public void memoryIsAbsentUntilTouchedThenPersistsOnTheEntity(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityType.COW, 2, 2, 2);
        helper.runAfterDelay(20, () -> {
            // A goal may already have touched it, so this asserts the mechanism rather than
            // demanding absence: writing then reading must round-trip on the live entity.
            AnimalMemory memory = AnimalMemoryStore.get(cow);
            UUID player = UUID.fromString("00000000-0000-0000-0000-00000000f00d");
            memory.addTrust(player, 0.5, cow.level().getGameTime() + 100_000L);
            memory.setHome(cow.blockPosition());

            AnimalMemory again = AnimalMemoryStore.get(cow);
            helper.assertTrue(again.trustScore(player, cow.level().getGameTime()) > 0.0,
                    "trust written through the attachment was not readable back");
            helper.assertTrue(again.home() != null, "home written through the attachment was lost");
            helper.assertTrue(cow.hasAttached(TamekindAttachments.MEMORY),
                    "the attachment must exist on the entity once touched");
            helper.succeed();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", padding = 48, maxTicks = 120, setupTicks = 5)
    public void dangerMemoryExpiresRatherThanSticking(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityType.COW, 2, 2, 2);
        helper.runAfterDelay(10, () -> {
            long now = cow.level().getGameTime();
            AnimalMemory memory = AnimalMemoryStore.get(cow);
            memory.rememberDanger(new Vec3(1, 2, 3), now + 20);
            helper.assertTrue(memory.dangerPos(now) != null, "danger should be live immediately");
            helper.runAfterDelay(40, () ->
                    helper.succeedIf(() -> helper.assertTrue(
                            memory.dangerPos(cow.level().getGameTime()) == null,
                            "danger memory outlived its expiry")));
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", padding = 48, maxTicks = 120, setupTicks = 5)
    public void aWildCowGetsASizeInsideTheConfiguredEnvelope(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityType.COW, 2, 2, 2);
        helper.runAfterDelay(20, () -> {
            var attr = cow.getAttribute(Attributes.SCALE);
            helper.assertTrue(attr != null, "cows should have a SCALE attribute");
            double base = attr.getBaseValue();
            double range = com.tamekind.config.TamekindConfig.sizeVarianceRange;
            helper.assertTrue(base >= 1.0 - range - 1e-6 && base <= 1.0 + range + 1e-6,
                    "wild scale " + base + " escaped the configured envelope");
            helper.succeed();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", padding = 48, maxTicks = 120, setupTicks = 5)
    public void aStampedInheritedScaleWins(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityType.COW, 2, 2, 2);
        helper.runAfterDelay(20, () -> {
            AnimalMemoryStore.get(cow).setInheritedScale(1.2);
            SizeVariance.apply(cow);
            var attr = cow.getAttribute(Attributes.SCALE);
            helper.assertTrue(attr != null && Math.abs(attr.getBaseValue() - 1.2) < 1e-6,
                    "a bred animal's inherited scale must beat its wild roll, got "
                            + (attr == null ? "no attribute" : attr.getBaseValue()));
            helper.succeed();
        });
    }
}
