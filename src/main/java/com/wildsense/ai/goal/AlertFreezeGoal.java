package com.wildsense.ai.goal;

import com.wildsense.ai.AiLod;
import com.wildsense.ai.ThreatScanner;
import com.wildsense.ai.WildsenseAnimalRules;
import com.wildsense.config.WildsenseConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public final class AlertFreezeGoal extends Goal implements WildsenseGoal {
    private final Animal animal;
    private Entity threat;
    private int alertTicks;

    public AlertFreezeGoal(Animal animal) {
        this.animal = animal;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!WildsenseConfig.enabled || !WildsenseConfig.alertEnabled || AiLod.forAnimal(animal) != AiLod.FULL) return false;
        if (WildsenseAnimalRules.skipMovementGoals(animal)) return false;
        threat = ThreatScanner.nearestThreat(animal, WildsenseConfig.alertRadius);
        if (threat == null) return false;
        return animal.distanceToSqr(threat) > WildsenseConfig.panicRadius * WildsenseConfig.panicRadius;
    }

    @Override
    public boolean canContinueToUse() {
        return threat != null && threat.isAlive() && alertTicks > 0
                && animal.distanceToSqr(threat) > WildsenseConfig.panicRadius * WildsenseConfig.panicRadius;
    }

    @Override
    public void start() {
        alertTicks = 30 + animal.getRandom().nextInt(30);
        animal.getNavigation().stop();
    }

    @Override
    public void tick() {
        alertTicks--;
        if (threat != null) {
            animal.getLookControl().setLookAt(threat, 30.0F, 30.0F);
        }
    }

    @Override
    public void stop() {
        threat = null;
        alertTicks = 0;
    }
}
