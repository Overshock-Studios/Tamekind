package com.wildsense.ai.goal;

import com.wildsense.ai.AiLod;
import com.wildsense.ai.HerdCoordinator;
import com.wildsense.ai.ThreatScanner;
import com.wildsense.ai.WildsenseAnimalRules;
import com.wildsense.config.WildsenseConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public final class BabyAnchorGoal extends Goal implements WildsenseGoal {
    private final Animal baby;
    private Animal adult;

    public BabyAnchorGoal(Animal baby) {
        this.baby = baby;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!WildsenseConfig.enabled || !WildsenseConfig.babyAnchoringEnabled || !baby.isBaby()) return false;
        if (WildsenseAnimalRules.skipMovementGoals(baby)) return false;
        AiLod lod = AiLod.forAnimal(baby);
        if (lod == AiLod.SLEEP) return false;
        adult = HerdCoordinator.nearestAdultForBaby(baby, WildsenseConfig.babyAnchorSearchRadius);
        if (adult == null) return false;
        Entity threat = ThreatScanner.nearestThreat(baby, WildsenseConfig.alertRadius);
        return threat != null || baby.distanceToSqr(adult) > 16.0;
    }

    @Override
    public boolean canContinueToUse() {
        return baby.isBaby()
                && adult != null
                && adult.isAlive()
                && baby.distanceToSqr(adult) > 5.0
                && !baby.getNavigation().isDone();
    }

    @Override
    public void start() {
        baby.getNavigation().moveTo(adult, WildsenseConfig.babyAnchorSpeed);
    }

    @Override
    public void tick() {
        if (adult == null) return;
        baby.getLookControl().setLookAt(adult, 30.0F, 30.0F);
        if (baby.tickCount % 10 == 0) {
            baby.getNavigation().moveTo(adult, WildsenseConfig.babyAnchorSpeed);
        }
    }

    @Override
    public void stop() {
        adult = null;
    }
}
