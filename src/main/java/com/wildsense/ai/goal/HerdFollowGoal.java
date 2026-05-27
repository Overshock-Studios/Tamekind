package com.tamekind.ai.goal;

import com.tamekind.ai.AiLod;
import com.tamekind.ai.AnimalMemoryStore;
import com.tamekind.ai.HerdCoordinator;
import com.tamekind.ai.TamekindAnimalRules;
import com.tamekind.config.TamekindConfig;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.goal.Goal;

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
        animal.getNavigation().moveTo(leader, TamekindConfig.herdFollowSpeed);
    }

    @Override
    public void tick() {
        if (leader != null && animal.tickCount % 20 == 0) {
            animal.getNavigation().moveTo(leader, TamekindConfig.herdFollowSpeed);
        }
    }

    @Override
    public void stop() {
        leader = null;
    }
}
