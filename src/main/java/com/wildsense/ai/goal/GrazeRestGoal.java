package com.wildsense.ai.goal;

import com.wildsense.ai.AiLod;
import com.wildsense.ai.AnimalMemoryStore;
import com.wildsense.ai.WildsenseAnimalRules;
import com.wildsense.compat.WildsenseTags;
import com.wildsense.config.WildsenseConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

public final class GrazeRestGoal extends Goal implements WildsenseGoal {
    private final Animal animal;
    private BlockPos grazingSpot;
    private int nextTryTick;
    private int grazeTicks;

    public GrazeRestGoal(Animal animal) {
        this.animal = animal;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!WildsenseConfig.enabled || !WildsenseConfig.dailyRhythmEnabled) return false;
        if (WildsenseAnimalRules.skipMovementGoals(animal)) return false;
        if (AiLod.forAnimal(animal) != AiLod.FULL) return false;
        if (animal.tickCount < nextTryTick) return false;
        nextTryTick = animal.tickCount + WildsenseConfig.grazeMinIntervalTicks
                + animal.getRandom().nextInt(Math.max(1, WildsenseConfig.grazeMinIntervalTicks));
        if (AnimalMemoryStore.get(animal).dangerPos(animal.level().getGameTime()) != null) return false;
        if (animal.isInLove() || animal.isBaby()) return false;
        grazingSpot = findGrazingSpot(animal.level(), animal.blockPosition());
        return grazingSpot != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (grazingSpot == null) return false;
        if (AnimalMemoryStore.get(animal).dangerPos(animal.level().getGameTime()) != null) return false;
        return grazeTicks > 0 || !animal.getNavigation().isDone();
    }

    @Override
    public void start() {
        int base = WildsenseConfig.grazeDurationTicks;
        if (!animal.level().isBrightOutside()) base *= 3;
        grazeTicks = base + animal.getRandom().nextInt(Math.max(1, base));
        if (animal.blockPosition().distSqr(grazingSpot) > 3.0) {
            animal.getNavigation().moveTo(grazingSpot.getX() + 0.5, grazingSpot.getY(), grazingSpot.getZ() + 0.5, 0.8);
        } else {
            animal.getNavigation().stop();
        }
    }

    @Override
    public void tick() {
        if (!animal.getNavigation().isDone()) return;
        grazeTicks--;
        animal.getLookControl().setLookAt(grazingSpot.getX() + 0.5, grazingSpot.getY() - 0.2, grazingSpot.getZ() + 0.5);
    }

    @Override
    public void stop() {
        if (grazingSpot != null && animal.blockPosition().distSqr(grazingSpot) < 9.0) {
            AnimalMemoryStore.get(animal).setHome(grazingSpot);
        }
        grazingSpot = null;
        grazeTicks = 0;
    }

    private BlockPos findGrazingSpot(Level level, BlockPos origin) {
        if (isRestStandPos(level, origin)) return origin;
        boolean night = !level.isBrightOutside();
        BlockPos best = null;
        long bestDist = Long.MAX_VALUE;
        int radius = WildsenseConfig.grazeSearchRadius;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (!isRestStandPos(level, cursor)) continue;
                    long dist = (long) dx * dx + (long) dz * dz + (long) dy * dy * 3L;
                    if (level.getBlockState(cursor).is(WildsenseTags.SOFT_AVOID_BLOCKS)) dist += 12;
                    if (night && level.getBlockState(cursor.below()).is(WildsenseTags.COMFORT_BLOCKS)) dist -= 10;
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = cursor.immutable();
                    }
                }
            }
        }
        return best;
    }

    private boolean isRestStandPos(Level level, BlockPos pos) {
        if (!level.getBlockState(pos).isAir()) return false;
        if (!level.getBlockState(pos.above()).isAir()) return false;
        if (level.getBlockState(pos.below()).is(WildsenseTags.AVOID_BLOCKS)) return false;
        return level.getBlockState(pos.below()).is(WildsenseTags.GRAZING_BLOCKS)
                || level.getBlockState(pos.below()).is(WildsenseTags.COMFORT_BLOCKS);
    }
}
