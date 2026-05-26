package com.wildsense.ai;

import com.wildsense.ai.goal.AlertFreezeGoal;
import com.wildsense.ai.goal.BabyAnchorGoal;
import com.wildsense.ai.goal.DrinkGoal;
import com.wildsense.ai.goal.GrazeRestGoal;
import com.wildsense.ai.goal.HabitatShelterGoal;
import com.wildsense.ai.goal.HerdFollowGoal;
import com.wildsense.ai.goal.WildPanicGoal;
import com.wildsense.ai.goal.WildsenseGoal;
import com.wildsense.config.WildsenseConfig;
import com.wildsense.mixin.MobGoalSelectorAccessor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.phys.AABB;

public final class PassiveGoalInjector {
    private PassiveGoalInjector() {
    }

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof Animal animal) {
                inject(animal);
            }
        });

        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (!WildsenseConfig.enabled) return InteractionResult.PASS;
            if (level.isClientSide()) return InteractionResult.PASS;
            if (entity instanceof Animal animal && animal.isFood(player.getItemInHand(hand))) {
                if (shouldBlockCrowdedBreeding(animal)) {
                    if (WildsenseConfig.breedingCrowdMessageEnabled) {
                        player.sendSystemMessage(Component.literal("This pen is too crowded for more breeding."));
                    }
                    return InteractionResult.FAIL;
                }
                if (!WildsenseConfig.trustEnabled) return InteractionResult.PASS;
                long until = level.getGameTime() + WildsenseConfig.trustTicks;
                AnimalMemoryStore.get(animal).addTrust(player.getUUID(), WildsenseConfig.trustPerFeeding, until);
                for (Animal herdMate : HerdCoordinator.nearbyHerd(animal)) {
                    AnimalMemoryStore.get(herdMate).addTrust(
                            player.getUUID(),
                            WildsenseConfig.trustPerFeeding * WildsenseConfig.herdTrustShareMultiplier,
                            until);
                }
            }
            return InteractionResult.PASS;
        });
    }

    private static boolean shouldBlockCrowdedBreeding(Animal animal) {
        if (!WildsenseConfig.breedingCrowdControlEnabled || animal.isBaby()) return false;
        if (WildsenseConfig.breedingCrowdHardLimit <= 0) return false;
        double radius = WildsenseConfig.breedingCrowdRadius;
        AABB box = animal.getBoundingBox().inflate(radius);
        int sameType = animal.level().getEntitiesOfClass(Animal.class, box,
                other -> other.isAlive() && other.getType() == animal.getType()).size();
        return sameType >= WildsenseConfig.breedingCrowdHardLimit;
    }

    private static void inject(Animal animal) {
        if (!WildsenseConfig.enabled || hasWildsenseGoal(animal)) return;
        MobGoalSelectorAccessor accessor = (MobGoalSelectorAccessor) animal;
        accessor.wildsense$goalSelector().addGoal(0, new BabyAnchorGoal(animal));
        accessor.wildsense$goalSelector().addGoal(1, new WildPanicGoal(animal));
        accessor.wildsense$goalSelector().addGoal(2, new AlertFreezeGoal(animal));
        accessor.wildsense$goalSelector().addGoal(5, new HabitatShelterGoal(animal));
        accessor.wildsense$goalSelector().addGoal(6, new HerdFollowGoal(animal));
        accessor.wildsense$goalSelector().addGoal(8, new GrazeRestGoal(animal));
        accessor.wildsense$goalSelector().addGoal(9, new DrinkGoal(animal));
    }

    private static boolean hasWildsenseGoal(Animal animal) {
        for (WrappedGoal goal : ((MobGoalSelectorAccessor) animal).wildsense$goalSelector().getAvailableGoals()) {
            if (goal.getGoal() instanceof WildsenseGoal) return true;
        }
        return false;
    }
}
