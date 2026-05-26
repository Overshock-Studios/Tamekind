package com.wildsense.ai.goal;

import com.wildsense.ai.AiLod;
import com.wildsense.ai.AnimalMemoryStore;
import com.wildsense.ai.HerdCoordinator;
import com.wildsense.ai.WildsenseAnimalRules;
import com.wildsense.config.WildsenseConfig;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public final class HerdFollowGoal extends Goal implements WildsenseGoal {
    private final Animal animal;
    private Animal leader;

    public HerdFollowGoal(Animal animal) {
        this.animal = animal;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!WildsenseConfig.enabled || !WildsenseConfig.herdEnabled) return false;
        if (WildsenseAnimalRules.skipMovementGoals(animal)) return false;
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
        animal.getNavigation().moveTo(leader, WildsenseConfig.herdFollowSpeed);
    }

    @Override
    public void tick() {
        if (leader != null && animal.tickCount % 20 == 0) {
            animal.getNavigation().moveTo(leader, WildsenseConfig.herdFollowSpeed);
        }
    }

    @Override
    public void stop() {
        leader = null;
    }
}
