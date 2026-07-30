package com.tamekind.ai.goal;

import com.tamekind.ai.AiLod;
import com.tamekind.ai.AnimalMemoryStore;
import com.tamekind.config.TamekindConfig;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Drains body condition over time. Off unless {@code conditionEnabled}.
 *
 * <p>Deliberately non-lethal. Condition never falls below {@code conditionFloor} and
 * never deals damage: a neglected animal becomes slower and declines to breed, and that
 * is the whole consequence. Livestock cannot starve to death while a player is away,
 * which is the objection that sinks every hunger system in this genre.
 *
 * <p>A bookkeeping ticker with no movement flags; {@link #canUse()} always returns
 * {@code false}.
 */
public final class ConditionGoal extends Goal implements TamekindGoal {
    private final Animal animal;

    public ConditionGoal(Animal animal) {
        this.animal = animal;
        setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (!TamekindConfig.enabled || !TamekindConfig.conditionEnabled) return false;
        // Only drain where the animal is actually being simulated, so a herd nobody has
        // visited for a week does not come back emaciated.
        if (AiLod.forAnimal(animal) != AiLod.FULL) return false;
        if (animal.isBaby()) return false;
        AnimalMemoryStore.get(animal).decayCondition(
                animal.level().getGameTime(),
                TamekindConfig.conditionDecayIntervalTicks,
                TamekindConfig.conditionDecayPerInterval,
                TamekindConfig.conditionFloor);
        return false;
    }
}
