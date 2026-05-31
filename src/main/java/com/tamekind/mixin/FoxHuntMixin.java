package com.tamekind.mixin;

import com.tamekind.ai.TagHunting;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.fox.Fox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Fox.class)
public abstract class FoxHuntMixin {

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void tamekind$addTagHunt(CallbackInfo ci) {
        Fox self = (Fox) (Object) this;
        try {
            var f = PathfinderMob.class.getSuperclass().getDeclaredField("targetSelector");
            f.setAccessible(true);
            GoalSelector ts = (GoalSelector) f.get(self);
            ts.addGoal(7, new NearestAttackableTargetGoal(self, Animal.class, 10, true, false,
                    (e, l) -> TagHunting.shouldHunt(self, (Animal) e)));
        } catch (Throwable ignored) {
        }
    }
}
