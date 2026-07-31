package com.tamekind.ai;

import com.tamekind.TamekindMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/**
 * Tamekind's Fabric data attachments.
 *
 * <p>Replaces a static {@code WeakHashMap} plus a mixin into
 * {@code Animal.addAdditionalSaveData}. That worked, but it was the wrong tool twice
 * over: the map was mutated with {@code computeIfAbsent} from entity deserialization,
 * which is not guaranteed to be the server thread, and a plain {@code HashMap} corrupts
 * or spins under concurrent {@code computeIfAbsent}. The attachment API owns the
 * lifetime, the thread safety and the serialization, and it is what Warband already
 * uses for exactly this.
 *
 * <p>Deliberately not {@code copyOnDeath}: animals do not respawn, so the memory should
 * die with the animal.
 */
public final class TamekindAttachments {

    /** Per-animal herd, trust, danger, descent and condition state. Survives save/load. */
    public static final AttachmentType<AnimalMemory> MEMORY = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(TamekindMod.MOD_ID, "memory"),
            builder -> builder.initializer(AnimalMemory::new).persistent(AnimalMemory.CODEC));

    private TamekindAttachments() {
    }

    /** Touch this class so its attachments register. Called from {@code onInitialize}. */
    public static void init() {
        // Intentionally empty, referencing the class triggers static init above.
    }
}
