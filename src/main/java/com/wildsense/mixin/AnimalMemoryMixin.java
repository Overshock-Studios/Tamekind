package com.tamekind.mixin;

import com.tamekind.ai.AnimalMemoryStore;
import com.tamekind.config.TamekindConfig;
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
    private void tamekind$saveMemory(ValueOutput output, CallbackInfo ci) {
        if (!TamekindConfig.enabled) return;
        Animal animal = (Animal) (Object) this;
        AnimalMemoryStore.get(animal).save(output.child("TamekindMemory"));
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void tamekind$loadMemory(ValueInput input, CallbackInfo ci) {
        if (!TamekindConfig.enabled) return;
        Animal animal = (Animal) (Object) this;
        AnimalMemoryStore.get(animal).load(
                input.childOrEmpty("TamekindMemory"),
                animal.level().getGameTime());
    }
}
