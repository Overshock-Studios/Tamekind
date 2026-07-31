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

    /**
     * Adds trust and fires {@code TamekindEvents.TRUST_CHANGED}.
     *
     * <p>Every trust change goes through here rather than through
     * {@link AnimalMemory#addTrust}, because {@code AnimalMemory} has no reference to its
     * animal and so cannot fire the event itself. Centralising it means a new trust path
     * cannot silently skip the event.
     */
    public static void addTrust(Animal animal, java.util.UUID player, double amount, long until) {
        AnimalMemory memory = get(animal);
        memory.addTrust(player, amount, until);
        notifyTrust(animal, player, memory);
    }

    /** Removes trust and fires {@code TamekindEvents.TRUST_CHANGED}. */
    public static void removeTrust(Animal animal, java.util.UUID player, double amount) {
        AnimalMemory memory = get(animal);
        memory.removeTrust(player, amount);
        notifyTrust(animal, player, memory);
    }

    private static void notifyTrust(Animal animal, java.util.UUID player, AnimalMemory memory) {
        com.tamekind.api.TamekindEvents.TRUST_CHANGED.invoker().onTrustChanged(
                animal, player, memory.trustScore(player, animal.level().getGameTime()));
    }

    /** True when this animal has memory yet, without creating it. Used by diagnostics. */
    public static boolean has(Animal animal) {
        return animal.hasAttached(TamekindAttachments.MEMORY);
    }
}
