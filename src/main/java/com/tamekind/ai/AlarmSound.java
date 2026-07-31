package com.tamekind.ai;

import com.tamekind.mixin.MobSoundInvoker;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.animal.Animal;

/**
 * Alarm cues for startled animals.
 *
 * <p>These used to play {@code SoundEvents.GENERIC_HURT}, which is the damage sound.
 * Walking up to a herd made every animal sound like it was being hit, and the obvious
 * reading of that is that the mod is hurting your livestock. It was reported as exactly
 * that. Feedback that means "I am alarmed" must not be the sound that means "I am
 * injured".
 *
 * <p>An animal now raises its own voice instead: a cow moos, a sheep bleats, a chicken
 * clucks, pitched up so it reads as agitated rather than idle. That can never be
 * mistaken for damage, and it needs no new sound assets, which matters for a mod that
 * ships no content.
 */
public final class AlarmSound {

    private AlarmSound() {
    }

    /**
     * Plays the animal's own ambient sound, pitched up by {@code pitchBump}.
     *
     * <p>Silent when the species has no ambient sound. Silence is the right fallback:
     * substituting some other sound is how the hurt-sound bug happened.
     */
    public static void alarm(Animal animal, float volume, float pitchBump) {
        SoundEvent sound = ((MobSoundInvoker) animal).tamekind$getAmbientSound();
        if (sound == null) return;
        animal.playSound(sound, volume, animal.getVoicePitch() * pitchBump);
    }
}
