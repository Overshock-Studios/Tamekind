package com.tamekind.ai.goal;

import com.tamekind.ai.AiLod;
import com.tamekind.ai.HerdCoordinator;
import com.tamekind.ai.TamekindAnimalRules;
import com.tamekind.config.TamekindConfig;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public final class LostBabyGoal extends Goal implements TamekindGoal {
    private static final int LOST_THRESHOLD_TICKS = 20 * 30;
    private static final int FREEZE_TICKS = 20 * 8;
    private final Animal baby;
    private int lonelyTicks;
    private int freezeTicks;

    public LostBabyGoal(Animal baby) {
        this.baby = baby;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!TamekindConfig.enabled || !TamekindConfig.babyAnchoringEnabled || !baby.isBaby()) return false;
        if (TamekindAnimalRules.skipMovementGoals(baby)) return false;
        if (AiLod.forAnimal(baby) == AiLod.SLEEP) return false;
        if (HerdCoordinator.nearestAdultForBaby(baby, TamekindConfig.babyAnchorSearchRadius * 2) != null) {
            lonelyTicks = 0;
            return false;
        }
        lonelyTicks++;
        if (lonelyTicks < LOST_THRESHOLD_TICKS) return false;
        freezeTicks = FREEZE_TICKS;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (freezeTicks <= 0) return false;
        return baby.isBaby() && HerdCoordinator.nearestAdultForBaby(baby, TamekindConfig.babyAnchorSearchRadius * 2) == null;
    }

    @Override
    public void start() {
        baby.getNavigation().stop();
        if (TamekindConfig.panicSoundEnabled) {
            baby.playSound(net.minecraft.sounds.SoundEvents.GENERIC_HURT,
                    TamekindConfig.panicSoundVolume,
                    1.4f);
        }
    }

    @Override
    public void tick() {
        freezeTicks--;
        baby.getNavigation().stop();
    }

    @Override
    public void stop() {
        lonelyTicks = 0;
        freezeTicks = 0;
    }
}
