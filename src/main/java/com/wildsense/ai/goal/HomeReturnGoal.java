package com.wildsense.ai.goal;

import com.wildsense.ai.AiLod;
import com.wildsense.ai.AnimalMemory;
import com.wildsense.ai.AnimalMemoryStore;
import com.wildsense.ai.WildsenseAnimalRules;
import com.wildsense.config.WildsenseConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public final class HomeReturnGoal extends Goal implements WildsenseGoal {
    private final Animal animal;
    private BlockPos home;
    private int nextTryTick;

    public HomeReturnGoal(Animal animal) {
        this.animal = animal;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!WildsenseConfig.enabled || !WildsenseConfig.homeReturnEnabled) return false;
        if (WildsenseAnimalRules.skipMovementGoals(animal)) return false;
        if (AiLod.forAnimal(animal) != AiLod.FULL) return false;
        if (animal.tickCount < nextTryTick) return false;
        nextTryTick = animal.tickCount + WildsenseConfig.homeReturnIntervalTicks
                + animal.getRandom().nextInt(Math.max(1, WildsenseConfig.homeReturnIntervalTicks));
        AnimalMemory memory = AnimalMemoryStore.get(animal);
        if (memory.dangerPos(animal.level().getGameTime()) != null) return false;
        if (animal.isInLove() || animal.isBaby()) return false;
        BlockPos h = memory.home();
        if (h == null) return false;
        double distSqr = animal.blockPosition().distSqr(h);
        double min = WildsenseConfig.homeReturnMinDistance;
        double max = WildsenseConfig.homeReturnMaxDistance;
        if (distSqr < min * min || distSqr > max * max) return false;
        home = h;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return home != null && !animal.getNavigation().isDone()
                && animal.blockPosition().distSqr(home) > 9.0;
    }

    @Override
    public void start() {
        animal.getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, WildsenseConfig.homeReturnSpeed);
    }

    @Override
    public void stop() {
        home = null;
    }
}
