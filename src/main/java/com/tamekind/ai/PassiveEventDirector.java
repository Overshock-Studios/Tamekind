package com.tamekind.ai;

import com.tamekind.config.TamekindConfig;
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
        ServerLivingEntityEvents.AFTER_DEATH.register(PassiveEventDirector::afterDeath);
    }

    /**
     * Herd-mates that watch one of their own die remember it. Enough deaths inside the
     * memory window and the survivors stop fleeing: the herd has decided the field is
     * theirs. They never retaliate with damage; only panic is suppressed.
     */
    private static void afterDeath(LivingEntity entity, DamageSource source) {
        if (!TamekindConfig.enabled || !TamekindConfig.territorialRetaliationEnabled) return;
        if (!(entity instanceof Animal dead)) return;
        if (!(dead.level() instanceof ServerLevel level)) return;
        // Natural causes do not radicalise a herd; a killer does.
        if (source.getEntity() == null) return;

        long now = level.getGameTime();
        AABB box = dead.getBoundingBox().inflate(TamekindConfig.cullWitnessRadius);
        for (Animal witness : level.getEntitiesOfClass(Animal.class, box, other ->
                other.isAlive() && other != dead && !other.isBaby()
                        && other.getType() == dead.getType())) {
            AnimalMemory memory = AnimalMemoryStore.get(witness);
            int seen = memory.recordCull(now, TamekindConfig.cullMemoryTicks);
            if (seen >= TamekindConfig.cullVengeanceThreshold) {
                memory.markVengeful(now + TamekindConfig.cullVengeanceTicks);
            }
        }
    }

    private static void afterDamage(LivingEntity entity, DamageSource source,
                                    float baseDamageTaken, float damageTaken, boolean blocked) {
        if (!TamekindConfig.enabled || !(entity instanceof Animal animal)) return;
        if (damageTaken <= 0.0f && !source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) return;
        Entity attacker = source.getEntity();
        boolean forgive = false;
        if (attacker instanceof Player player && TamekindConfig.trustEnabled) {
            double trust = AnimalMemoryStore.get(animal).trustScore(player.getUUID(), animal.level().getGameTime());
            if (trust >= TamekindConfig.trustHitForgivenessThreshold) forgive = true;
            AnimalMemoryStore.get(animal).removeTrust(player.getUUID(), TamekindConfig.trustLossPerHit);
        }
        if (!forgive) {
            Vec3 danger = dangerPosition(animal, source);
            DangerBroadcaster.rememberAndSpread(animal, danger);
        }
        if (animal.isBaby() && TamekindConfig.parentGuardEnabled
                && animal.level() instanceof ServerLevel level) {
            long until = level.getGameTime() + TamekindConfig.parentGuardTicks;
            AABB box = animal.getBoundingBox().inflate(TamekindConfig.parentGuardRadius);
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
