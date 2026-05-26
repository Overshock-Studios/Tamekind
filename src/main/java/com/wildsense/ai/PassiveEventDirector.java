package com.wildsense.ai;

import com.wildsense.config.WildsenseConfig;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class PassiveEventDirector {
    private PassiveEventDirector() {
    }

    public static void register() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register(PassiveEventDirector::afterDamage);
    }

    private static void afterDamage(LivingEntity entity, DamageSource source,
                                    float baseDamageTaken, float damageTaken, boolean blocked) {
        if (!WildsenseConfig.enabled || !(entity instanceof Animal animal)) return;
        if (damageTaken <= 0.0f && !source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) return;
        Vec3 danger = dangerPosition(animal, source);
        DangerBroadcaster.rememberAndSpread(animal, danger);
        Entity attacker = source.getEntity();
        if (attacker instanceof Player player && WildsenseConfig.trustEnabled) {
            AnimalMemoryStore.get(animal).removeTrust(player.getUUID(), WildsenseConfig.trustLossPerHit);
        }
        if (animal.isBaby() && WildsenseConfig.parentGuardEnabled
                && animal.level() instanceof ServerLevel level) {
            long until = level.getGameTime() + WildsenseConfig.parentGuardTicks;
            AABB box = animal.getBoundingBox().inflate(WildsenseConfig.parentGuardRadius);
            for (Animal adult : level.getEntitiesOfClass(Animal.class, box,
                    other -> other.isAlive() && !other.isBaby() && other.getType() == animal.getType())) {
                AnimalMemoryStore.get(adult).markGuarding(until);
            }
        }
    }

    private static Vec3 dangerPosition(Animal animal, DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker == null) attacker = source.getDirectEntity();
        if (attacker != null) return attacker.position();
        Vec3 sourcePos = source.getSourcePosition();
        return sourcePos != null ? sourcePos : animal.position();
    }

}
