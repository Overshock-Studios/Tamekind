package com.tamekind.ai.goal;

import com.tamekind.ai.AiLod;
import com.tamekind.ai.AnimalMemoryStore;
import com.tamekind.ai.HerdCoordinator;
import com.tamekind.ai.TamekindAnimalRules;
import com.tamekind.compat.TamekindTags;
import com.tamekind.config.TamekindConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

public final class DrinkGoal extends Goal implements TamekindGoal {
    private final Animal animal;
    private BlockPos waterEdge;
    private int nextTryTick;
    private int drinkTicks;

    public DrinkGoal(Animal animal) {
        this.animal = animal;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!TamekindConfig.enabled || !TamekindConfig.dailyRhythmEnabled) return false;
        if (TamekindAnimalRules.skipMovementGoals(animal)) return false;
        if (AiLod.forAnimal(animal) != AiLod.FULL) return false;
        if (animal.tickCount < nextTryTick) return false;
        nextTryTick = animal.tickCount + TamekindConfig.drinkMinIntervalTicks
                + animal.getRandom().nextInt(Math.max(1, TamekindConfig.drinkMinIntervalTicks));
        if (AnimalMemoryStore.get(animal).dangerPos(animal.level().getGameTime()) != null) return false;
        if (animal.isInLove() || animal.isBaby()) return false;
        if (animal.isInWater()) return false;
        long now = animal.level().getGameTime();
        Animal leader = HerdCoordinator.leaderFor(animal);
        if (leader != null && leader != animal) {
            BlockPos shared = AnimalMemoryStore.get(leader).sharedWater(now);
            if (shared != null && animal.blockPosition().distSqr(shared) < 1024.0) {
                waterEdge = shared;
                return true;
            }
        }
        waterEdge = findWaterEdge(animal.level(), animal.blockPosition());
        if (waterEdge != null && leader == animal) {
            AnimalMemoryStore.get(animal).setSharedWater(waterEdge, now + 200);
        }
        return waterEdge != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (waterEdge == null) return false;
        if (AnimalMemoryStore.get(animal).dangerPos(animal.level().getGameTime()) != null) return false;
        return drinkTicks > 0 || !animal.getNavigation().isDone();
    }

    @Override
    public void start() {
        drinkTicks = TamekindConfig.drinkDurationTicks + animal.getRandom().nextInt(Math.max(1, TamekindConfig.drinkDurationTicks));
        if (animal.blockPosition().distSqr(waterEdge) > 3.0) {
            animal.getNavigation().moveTo(waterEdge.getX() + 0.5, waterEdge.getY(), waterEdge.getZ() + 0.5, 0.85);
        } else {
            animal.getNavigation().stop();
        }
    }

    @Override
    public void tick() {
        if (!animal.getNavigation().isDone()) return;
        drinkTicks--;
        animal.getLookControl().setLookAt(waterEdge.getX() + 0.5, waterEdge.getY(), waterEdge.getZ() + 0.5);
    }

    @Override
    public void stop() {
        waterEdge = null;
        drinkTicks = 0;
    }

    private BlockPos findWaterEdge(Level level, BlockPos origin) {
        BlockPos best = null;
        long bestDist = Long.MAX_VALUE;
        int radius = TamekindConfig.drinkSearchRadius;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -2; dy <= 1; dy++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (!isStandableNextToWater(level, cursor)) continue;
                    long dist = (long) dx * dx + (long) dz * dz + (long) dy * dy * 3L;
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = cursor.immutable();
                    }
                }
            }
        }
        return best;
    }

    private boolean isStandableNextToWater(Level level, BlockPos pos) {
        if (!level.getBlockState(pos).isAir()) return false;
        if (!level.getBlockState(pos.above()).isAir()) return false;
        if (level.getBlockState(pos.below()).is(TamekindTags.AVOID_BLOCKS)) return false;
        if (!level.getBlockState(pos.below()).isSolid()) return false;
        return level.getBlockState(pos.north()).is(TamekindTags.WATER_BLOCKS)
                || level.getBlockState(pos.south()).is(TamekindTags.WATER_BLOCKS)
                || level.getBlockState(pos.east()).is(TamekindTags.WATER_BLOCKS)
                || level.getBlockState(pos.west()).is(TamekindTags.WATER_BLOCKS);
    }
}
