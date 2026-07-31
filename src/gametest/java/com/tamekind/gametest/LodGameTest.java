package com.tamekind.gametest;

import com.tamekind.ai.AiLod;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.phys.Vec3;

/**
 * Level-of-detail, and the harness fact that every other in-world test depends on.
 *
 * <p>Most Tamekind behaviour only runs at {@code AiLod.FULL}, which is measured from the
 * nearest player. A headless test server has no players, so without a mock player none of
 * that behaviour fires and the failure looks like a broken mod rather than an absent
 * observer. That cost a run to learn, so it is asserted here rather than remembered.
 */
public class LodGameTest {

    @GameTest(structure = "fabric-gametest-api-v1:empty", padding = 48, maxTicks = 200, setupTicks = 10)
    public void withoutAPlayerNearbyBehaviourIsCorrectlyDormant(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityType.COW, 2, 2, 2);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(AiLod.forAnimal(cow) != AiLod.FULL,
                    "with no player in the level an animal must not be fully simulated");
            helper.succeed();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", padding = 48, maxTicks = 200, setupTicks = 10)
    public void aMockPlayerBringsAnimalsUpToFullSimulation(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        player.snapTo(Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 2, 2))), 0f, 0f);
        Cow cow = helper.spawn(EntityType.COW, 3, 2, 3);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(AiLod.forAnimal(cow) == AiLod.FULL,
                    "a cow beside a player should be fully simulated");
            helper.succeed();
        });
    }
}
