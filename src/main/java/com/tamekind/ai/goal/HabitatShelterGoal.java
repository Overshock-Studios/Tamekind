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
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

public final class HabitatShelterGoal extends Goal implements TamekindGoal {
    private final Animal animal;
    private BlockPos shelter;
    private int nextScanTick;

    public HabitatShelterGoal(Animal animal) {
        this.animal = animal;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!TamekindConfig.enabled || !TamekindConfig.habitatEnabled || AiLod.forAnimal(animal) != AiLod.FULL) return false;
        if (TamekindAnimalRules.skipMovementGoals(animal)) return false;
        if (animal.tickCount < nextScanTick) return false;
        Level level = animal.level();
        boolean thundering = level.isThundering();
        int cooldown = thundering ? 20 : 80;
        nextScanTick = animal.tickCount + cooldown + animal.getRandom().nextInt(cooldown);
        boolean injured = animal.getHealth() < animal.getMaxHealth() * TamekindConfig.lowHpThresholdFraction;
        long dt = level.getGameTime() % 24000L;
        var season = com.tamekind.ai.BreedingSeason.current(level);
        boolean midday = dt > 4000L && dt < 8000L;
        boolean heatSensitive = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(animal.getType()).is(TamekindTags.HEAT_SENSITIVE);
        boolean heatStressed = level.isBrightOutside() && !level.isRaining()
                && heatSensitive && level.canSeeSky(animal.blockPosition())
                && (midday || season == com.tamekind.ai.BreedingSeason.Season.SUMMER);
        boolean winter = season == com.tamekind.ai.BreedingSeason.Season.WINTER;
        boolean raid = level instanceof net.minecraft.server.level.ServerLevel sl
                && com.tamekind.compat.WarbandCompat.activeRaidNear(sl, animal.blockPosition(), 32);
        if (!injured && !heatStressed && !winter && !raid && !level.isRaining() && level.isBrightOutside()) return false;

        long now = level.getGameTime();
        Animal leader = HerdCoordinator.leaderFor(animal);
        BlockPos selfShared = AnimalMemoryStore.get(animal).sharedShelter(now);
        if (leader == animal && selfShared != null && animal.blockPosition().distSqr(selfShared) > 6.0) {
            shelter = selfShared;
            return true;
        }
        if (leader != null && leader != animal) {
            BlockPos shared = AnimalMemoryStore.get(leader).sharedShelter(now);
            if (shared != null && animal.blockPosition().distSqr(shared) > 6.0) {
                shelter = shared;
                return true;
            }
        }
        shelter = findShelter(level, animal.blockPosition());
        if (shelter != null && animal.blockPosition().distSqr(shelter) > 6.0) {
            if (leader == animal && !animal.isVehicle()) {
                AnimalMemoryStore.get(animal).setSharedShelter(shelter, now + 200);
            }
            return true;
        }
        if (TamekindConfig.scaredNoCoverEnabled
                && AnimalMemoryStore.get(animal).dangerPos(now) == null) {
            AnimalMemoryStore.get(animal).rememberDanger(
                    animal.position(), now + TamekindConfig.scaredNoCoverDurationTicks);
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return shelter != null && !animal.getNavigation().isDone() && animal.blockPosition().distSqr(shelter) > 4.0;
    }

    @Override
    public void start() {
        double speed = animal.level().isThundering()
                ? TamekindConfig.shelterSpeed * 1.4
                : TamekindConfig.shelterSpeed;
        animal.getNavigation().moveTo(shelter.getX() + 0.5, shelter.getY(), shelter.getZ() + 0.5, speed);
    }

    @Override
    public void stop() {
        shelter = null;
    }

    /**
     * Finds the best sheltered spot within the search radius.
     *
     * <p>Exhaustive, and deliberately so. Sampling was tried first, copying vanilla's
     * {@code FleeSunGoal}, and it does not transfer: vanilla is looking for *any* shade for
     * a burning mob standing in terrain, where shade is abundant, while this is looking for
     * a *specific* shelter in a mostly open field. A 5x5 hut is 0.6% of the default search
     * volume, so 48 probes found it about a quarter of the time and a wounded herd of five
     * reached cover once out of five. Correctness wins here.
     *
     * <p>What made the old version expensive was reading every cell of a 31x31x9 volume,
     * 8,649 block reads, herd-wide the instant rain starts. The saving is a column
     * short-circuit instead: {@code canSeeSky} is monotonic going up, so if the lowest cell
     * in a column sees sky then every cell above it does too, and the whole column can be
     * dismissed after one read unless something tagged as shelter sits over it. In the open
     * field that is the common case, which turns 8,649 reads into roughly 961.
     *
     * <p>Also folds in {@code getWalkTargetValue}, which vanilla consults and the old scan
     * ignored. It is the species-aware "does this mob like this ground" judgement, so a cow
     * now prefers grass under its roof at no cost.
     */
    private BlockPos findShelter(Level level, BlockPos origin) {
        int radius = TamekindConfig.shelterSearchRadius;
        int vertical = TamekindConfig.shelterVerticalRadius;
        boolean night = !level.isBrightOutside();

        BlockPos best = null;
        long bestScore = Long.MAX_VALUE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                // One read decides the whole column in open terrain.
                cursor.set(origin.getX() + dx, origin.getY() - vertical, origin.getZ() + dz);
                boolean openColumn = level.canSeeSky(cursor);

                for (int dy = -vertical; dy <= vertical; dy++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    boolean taggedShelter =
                            level.getBlockState(cursor.above()).is(TamekindTags.SHELTER_BLOCKS);
                    if (openColumn && !taggedShelter) continue;
                    if (!level.getBlockState(cursor).isAir()) continue;
                    BlockState ground = level.getBlockState(cursor.below());
                    if (ground.isAir() || ground.is(TamekindTags.AVOID_BLOCKS)) continue;
                    if (!taggedShelter && level.canSeeSky(cursor)) continue;

                    long score = (long) dx * dx + (long) dz * dz + (long) dy * dy * 3L;
                    if (taggedShelter) score -= 8;
                    if (ground.is(TamekindTags.GRAZING_BLOCKS)) score -= 2;
                    if (ground.is(TamekindTags.COMFORT_BLOCKS)) score -= 6;
                    if (ground.is(TamekindTags.SOFT_AVOID_BLOCKS)
                            || level.getBlockState(cursor).is(TamekindTags.SOFT_AVOID_BLOCKS)) {
                        score += 10;
                    }
                    if (night) {
                        score -= level.getBrightness(
                                net.minecraft.world.level.LightLayer.BLOCK, cursor);
                    }
                    // Negative means the mob wants this ground, so subtracting rewards it.
                    score += (long) (animal.getWalkTargetValue(cursor, level) * -4.0f);

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
