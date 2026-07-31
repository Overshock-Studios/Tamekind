package com.tamekind.ai;

import net.minecraft.world.entity.animal.Animal;

/**
 * Access to an animal's persistent Tamekind memory.
 *
 * <p>A thin front on {@link TamekindAttachments#MEMORY}, kept because every goal already
 * calls {@code AnimalMemoryStore.get(animal)} and the indirection costs nothing.
 *
 * <p>{@code getAttachedOrCreate} creates the memory on first touch, so an animal that
 * has never been near a player carries no attachment and writes nothing to the save.
 * That matters on a world full of untouched livestock.
 */
public final class AnimalMemoryStore {

    private AnimalMemoryStore() {
    }

    public static AnimalMemory get(Animal animal) {
        return animal.getAttachedOrCreate(TamekindAttachments.MEMORY);
    }

    /** True when this animal has memory yet, without creating it. Used by diagnostics. */
    public static boolean has(Animal animal) {
        return animal.hasAttached(TamekindAttachments.MEMORY);
    }
}
