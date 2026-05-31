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

public final class GrazeRestGoal extends Goal implements TamekindGoal {
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
        if (!TamekindConfig.enabled || !TamekindConfig.dailyRhythmEnabled) return false;
        if (TamekindAnimalRules.skipMovementGoals(animal)) return false;
        if (AiLod.forAnimal(animal) != AiLod.FULL) return false;
        if (animal.tickCount < nextTryTick) return false;
        nextTryTick = animal.tickCount + TamekindConfig.grazeMinIntervalTicks
                + animal.getRandom().nextInt(Math.max(1, TamekindConfig.grazeMinIntervalTicks));
        if (AnimalMemoryStore.get(animal).dangerPos(animal.level().getGameTime()) != null) return false;
        if (animal.isInLove() || animal.isBaby()) return false;
        long now = animal.level().getGameTime();
        Animal leader = HerdCoordinator.leaderFor(animal);
        if (leader != null && leader != animal) {
            BlockPos shared = AnimalMemoryStore.get(leader).sharedGraze(now);
            if (shared != null && animal.blockPosition().distSqr(shared) < 1024.0) {
                grazingSpot = shared;
                return true;
            }
        }
        grazingSpot = findGrazingSpot(animal.level(), animal.blockPosition());
        if (grazingSpot != null && leader == animal && !animal.isVehicle()) {
            AnimalMemoryStore.get(animal).setSharedGraze(grazingSpot, now + 200);
        }
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
        int base = TamekindConfig.grazeDurationTicks;
        if (!animal.level().isBrightOutside()) base *= 3;
        if (com.tamekind.ai.BiomeComfort.isInComfortBiome(animal)) base = (int) (base * 1.5);
        var season = com.tamekind.ai.BreedingSeason.current(animal.level());
        if (season == com.tamekind.ai.BreedingSeason.Season.WINTER) base = (int) (base * 0.5);
        else if (season == com.tamekind.ai.BreedingSeason.Season.SPRING) base = (int) (base * 1.3);
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
        if (animal.level() instanceof net.minecraft.server.level.ServerLevel server
                && animal.getRandom().nextInt(400) == 0) {
            net.minecraft.core.BlockPos below = grazingSpot.below();
            var state = server.getBlockState(below);
            if (state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                server.levelEvent(2001, below,
                        net.minecraft.core.registries.BuiltInRegistries.BLOCK.getId(state.getBlock()));
                server.setBlock(below, net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState(), 2);
            }
        }
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
        double cx = 0, cz = 0;
        int herdN = 0;
        if (night) {
            for (Animal h : HerdCoordinator.nearbyHerd(animal)) { cx += h.getX(); cz += h.getZ(); herdN++; }
        }
        boolean cluster = night && herdN > 0;
        if (cluster) { cx /= herdN; cz /= herdN; }
        BlockPos best = null;
        long bestDist = Long.MAX_VALUE;
        int radius = TamekindConfig.grazeSearchRadius;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (!isRestStandPos(level, cursor)) continue;
                    long dist = (long) dx * dx + (long) dz * dz + (long) dy * dy * 3L;
                    if (level.getBlockState(cursor).is(TamekindTags.SOFT_AVOID_BLOCKS)) dist += 12;
                    if (night && level.getBlockState(cursor.below()).is(TamekindTags.COMFORT_BLOCKS)) dist -= 10;
                    if (cluster) {
                        double ddx = cursor.getX() + 0.5 - cx;
                        double ddz = cursor.getZ() + 0.5 - cz;
                        dist += (long) (ddx * ddx + ddz * ddz) / 2;
                    }
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
        if (level.getBlockState(pos.below()).is(TamekindTags.AVOID_BLOCKS)) return false;
        return level.getBlockState(pos.below()).is(TamekindTags.GRAZING_BLOCKS)
                || level.getBlockState(pos.below()).is(TamekindTags.COMFORT_BLOCKS);
    }
}
