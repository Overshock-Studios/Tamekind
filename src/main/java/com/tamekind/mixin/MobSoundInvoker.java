package com.tamekind.mixin;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Reaches {@code Mob.getAmbientSound()}, which is protected.
 *
 * <p>Needed so an alarmed animal can raise its own voice rather than borrowing a sound
 * that means something else. {@code playAmbientSound()} is public but plays at the normal
 * pitch and resets the ambient timer, and the whole point of an alarm cue is that it is
 * pitched differently from idle chatter.
 */
@Mixin(Mob.class)
public interface MobSoundInvoker {

    @Invoker("getAmbientSound")
    SoundEvent tamekind$getAmbientSound();
}
