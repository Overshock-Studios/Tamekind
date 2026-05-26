package com.wildsense.ai;

import com.wildsense.compat.WildsenseTags;
import com.wildsense.config.WildsenseConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.animal.Animal;

public final class WildsenseAnimalRules {
    private WildsenseAnimalRules() {
    }

    private static final java.util.Set<net.minecraft.world.entity.EntityType<?>> RUNTIME_DISABLED =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    public static boolean isDisabled(Animal animal) {
        if (RUNTIME_DISABLED.contains(animal.getType())) return true;
        return BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(animal.getType()).is(WildsenseTags.DISABLED);
    }

    public static boolean toggleRuntimeDisable(net.minecraft.world.entity.EntityType<?> type) {
        if (RUNTIME_DISABLED.remove(type)) return false;
        RUNTIME_DISABLED.add(type);
        return true;
    }

    public static boolean skipMovementGoals(Animal animal) {
        if (isDisabled(animal)) return true;
        if (WildsenseConfig.respectLeashedAnimals && animal.isLeashed()) return true;
        if (WildsenseConfig.respectMountedAnimals && (animal.isPassenger() || animal.isVehicle())) return true;
        if (WildsenseConfig.respectNamedAnimals && animal.hasCustomName()) return true;
        return WildsenseConfig.respectBreedingAnimals && animal.isInLove();
    }
}
