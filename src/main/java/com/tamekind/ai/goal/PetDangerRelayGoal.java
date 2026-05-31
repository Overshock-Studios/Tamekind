package com.tamekind.ai.goal;

import com.tamekind.ai.AnimalMemoryStore;
import com.tamekind.config.TamekindConfig;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public final class PetDangerRelayGoal extends Goal implements TamekindGoal {
    private final Animal pet;
    private Vec3 lookAt;
    private int lookTicks;
    private int nextTryTick;

    public PetDangerRelayGoal(Animal pet) {
        this.pet = pet;
        setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!TamekindConfig.enabled) return false;
        if (!(pet instanceof TamableAnimal tame) || tame.getOwner() == null) return false;
        if (pet.tickCount < nextTryTick) return false;
        nextTryTick = pet.tickCount + 40;
        var owner = tame.getOwner();
        if (owner.distanceToSqr(pet) > 64.0) return false;
        long now = pet.level().getGameTime();
        AABB box = pet.getBoundingBox().inflate(12.0);
        for (Animal other : pet.level().getEntitiesOfClass(Animal.class, box,
                o -> o != pet && o.isAlive() && !(o instanceof TamableAnimal))) {
            Vec3 d = AnimalMemoryStore.get(other).dangerPos(now);
            if (d != null) {
                lookAt = d;
                lookTicks = 30;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return lookTicks > 0 && lookAt != null;
    }

    @Override
    public void tick() {
        if (lookAt != null) pet.getLookControl().setLookAt(lookAt.x, lookAt.y, lookAt.z);
        lookTicks--;
    }

    @Override
    public void stop() {
        lookAt = null;
        lookTicks = 0;
    }
}
