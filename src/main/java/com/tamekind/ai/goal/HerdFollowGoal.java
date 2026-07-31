package com.tamekind.ai.goal;

import com.tamekind.ai.AiLod;
import com.tamekind.ai.AnimalMemoryStore;
import com.tamekind.ai.Disposition;
import com.tamekind.ai.HerdCoordinator;
import com.tamekind.ai.TamekindAnimalRules;
import com.tamekind.config.TamekindConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;

import java.util.EnumSet;

public final class HerdFollowGoal extends Goal implements TamekindGoal {
    private final Animal animal;
    private Animal leader;

    public HerdFollowGoal(Animal animal) {
        this.animal = animal;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!TamekindConfig.enabled || !TamekindConfig.herdEnabled) return false;
        if (TamekindAnimalRules.skipMovementGoals(animal)) return false;
        AiLod lod = AiLod.forAnimal(animal);
        if (lod == AiLod.SLEEP || !HerdCoordinator.isHerdable(animal)) return false;
        leader = HerdCoordinator.leaderFor(animal);
        if (leader == null || leader == animal) return false;
        long now = animal.level().getGameTime();
        if (AnimalMemoryStore.get(leader).dangerPos(now) != null) return false;
        return animal.distanceToSqr(leader) > 36.0;
    }

    @Override
    public boolean canContinueToUse() {
        return leader != null && leader.isAlive() && animal.distanceToSqr(leader) > 16.0 && !animal.getNavigation().isDone();
    }

    @Override
    public void start() {
        follow();
    }

    @Override
    public void tick() {
        if (leader != null && animal.tickCount % 20 == 0) {
            follow();
        }
    }

    /**
     * Walks the leader's recorded trail rather than making a beeline for it.
     *
     * <p>Heading straight at a leader makes a herd converge into one clump and shove
     * itself through terrain the leader already picked a way around. Following the
     * oldest still-nearby trail point instead puts the herd in a line along a route
     * that is known to be walkable. Falls back to the leader directly when no usable
     * trail point exists: a leader that has not moved has nothing to follow.
     */
    private void follow() {
        double speed = TamekindConfig.herdFollowSpeed * Disposition.speedMultiplier(animal);
        BlockPos point = AnimalMemoryStore.get(leader)
                .trailPointFor(animal.blockPosition(), TamekindConfig.herdSearchRadius
                        * (double) TamekindConfig.herdSearchRadius);
        if (point != null) {
            animal.getNavigation().moveTo(point.getX() + 0.5, point.getY(), point.getZ() + 0.5, speed);
            return;
        }
        animal.getNavigation().moveTo(leader, speed);
    }

    @Override
    public void stop() {
        leader = null;
    }
}
