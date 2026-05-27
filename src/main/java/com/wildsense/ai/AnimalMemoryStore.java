package com.tamekind.ai;

import net.minecraft.world.entity.animal.Animal;

import java.util.Map;
import java.util.WeakHashMap;

public final class AnimalMemoryStore {
    private static final Map<Animal, AnimalMemory> MEMORIES = new WeakHashMap<>();

    private AnimalMemoryStore() {
    }

    public static AnimalMemory get(Animal animal) {
        return MEMORIES.computeIfAbsent(animal, ignored -> new AnimalMemory());
    }
}
