package com.wildsense.ai.goal;

import com.wildsense.ai.AiLod;
import com.wildsense.ai.AnimalMemory;
import com.wildsense.ai.AnimalMemoryStore;
import com.wildsense.ai.DangerBroadcaster;
import com.wildsense.ai.HerdCoordinator;
import com.wildsense.ai.ThreatScanner;
import com.wildsense.ai.WildsenseAnimalRules;
import com.wildsense.compat.WildsenseTags;
import com.wildsense.config.WildsenseConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public final class WildPanicGoal extends Goal implements WildsenseGoal {
    private final Animal animal;
    private Vec3 danger;

    public WildPanicGoal(Animal animal) {
        this.animal = animal;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!WildsenseConfig.enabled || !WildsenseConfig.panicEnabled || AiLod.forAnimal(animal) != AiLod.FULL) return false;
        if (WildsenseAnimalRules.skipMovementGoals(animal)) return false;
        AnimalMemory memory = AnimalMemoryStore.get(animal);
        memory.tick(animal.level().getGameTime());
        Entity threat = ThreatScanner.nearestThreat(animal, WildsenseConfig.panicRadius);
        if (threat != null) {
            danger = threat.position();
            DangerBroadcaster.rememberAndSpread(animal, danger);
            return true;
        }
        danger = memory.dangerPos(animal.level().getGameTime());
        return danger != null && animal.distanceToSqr(danger) < WildsenseConfig.panicRadius * WildsenseConfig.panicRadius * 2.0;
    }

    @Override
    public boolean canContinueToUse() {
        return danger != null && !animal.getNavigation().isDone();
    }

    @Override
    public void start() {
        act();
    }

    @Override
    public void tick() {
        if (animal.tickCount % 12 == 0) {
            act();
        }
        if (WildsenseConfig.stampedeEnabled && HerdCoordinator.herdSize(animal) >= WildsenseConfig.minStampedeHerdSize) {
            applyStampedeBump();
        }
    }

    private void act() {
        if (danger == null) return;
        long now = animal.level().getGameTime();
        if (WildsenseConfig.parentGuardEnabled
                && !animal.isBaby()
                && AnimalMemoryStore.get(animal).isGuarding(now)) {
            Animal baby = nearestSameTypeBaby();
            if (baby != null) {
                animal.getNavigation().moveTo(baby, WildsenseConfig.panicSpeed);
                return;
            }
        }
        BlockPos escape = chooseEscapePos();
        if (escape == null) return;
        animal.getNavigation().moveTo(escape.getX() + 0.5, escape.getY(), escape.getZ() + 0.5, WildsenseConfig.panicSpeed);
    }

    private Animal nearestSameTypeBaby() {
        AABB box = animal.getBoundingBox().inflate(WildsenseConfig.parentGuardRadius);
        Animal best = null;
        double bestDist = Double.MAX_VALUE;
        for (Animal other : animal.level().getEntitiesOfClass(Animal.class, box,
                o -> o != animal && o.isAlive() && o.isBaby() && o.getType() == animal.getType())) {
            double d = other.distanceToSqr(animal);
            if (d < bestDist) {
                bestDist = d;
                best = other;
            }
        }
        return best;
    }

    private BlockPos chooseEscapePos() {
        Vec3 current = animal.position();
        Vec3 delta = current.subtract(danger);
        if (delta.lengthSqr() < 0.001) {
            delta = new Vec3(animal.getRandom().nextDouble() - 0.5, 0.0, animal.getRandom().nextDouble() - 0.5);
        }
        Vec3 away = delta.normalize();
        BlockPos best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        int candidates = Math.max(1, WildsenseConfig.panicCandidateCount);
        for (int i = 0; i < candidates; i++) {
            double spread = (i == 0) ? 0.0 : (animal.getRandom().nextDouble() - 0.5) * 1.8;
            double distance = WildsenseConfig.panicEscapeDistance * (0.75 + animal.getRandom().nextDouble() * 0.55);
            Vec3 direction = rotateY(away, spread);
            Vec3 target = current.add(direction.scale(distance));
            BlockPos candidate = settleEscapePos(BlockPos.containing(target.x, target.y, target.z));
            if (candidate == null) continue;
            double score = scoreEscape(candidate, direction);
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private BlockPos settleEscapePos(BlockPos start) {
        Level level = animal.level();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = 2; dy >= -WildsenseConfig.panicDropCheckDepth; dy--) {
            cursor.set(start.getX(), start.getY() + dy, start.getZ());
            if (isSafeStandPos(level, cursor)) {
                return cursor.immutable();
            }
        }
        return null;
    }

    private boolean isSafeStandPos(Level level, BlockPos pos) {
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState ground = level.getBlockState(pos.below());
        if (!feet.isAir() || !head.isAir()) return false;
        if (ground.isAir() || ground.is(WildsenseTags.AVOID_BLOCKS)) return false;
        if (feet.is(WildsenseTags.AVOID_BLOCKS) || head.is(WildsenseTags.AVOID_BLOCKS)) return false;
        if (!feet.getFluidState().isEmpty() || !head.getFluidState().isEmpty()) return false;
        return true;
    }

    private double scoreEscape(BlockPos pos, Vec3 intendedDirection) {
        Vec3 center = pos.getCenter();
        double distanceFromDanger = center.distanceToSqr(danger);
        Vec3 actual = center.subtract(animal.position());
        double alignment = actual.lengthSqr() < 0.001 ? 0.0 : actual.normalize().dot(intendedDirection);
        double score = distanceFromDanger + alignment * 20.0;
        Level level = animal.level();
        if (level.canSeeSky(pos) && level.isRaining()) score -= 8.0;
        if (level.getBlockState(pos.below()).is(WildsenseTags.GRAZING_BLOCKS)) score += 3.0;
        if (!level.isBrightOutside()) {
            score += level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos) * 2.0;
        }
        int waterNeighbors = 0;
        int blockedNeighbors = 0;
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockPos n = pos.relative(dir);
            if (!level.getBlockState(n).getFluidState().isEmpty()
                    || !level.getBlockState(n.below()).getFluidState().isEmpty()) {
                waterNeighbors++;
            }
            BlockState head = level.getBlockState(n.above());
            BlockState feet = level.getBlockState(n);
            if (!head.isAir() && !feet.isAir()) blockedNeighbors++;
        }
        score -= waterNeighbors * 25.0;
        if (blockedNeighbors >= 3) score -= 30.0;
        else if (blockedNeighbors == 2) score -= 8.0;
        return score;
    }

    private Vec3 rotateY(Vec3 vec, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(vec.x * cos - vec.z * sin, 0.0, vec.x * sin + vec.z * cos).normalize();
    }

    private void applyStampedeBump() {
        int herdSize = HerdCoordinator.herdSize(animal);
        AABB box = animal.getBoundingBox().inflate(0.45);
        Vec3 motion = animal.getDeltaMovement();
        if (motion.horizontalDistanceSqr() < 0.01) return;
        double scale = 1.0 + Math.max(0, herdSize - WildsenseConfig.minStampedeHerdSize) * WildsenseConfig.stampedeHerdScaling;
        Vec3 push = motion.normalize().scale(WildsenseConfig.stampedeKnockback * scale);
        for (Entity entity : animal.level().getEntities(animal, box, entity -> entity.isAlive() && entity.isPushable())) {
            if (entity instanceof Animal other && WildsenseAnimalRules.skipMovementGoals(other)) continue;
            entity.push(push.x, 0.08 * scale, push.z);
        }
    }
}
