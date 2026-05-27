package com.tamekind.ai;

import com.tamekind.compat.TamekindTags;
import com.tamekind.config.TamekindConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.animal.Animal;

public final class TamekindAnimalRules {
    private TamekindAnimalRules() {
    }

    private static final java.util.Set<net.minecraft.world.entity.EntityType<?>> RUNTIME_DISABLED =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    public static boolean isDisabled(Animal animal) {
        if (RUNTIME_DISABLED.contains(animal.getType())) return true;
        return BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(animal.getType()).is(TamekindTags.DISABLED);
    }

    public static boolean toggleRuntimeDisable(net.minecraft.world.entity.EntityType<?> type) {
        if (RUNTIME_DISABLED.remove(type)) return false;
        RUNTIME_DISABLED.add(type);
        return true;
    }

    public static boolean skipMovementGoals(Animal animal) {
        if (isDisabled(animal)) return true;
        if (TamekindConfig.respectLeashedAnimals && animal.isLeashed()) return true;
        if (TamekindConfig.respectMountedAnimals && (animal.isPassenger() || animal.isVehicle())) return true;
        if (TamekindConfig.respectNamedAnimals && animal.hasCustomName()) return true;
        return TamekindConfig.respectBreedingAnimals && animal.isInLove();
    }
}
