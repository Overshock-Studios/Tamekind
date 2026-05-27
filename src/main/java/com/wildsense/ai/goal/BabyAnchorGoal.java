package com.tamekind.ai.goal;

import com.tamekind.ai.AiLod;
import com.tamekind.ai.AnimalMemoryStore;
import com.tamekind.ai.HerdCoordinator;
import com.tamekind.ai.ThreatScanner;
import com.tamekind.ai.TamekindAnimalRules;
import com.tamekind.config.TamekindConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public final class BabyAnchorGoal extends Goal implements TamekindGoal {
    private final Animal baby;
    private Animal adult;

    public BabyAnchorGoal(Animal baby) {
        this.baby = baby;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!TamekindConfig.enabled || !TamekindConfig.babyAnchoringEnabled || !baby.isBaby()) return false;
        if (TamekindAnimalRules.skipMovementGoals(baby)) return false;
        AiLod lod = AiLod.forAnimal(baby);
        if (lod == AiLod.SLEEP) return false;
        adult = HerdCoordinator.nearestAdultForBaby(baby, TamekindConfig.babyAnchorSearchRadius);
        if (adult == null) return false;
        Entity threat = ThreatScanner.nearestThreat(baby, TamekindConfig.alertRadius);
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
        baby.getNavigation().moveTo(adult, TamekindConfig.babyAnchorSpeed);
        if (adult != null) {
            var babyMem = AnimalMemoryStore.get(baby);
            if (babyMem.home() == null) {
                var adultHome = AnimalMemoryStore.get(adult).home();
                if (adultHome != null) babyMem.setHome(adultHome);
            }
        }
    }

    @Override
    public void tick() {
        if (adult == null) return;
        baby.getLookControl().setLookAt(adult, 30.0F, 30.0F);
        if (baby.tickCount % 10 == 0) {
            baby.getNavigation().moveTo(adult, TamekindConfig.babyAnchorSpeed);
        }
    }

    @Override
    public void stop() {
        adult = null;
    }
}
