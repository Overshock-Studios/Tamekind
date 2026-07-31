package com.tamekind.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Server-side events other mods can listen to.
 *
 * <p>Only three, and each one exists because there is a real point in the code where it
 * can honestly fire. It would be easy to invent an "alpha changed" event, but election is
 * computed on demand rather than stored, so there is no moment to hang it on. The alpha
 * event below fires from the size-bonus handover instead, which is a real transition.
 *
 * <p>Listeners run on the server thread, inside the behaviour that fired them. Keep them
 * cheap and do not mutate the herd from inside one.
 */
public final class TamekindEvents {

    private TamekindEvents() {
    }

    /** An animal became frightened, either first-hand or by a herd-mate's warning. */
    public interface Alarmed {
        void onAlarmed(Animal animal, Vec3 dangerPos);
    }

    /** An animal's trust in a player changed. */
    public interface TrustChanged {
        void onTrustChanged(Animal animal, UUID player, double newTrust);
    }

    /** An animal took or lost the alpha role in its herd. */
    public interface AlphaChanged {
        void onAlphaChanged(Animal animal, boolean isAlpha);
    }

    /**
     * Fired when an animal records a danger memory, including when it learns of one
     * second-hand from a herd-mate or the herd's lookout. Fires once per animal per
     * broadcast, so a startled herd of ten produces ten calls.
     */
    public static final Event<Alarmed> ALARMED =
            EventFactory.createArrayBacked(Alarmed.class, listeners -> (animal, dangerPos) -> {
                for (Alarmed listener : listeners) listener.onAlarmed(animal, dangerPos);
            });

    /**
     * Fired when trust changes, whether gained by feeding or idle bonding or lost to a
     * hit. The value passed is the score after the change.
     */
    public static final Event<TrustChanged> TRUST_CHANGED =
            EventFactory.createArrayBacked(TrustChanged.class, listeners -> (animal, player, trust) -> {
                for (TrustChanged listener : listeners) listener.onTrustChanged(animal, player, trust);
            });

    /**
     * Fired on a real alpha handover, not on every frame of the election.
     *
     * <p>Election is recomputed constantly as a herd drifts, so the raw answer flickers.
     * This fires from the point where the alpha's size bonus is actually applied or
     * withdrawn, which is hysteresis-guarded and therefore a transition worth reacting to.
     */
    public static final Event<AlphaChanged> ALPHA_CHANGED =
            EventFactory.createArrayBacked(AlphaChanged.class, listeners -> (animal, isAlpha) -> {
                for (AlphaChanged listener : listeners) listener.onAlphaChanged(animal, isAlpha);
            });
}
