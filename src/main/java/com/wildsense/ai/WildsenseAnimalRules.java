package com.wildsense.ai;

import com.wildsense.config.WildsenseConfig;
import net.minecraft.world.entity.animal.Animal;

public final class WildsenseAnimalRules {
    private WildsenseAnimalRules() {
    }

    public static boolean skipMovementGoals(Animal animal) {
        if (WildsenseConfig.respectLeashedAnimals && animal.isLeashed()) return true;
        if (WildsenseConfig.respectMountedAnimals && (animal.isPassenger() || animal.isVehicle())) return true;
        return WildsenseConfig.respectBreedingAnimals && animal.isInLove();
    }
}
