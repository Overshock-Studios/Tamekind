package com.tamekind.mixin;

import com.tamekind.ai.AnimalMemoryStore;
import com.tamekind.ai.Disposition;
import com.tamekind.ai.SizeVariance;
import com.tamekind.config.TamekindConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks vanilla breeding for the two features that need to sit exactly there:
 * heritable size, and declining to mate when worn down.
 */
@Mixin(Animal.class)
public abstract class AnimalBreedingMixin {

    /**
     * Stamps the newborn with the average of its parents' sizes.
     *
     * <p>Injected at TAIL so the child already exists and has its attributes, and so
     * vanilla has finished its own setup before the base scale is rewritten.
     */
    @Inject(method = "finalizeSpawnChildFromBreeding", at = @At("TAIL"))
    private void tamekind$inheritSize(ServerLevel level, Animal partner, AgeableMob child, CallbackInfo ci) {
        if (!TamekindConfig.enabled
                || !TamekindConfig.sizeVarianceEnabled
                || !TamekindConfig.heritableSizeEnabled) return;
        if (!(child instanceof Animal calf)) return;
        Animal self = (Animal) (Object) this;
        double scale = SizeVariance.inheritFrom(self, partner, calf.getRandom().nextDouble());
        AnimalMemoryStore.get(calf).setInheritedScale(scale);
        SizeVariance.apply(calf);
    }

    /**
     * A worn-down animal declines to mate. Cancels the vanilla check rather than
     * consuming the food, so nothing is lost: the player is simply told "not yet" by
     * the animal not entering love mode.
     *
     * <p>Only active when {@code conditionEnabled} is on, which it is not by default.
     */
    @Inject(method = "canMate", at = @At("HEAD"), cancellable = true)
    private void tamekind$conditionGate(Animal other, CallbackInfoReturnable<Boolean> cir) {
        if (!TamekindConfig.enabled || !TamekindConfig.conditionEnabled) return;
        Animal self = (Animal) (Object) this;
        if (Disposition.tooWornToBreed(self) || Disposition.tooWornToBreed(other)) {
            cir.setReturnValue(false);
        }
    }
}
