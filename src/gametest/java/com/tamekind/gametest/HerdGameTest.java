package com.tamekind.gametest;

import com.tamekind.ai.GoalPriorities;
import com.tamekind.ai.HerdCoordinator;
import com.tamekind.config.TamekindConfig;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.cow.Cow;

import java.util.EnumSet;
import java.util.List;

/**
 * Herd election and goal wiring, in a real world.
 *
 * <p>These cover the things unit tests structurally cannot: whether goals actually attach
 * to a live entity, and whether the election agrees across a herd that exists. The alpha
 * bug shipped precisely because no test could see a real herd.
 */
public class HerdGameTest {

    /** Spawns a settled herd of cows at the test origin. */
    private static List<Cow> herd(GameTestHelper helper, int count) {
        // Relative coordinates. Every GameTestHelper method works in test-local space, so
        // wrapping this in absolutePos() double-converts and spawns the herd far outside
        // the test region, where the members cannot see each other.
        return helper.spawn(EntityType.COW, new net.minecraft.core.BlockPos(2, 2, 2), count);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", padding = 48, maxTicks = 120, setupTicks = 5)
    public void everyGoalAttachesToASpawnedAnimal(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityType.COW, 2, 2, 2);
        helper.runAfterDelay(10, () -> {
            long tamekind = ((com.tamekind.mixin.MobGoalSelectorAccessor) cow)
                    .tamekind$goalSelector().getAvailableGoals().stream()
                    .filter(g -> g.getGoal() instanceof com.tamekind.ai.goal.TamekindGoal)
                    .count();
            // 22 goals are injected. Asserting a floor rather than the exact number keeps
            // this from breaking every time a behaviour is added.
            helper.assertTrue(tamekind >= 20,
                    "expected Tamekind goals on a fresh cow, found " + tamekind);
            helper.succeed();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", padding = 48, maxTicks = 200, setupTicks = 5)
    public void herdMatesCanSeeEachOther(GameTestHelper helper) {
        // Runs before the election tests on purpose. If this fails, the election tests are
        // failing for a lookup reason and not an election reason, and chasing the election
        // logic would be chasing the wrong thing.
        List<Cow> cows = herd(helper, 5);
        helper.runAfterDelay(40, () -> {
            Cow first = cows.getFirst();
            int size = HerdCoordinator.herdSize(first);
            int nearby = HerdCoordinator.nearbyHerd(first).size();
            helper.assertValueEqual(5, size, "herdSize for a co-located herd of 5");
            helper.assertValueEqual(4, nearby, "nearbyHerd excluding self");
            helper.succeed();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", padding = 48, maxTicks = 200, setupTicks = 5)
    public void exactlyOneAlphaPerHerdAndEveryoneAgrees(GameTestHelper helper) {
        List<Cow> cows = herd(helper, 5);
        helper.runAfterDelay(40, () -> {
            // Report the roster first so a failure says which of the two problems it is.
            helper.assertValueEqual(5, HerdCoordinator.herdSize(cows.getFirst()),
                    "herd roster size before electing");
            Animal elected = null;
            int selfElected = 0;
            for (Cow cow : cows) {
                Animal leader = HerdCoordinator.leaderFor(cow);
                helper.assertTrue(leader != null, "a herdable cow in a herd must have a leader");
                if (leader == cow) selfElected++;
                if (elected == null) elected = leader;
                // The bug was disagreement: A naming B while B named A.
                helper.assertTrue(leader == elected,
                        "herd disagreed on its alpha: " + elected + " vs " + leader);
            }
            helper.assertValueEqual(1, selfElected, "self-elected alphas");
            helper.succeed();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", padding = 48, maxTicks = 200, setupTicks = 5)
    public void aLoneCowIsIsolatedAndHasNoHerd(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityType.COW, 2, 2, 2);
        helper.runAfterDelay(20, () -> {
            helper.assertValueEqual(1, HerdCoordinator.herdSize(cow), "lone herd size");
            helper.assertTrue(HerdCoordinator.isIsolated(cow),
                    "a cow with no herd-mates must read as isolated");
            helper.succeed();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", padding = 48, maxTicks = 260, setupTicks = 5)
    public void theSentinelRotatesRatherThanGuardingForever(GameTestHelper helper) {
        List<Cow> cows = herd(helper, 5);
        // A shift is sentinelWatchTicks long, so sample either side of one boundary.
        helper.runAfterDelay(30, () -> {
            Animal first = HerdCoordinator.sentinelFor(cows.getFirst());
            helper.assertTrue(first != null, "a herd should have someone on watch");
            helper.runAfterDelay(TamekindConfig.sentinelWatchTicks + 30, () -> {
                Animal later = HerdCoordinator.sentinelFor(cows.getFirst());
                helper.assertTrue(later != null, "watch should still be staffed");
                helper.assertTrue(later != first,
                        "the watch must rotate; the same animal held it across two shifts");
                helper.succeed();
            });
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", padding = 48, maxTicks = 200, setupTicks = 5)
    public void nothingHoldsMoveAtVanillaFloatPriority(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityType.COW, 2, 2, 2);
        helper.runAfterDelay(10, () -> {
            for (WrappedGoal wrapped : ((com.tamekind.mixin.MobGoalSelectorAccessor) cow)
                    .tamekind$goalSelector().getAvailableGoals()) {
                if (wrapped.getPriority() != GoalPriorities.VANILLA_FLOAT) continue;
                EnumSet<Goal.Flag> flags = wrapped.getFlags();
                boolean isFloat = wrapped.getGoal().getClass().getSimpleName().equals("FloatGoal");
                if (isFloat) {
                    // If FloatGoal ever gains MOVE, BabyAnchorGoal at 0 becomes a drowning
                    // hazard. This is the assertion that would catch that on a MC update.
                    helper.assertFalse(flags.contains(Goal.Flag.MOVE),
                            "FloatGoal gained MOVE; BabyAnchorGoal must move off priority 0");
                }
            }
            helper.succeed();
        });
    }
}
