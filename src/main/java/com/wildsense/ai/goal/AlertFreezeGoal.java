package com.tamekind.ai.goal;

import com.tamekind.ai.AiLod;
import com.tamekind.ai.ThreatScanner;
import com.tamekind.ai.TamekindAnimalRules;
import com.tamekind.config.TamekindConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public final class AlertFreezeGoal extends Goal implements TamekindGoal {
    private final Animal animal;
    private Entity threat;
    private int alertTicks;
    private int initialTicks;
    private int nextAllowedTick;

    public AlertFreezeGoal(Animal animal) {
        this.animal = animal;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!TamekindConfig.enabled || !TamekindConfig.alertEnabled || AiLod.forAnimal(animal) != AiLod.FULL) return false;
        if (TamekindAnimalRules.skipMovementGoals(animal)) return false;
        if (animal.tickCount < nextAllowedTick) return false;
        threat = ThreatScanner.nearestThreat(animal, TamekindConfig.alertRadius);
        if (threat == null) return false;
        return animal.distanceToSqr(threat) > TamekindConfig.panicRadius * TamekindConfig.panicRadius;
    }

    @Override
    public boolean canContinueToUse() {
        return threat != null && threat.isAlive() && alertTicks > 0
                && animal.distanceToSqr(threat) > TamekindConfig.panicRadius * TamekindConfig.panicRadius;
    }

    @Override
    public void start() {
        int rnd = Math.max(1, TamekindConfig.alertFreezeRandomTicks);
        int min = TamekindConfig.alertFreezeMinTicks;
        if (animal.isBaby()) { min = Math.max(5, min / 2); rnd = Math.max(1, rnd / 2); }
        alertTicks = min + animal.getRandom().nextInt(rnd);
        initialTicks = alertTicks;
        animal.getNavigation().stop();
    }

    @Override
    public void tick() {
        alertTicks--;
        if (threat != null) {
            animal.getLookControl().setLookAt(threat, 30.0F, 30.0F);
        }
        if (threat != null && alertTicks < initialTicks / 2 && animal.getNavigation().isDone()) {
            net.minecraft.world.phys.Vec3 away = animal.position().subtract(threat.position());
            if (away.lengthSqr() > 0.001) {
                away = away.normalize().scale(6.0);
                animal.getNavigation().moveTo(
                        animal.getX() + away.x,
                        animal.getY(),
                        animal.getZ() + away.z,
                        TamekindConfig.alertDriftSpeed);
            }
        }
    }

    @Override
    public void stop() {
        threat = null;
        alertTicks = 0;
        initialTicks = 0;
        nextAllowedTick = animal.tickCount + 60 + animal.getRandom().nextInt(60);
    }
}
