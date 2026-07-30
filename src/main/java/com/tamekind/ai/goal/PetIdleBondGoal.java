package com.tamekind.ai.goal;

import com.tamekind.ai.AnimalMemoryStore;
import com.tamekind.config.TamekindConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.UUID;

public final class PetIdleBondGoal extends Goal implements TamekindGoal {
    private final Animal animal;
    private int nextTickAt;

    public PetIdleBondGoal(Animal animal) {
        this.animal = animal;
        setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (!TamekindConfig.enabled || !TamekindConfig.trustEnabled) return false;
        if (!(animal.level() instanceof ServerLevel level)) return false;
        if (animal.tickCount < nextTickAt) return false;
        nextTickAt = animal.tickCount + TamekindConfig.idleBondTickInterval;
        UUID ownerId = null;
        if (animal instanceof TamableAnimal tame && tame.getOwner() != null) ownerId = tame.getOwner().getUUID();
        double radius = TamekindConfig.idleBondRadius;
        double bestDist = radius * radius;
        Player chosen = null;
        for (Player p : level.players()) {
            double d = p.distanceToSqr(animal);
            if (d > bestDist) continue;
            if (ownerId != null && p.getUUID().equals(ownerId)) {
                chosen = p;
                break;
            }
            if (ownerId == null && p instanceof ServerPlayer) {
                if (chosen == null || d < bestDist) {
                    chosen = p;
                    bestDist = d;
                }
            }
        }
        if (chosen != null && AnimalMemoryStore.get(animal).dangerPos(level.getGameTime()) == null) {
            long until = level.getGameTime() + TamekindConfig.trustTicks;
            double gain = TamekindConfig.idleBondTrustGain
                    * com.tamekind.ai.AnimalTemperament.forAnimal(animal).trustGainMultiplier();
            AnimalMemoryStore.get(animal).addTrust(chosen.getUUID(), gain, until);
        }
        return false;
    }
}
