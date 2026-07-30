package com.tamekind.ai.goal;

import com.tamekind.ai.AiLod;
import com.tamekind.ai.AnimalMemoryStore;
import com.tamekind.ai.HerdCoordinator;
import com.tamekind.config.TamekindConfig;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Records the herd leader's recent positions so followers can walk its route instead of
 * cutting a straight line at it. See {@link HerdFollowGoal#follow}.
 *
 * <p>Holds no movement flags and always returns {@code false} from {@link #canUse()} —
 * it is a bookkeeping ticker in goal clothing, the same shape as
 * {@link AgeScaleGoal} and {@link PetIdleBondGoal}.
 */
public final class HerdTrailGoal extends Goal implements TamekindGoal {
    private static final int TRAIL_EVERY_TICKS = 20;
    private static final int TRAIL_POINTS = 8;

    private final Animal animal;

    public HerdTrailGoal(Animal animal) {
        this.animal = animal;
        setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (!TamekindConfig.enabled || !TamekindConfig.herdEnabled) return false;
        if (AiLod.forAnimal(animal) == AiLod.SLEEP || AiLod.forAnimal(animal) == AiLod.HIBERNATE) return false;
        if (!HerdCoordinator.isHerdable(animal)) return false;

        long now = animal.level().getGameTime();
        if (HerdCoordinator.leaderFor(animal) != animal) {
            // Not the leader any more: drop the trail so a demoted animal does not
            // leave followers chasing a route it is no longer walking.
            AnimalMemoryStore.get(animal).clearTrail();
            return false;
        }
        if (!animal.getNavigation().isDone()) {
            AnimalMemoryStore.get(animal).pushTrail(
                    animal.blockPosition(), now, TRAIL_EVERY_TICKS, TRAIL_POINTS);
        }
        return false;
    }
}
