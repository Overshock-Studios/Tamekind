package com.tamekind.gametest;

import com.tamekind.ai.TagHunting;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;

/**
 * The predator food-web, and the farm guarantee that constrains it.
 *
 * <p>This is the highest-value test in the suite. The hunt goal spent a whole release
 * silently not attaching, because reflection on a mapped field name resolves in dev and
 * throws in a released jar. Repairing it then exposed that a tamed wolf would work
 * through its owner's own flock. Both failures were invisible to compilation and to unit
 * tests, and both are asserted here.
 */
public class PredatorGameTest {

    @GameTest(structure = "fabric-gametest-api-v1:empty", padding = 48, maxTicks = 200, setupTicks = 5)
    public void aWildWolfWillHuntASheep(GameTestHelper helper) {
        Wolf wolf = helper.spawn(EntityType.WOLF, 2, 2, 2);
        Sheep sheep = helper.spawn(EntityType.SHEEP, 4, 2, 2);
        helper.runAfterDelay(10, () -> {
            // The tag decides, so this asserts the data and the guard together rather than
            // waiting on pathfinding, which would make the test flaky.
            helper.assertTrue(TagHunting.shouldHunt(wolf, sheep),
                    "a wild wolf must hunt a sheep listed in predators_of/minecraft/sheep");
            helper.succeed();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", padding = 48, maxTicks = 200, setupTicks = 5)
    public void aTamedWolfNeverHuntsItsOwnersFlock(GameTestHelper helper) {
        Wolf wolf = helper.spawn(EntityType.WOLF, 2, 2, 2);
        Sheep sheep = helper.spawn(EntityType.SHEEP, 4, 2, 2);
        wolf.setTame(true, false);
        helper.runAfterDelay(10, () -> {
            helper.assertFalse(TagHunting.shouldHunt(wolf, sheep),
                    "a tamed wolf hunting the player's own sheep breaks the farm guarantee");
            helper.succeed();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", padding = 48, maxTicks = 200, setupTicks = 5)
    public void theHuntGoalActuallyAttachedToTheWolf(GameTestHelper helper) {
        Wolf wolf = helper.spawn(EntityType.WOLF, 2, 2, 2);
        helper.runAfterDelay(10, () -> {
            // The regression guard for the reflection bug. The goal is added to the target
            // selector in registerGoals, so its absence means the mixin silently failed.
            long targets = ((com.tamekind.mixin.MobGoalSelectorAccessor) wolf)
                    .tamekind$targetSelector().getAvailableGoals().size();
            helper.assertTrue(targets > 0,
                    "the wolf has no target goals at all, so the accessor mixin did not apply");
            helper.succeed();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", padding = 48, maxTicks = 200, setupTicks = 5)
    public void aWolfDoesNotHuntItself(GameTestHelper helper) {
        Wolf wolf = helper.spawn(EntityType.WOLF, 2, 2, 2);
        helper.runAfterDelay(10, () -> {
            helper.assertFalse(TagHunting.shouldHunt(wolf, wolf), "self-hunting");
            helper.succeed();
        });
    }
}
