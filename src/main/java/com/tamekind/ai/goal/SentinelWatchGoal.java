package com.tamekind.ai.goal;

import com.tamekind.ai.AiLod;
import com.tamekind.ai.AnimalMemoryStore;
import com.tamekind.ai.DangerBroadcaster;
import com.tamekind.ai.Disposition;
import com.tamekind.ai.HerdCoordinator;
import com.tamekind.ai.TamekindAnimalRules;
import com.tamekind.ai.ThreatScanner;
import com.tamekind.config.TamekindConfig;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;

import java.util.EnumSet;

/**
 * While the herd settles down to graze or rest, the alpha stands watch instead of
 * eating: it holds position, sweeps its head across the surroundings, and scans for
 * threats at a widened radius, broadcasting danger to the herd the moment it sees
 * something.
 *
 * <p>Sits at a higher priority than {@link GrazeRestGoal} and {@link DrinkGoal} so
 * the alpha is visibly the one animal not with its head down. That readable silhouette
 * is the point: the herd reacts earlier than any individual animal could, and the
 * player can see why.
 *
 * <p>Lifted from the sentinel behaviour in open-world wildlife sims rather than from
 * any Minecraft mod: no competitor in this niche models a lookout at all
 * (see {@code docs/FEATURE-GAP.md}).
 */
public final class SentinelWatchGoal extends Goal implements TamekindGoal {
    private final Animal animal;
    private int watchTicks;
    private int nextAllowedTick;
    private double sweep;

    public SentinelWatchGoal(Animal animal) {
        this.animal = animal;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!TamekindConfig.enabled || !TamekindConfig.herdEnabled || !TamekindConfig.sentinelEnabled) return false;
        if (TamekindAnimalRules.skipMovementGoals(animal)) return false;
        if (AiLod.forAnimal(animal) != AiLod.FULL) return false;
        if (animal.tickCount < nextAllowedTick) return false;
        if (animal.isBaby() || animal.isInLove()) return false;
        long now = animal.level().getGameTime();
        // Already fleeing something: panic outranks standing watch.
        if (AnimalMemoryStore.get(animal).dangerPos(now) != null) return false;
        // Only the animal currently on watch, and only for a herd worth watching over.
        // The shift rotates through the herd's adults so nobody guards forever.
        if (HerdCoordinator.sentinelFor(animal) != animal) return false;
        return HerdCoordinator.herdSize(animal) >= Math.max(2, TamekindConfig.alphaMinHerdSize);
    }

    @Override
    public boolean canContinueToUse() {
        if (watchTicks <= 0) return false;
        if (AnimalMemoryStore.get(animal).dangerPos(animal.level().getGameTime()) != null) return false;
        return HerdCoordinator.sentinelFor(animal) == animal;
    }

    @Override
    public void start() {
        int base = Math.max(20, TamekindConfig.sentinelWatchTicks);
        watchTicks = base + animal.getRandom().nextInt(base);
        sweep = animal.getRandom().nextDouble() * Math.PI * 2.0;
        animal.getNavigation().stop();
    }

    @Override
    public void tick() {
        watchTicks--;
        // Sweep the head around rather than fixating, so a watching alpha reads as
        // scanning and not as a stuck mob.
        sweep += 0.06;
        double radius = 8.0;
        animal.getLookControl().setLookAt(
                animal.getX() + Math.cos(sweep) * radius,
                animal.getEyeY(),
                animal.getZ() + Math.sin(sweep) * radius);

        if (animal.tickCount % 10 != 0) return;
        double scanRadius = TamekindConfig.alertRadius
                * TamekindConfig.sentinelAlertRadiusMultiplier
                * Disposition.alertMultiplier(animal);
        Entity threat = ThreatScanner.nearestThreat(animal, scanRadius);
        if (threat != null) {
            // The whole point of a lookout: the herd learns about it from the alpha.
            DangerBroadcaster.rememberAndSpread(animal, threat.position());
            watchTicks = 0;
        }
    }

    @Override
    public void stop() {
        watchTicks = 0;
        nextAllowedTick = animal.tickCount + 100 + animal.getRandom().nextInt(200);
    }
}
