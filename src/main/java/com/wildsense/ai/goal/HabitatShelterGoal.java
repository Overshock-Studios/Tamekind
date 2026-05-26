package com.wildsense.ai.goal;

import com.wildsense.ai.AiLod;
import com.wildsense.ai.WildsenseAnimalRules;
import com.wildsense.compat.WildsenseTags;
import com.wildsense.config.WildsenseConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

public final class HabitatShelterGoal extends Goal implements WildsenseGoal {
    private final Animal animal;
    private BlockPos shelter;
    private int nextScanTick;

    public HabitatShelterGoal(Animal animal) {
        this.animal = animal;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!WildsenseConfig.enabled || !WildsenseConfig.habitatEnabled || AiLod.forAnimal(animal) != AiLod.FULL) return false;
        if (WildsenseAnimalRules.skipMovementGoals(animal)) return false;
        if (animal.tickCount < nextScanTick) return false;
        nextScanTick = animal.tickCount + 80 + animal.getRandom().nextInt(80);
        Level level = animal.level();
        if (!level.isRaining() && level.isBrightOutside()) return false;
        shelter = findShelter(level, animal.blockPosition());
        return shelter != null && animal.blockPosition().distSqr(shelter) > 6.0;
    }

    @Override
    public boolean canContinueToUse() {
        return shelter != null && !animal.getNavigation().isDone() && animal.blockPosition().distSqr(shelter) > 4.0;
    }

    @Override
    public void start() {
        animal.getNavigation().moveTo(shelter.getX() + 0.5, shelter.getY(), shelter.getZ() + 0.5, WildsenseConfig.shelterSpeed);
    }

    @Override
    public void stop() {
        shelter = null;
    }

    private BlockPos findShelter(Level level, BlockPos origin) {
        BlockPos best = null;
        long bestScore = Long.MAX_VALUE;
        int radius = WildsenseConfig.shelterSearchRadius;
        boolean night = !level.isBrightOutside();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (!level.getBlockState(cursor).isAir()) continue;
                    if (level.getBlockState(cursor.below()).isAir()) continue;
                    if (level.getBlockState(cursor.below()).is(WildsenseTags.AVOID_BLOCKS)) continue;
                    boolean covered = !level.canSeeSky(cursor);
                    boolean taggedShelter = level.getBlockState(cursor.above()).is(WildsenseTags.SHELTER_BLOCKS)
                            || level.getBlockState(cursor.below()).is(WildsenseTags.GRAZING_BLOCKS);
                    if (!covered && !taggedShelter) continue;
                    long score = (long) dx * dx + (long) dz * dz + (long) dy * dy * 3L;
                    if (taggedShelter) score -= 8;
                    if (level.getBlockState(cursor.below()).is(WildsenseTags.COMFORT_BLOCKS)) score -= 6;
                    if (night) {
                        int blockLight = level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, cursor);
                        score -= blockLight;
                    }
                    if (score < bestScore) {
                        bestScore = score;
                        best = cursor.immutable();
                    }
                }
            }
        }
        return best;
    }
}
