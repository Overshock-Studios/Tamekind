package com.tamekind.ai.goal;

import com.tamekind.ai.AiLod;
import com.tamekind.ai.AnimalMemory;
import com.tamekind.ai.AnimalMemoryStore;
import com.tamekind.ai.TamekindAnimalRules;
import com.tamekind.config.TamekindConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public final class HomeReturnGoal extends Goal implements TamekindGoal {
    private final Animal animal;
    private BlockPos home;
    private int nextTryTick;

    public HomeReturnGoal(Animal animal) {
        this.animal = animal;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!TamekindConfig.enabled || !TamekindConfig.homeReturnEnabled) return false;
        if (TamekindAnimalRules.skipMovementGoals(animal)) return false;
        if (AiLod.forAnimal(animal) != AiLod.FULL) return false;
        if (animal.tickCount < nextTryTick) return false;
        nextTryTick = animal.tickCount + TamekindConfig.homeReturnIntervalTicks
                + animal.getRandom().nextInt(Math.max(1, TamekindConfig.homeReturnIntervalTicks));
        AnimalMemory memory = AnimalMemoryStore.get(animal);
        if (memory.dangerPos(animal.level().getGameTime()) != null) return false;
        if (animal.isInLove() || animal.isBaby()) return false;
        BlockPos h = memory.home();
        if (h == null) return false;
        double distSqr = animal.blockPosition().distSqr(h);
        double min = TamekindConfig.homeReturnMinDistance;
        double max = TamekindConfig.homeReturnMaxDistance;
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
        animal.getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, TamekindConfig.homeReturnSpeed);
    }

    @Override
    public void stop() {
        home = null;
    }
}
