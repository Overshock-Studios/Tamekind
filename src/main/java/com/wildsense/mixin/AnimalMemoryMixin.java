package com.wildsense.mixin;

import com.wildsense.ai.AnimalMemoryStore;
import com.wildsense.config.WildsenseConfig;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Animal.class)
public abstract class AnimalMemoryMixin {
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void wildsense$saveMemory(ValueOutput output, CallbackInfo ci) {
        if (!WildsenseConfig.enabled) return;
        Animal animal = (Animal) (Object) this;
        AnimalMemoryStore.get(animal).save(output.child("WildsenseMemory"));
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void wildsense$loadMemory(ValueInput input, CallbackInfo ci) {
        if (!WildsenseConfig.enabled) return;
        Animal animal = (Animal) (Object) this;
        AnimalMemoryStore.get(animal).load(
                input.childOrEmpty("WildsenseMemory"),
                animal.level().getGameTime());
    }
}
