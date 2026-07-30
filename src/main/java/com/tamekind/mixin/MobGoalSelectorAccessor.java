package com.tamekind.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Accessor for injecting Tamekind goals without widening all of {@link Mob}. */
@Mixin(Mob.class)
public interface MobGoalSelectorAccessor {

    @Accessor("goalSelector")
    GoalSelector tamekind$goalSelector();

    @Accessor("targetSelector")
    GoalSelector tamekind$targetSelector();
}
