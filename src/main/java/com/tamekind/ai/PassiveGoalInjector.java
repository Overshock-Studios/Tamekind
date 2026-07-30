package com.tamekind.ai;

import com.tamekind.ai.goal.AlertFreezeGoal;
import com.tamekind.ai.goal.BabyAnchorGoal;
import com.tamekind.ai.goal.ConditionGoal;
import com.tamekind.ai.goal.DrinkGoal;
import com.tamekind.ai.goal.HerdTrailGoal;
import com.tamekind.ai.goal.GrazeRestGoal;
import com.tamekind.ai.goal.HomeReturnGoal;
import com.tamekind.ai.goal.FollowTrustedPlayerGoal;
import com.tamekind.ai.goal.LostBabyGoal;
import com.tamekind.ai.goal.MotherBondGoal;
import com.tamekind.ai.goal.MatingDisplayGoal;
import com.tamekind.ai.goal.PetIdleBondGoal;
import com.tamekind.ai.goal.AgeScaleGoal;
import com.tamekind.ai.goal.AlphaPrideGoal;
import com.tamekind.ai.goal.MountObedienceGoal;
import com.tamekind.ai.goal.PetDangerRelayGoal;
import com.tamekind.ai.goal.SentinelWatchGoal;
import com.tamekind.ai.goal.WallowGoal;
import com.tamekind.ai.goal.HabitatShelterGoal;
import com.tamekind.ai.goal.HerdFollowGoal;
import com.tamekind.ai.goal.PanicGoal;
import com.tamekind.ai.goal.TamekindGoal;
import com.tamekind.TamekindMod;
import com.tamekind.config.TamekindConfig;
import com.tamekind.mixin.MobGoalSelectorAccessor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.phys.AABB;

public final class PassiveGoalInjector {
    private static final java.util.concurrent.atomic.AtomicBoolean LOGGED_GOAL_TABLE =
            new java.util.concurrent.atomic.AtomicBoolean();

    private PassiveGoalInjector() {
    }

    /** Lets {@code /tamekind reload} re-log the goal table after a config change. */
    public static void resetGoalTableLog() {
        LOGGED_GOAL_TABLE.set(false);
    }

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof Animal animal) {
                inject(animal);
                SizeVariance.apply(animal);
            } else if (entity instanceof net.minecraft.world.entity.LightningBolt bolt) {
                broadcastLightning(bolt, level);
            }
        });

        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (!TamekindConfig.enabled) return InteractionResult.PASS;
            if (level.isClientSide()) return InteractionResult.PASS;
            if (entity instanceof Animal animal && animal.isFood(player.getItemInHand(hand))) {
                if (shouldBlockCrowdedBreeding(animal)) {
                    if (TamekindConfig.breedingCrowdMessageEnabled) {
                        player.sendSystemMessage(Component.literal("This pen is too crowded for more breeding."));
                    }
                    return InteractionResult.FAIL;
                }
                if (!TamekindConfig.trustEnabled) return InteractionResult.PASS;
                long durationTicks = TamekindConfig.trustTicks;
                if (animal instanceof net.minecraft.world.entity.TamableAnimal || isMountType(animal)) {
                    durationTicks = (long) (durationTicks * TamekindConfig.mountFoodTrustMultiplier);
                }
                long until = level.getGameTime() + durationTicks;
                // Approach etiquette: crouching reads as calm, so a careful player bonds
                // faster. Mirrors Warband's rule that crouching *reduces* how threatening
                // a player is, rather than adding a separate taming mechanic.
                double gain = TamekindConfig.trustPerFeeding
                        * AnimalTemperament.forAnimal(animal).trustGainMultiplier();
                if (player.isShiftKeyDown()) gain *= TamekindConfig.crouchFeedTrustMultiplier;
                AnimalMemoryStore.get(animal).addTrust(player.getUUID(), gain, until);
                double existingTrust = AnimalMemoryStore.get(animal).trustScore(player.getUUID(), level.getGameTime());
                if (existingTrust >= TamekindConfig.calmerBreedingTrustThreshold
                        && !animal.isInLove() && !animal.isBaby()) {
                    animal.setInLoveTime(TamekindConfig.calmerBreedingLoveTicks);
                }
                for (Animal herdMate : HerdCoordinator.nearbyHerd(animal)) {
                    AnimalMemoryStore.get(herdMate).addTrust(
                            player.getUUID(),
                            gain * TamekindConfig.herdTrustShareMultiplier,
                            until);
                }
            }
            return InteractionResult.PASS;
        });
    }

    private static boolean isMountType(Animal animal) {
        var id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(animal.getType()).getPath();
        return id.contains("horse") || id.equals("donkey") || id.equals("mule")
                || id.equals("llama") || id.equals("trader_llama") || id.equals("camel")
                || id.equals("pig") || id.equals("strider");
    }

    private static void broadcastLightning(net.minecraft.world.entity.LightningBolt bolt,
                                           net.minecraft.server.level.ServerLevel level) {
        if (!TamekindConfig.enabled || !TamekindConfig.lightningPanicEnabled) return;
        double radius = TamekindConfig.lightningPanicRadius;
        AABB box = bolt.getBoundingBox().inflate(radius);
        net.minecraft.world.phys.Vec3 source = bolt.position();
        for (Animal nearby : level.getEntitiesOfClass(Animal.class, box, a -> a.isAlive())) {
            DangerBroadcaster.rememberAndSpread(nearby, source);
        }
    }

    private static boolean shouldBlockCrowdedBreeding(Animal animal) {
        if (!TamekindConfig.breedingCrowdControlEnabled || animal.isBaby()) return false;
        // Only refuse a feed that would actually start breeding. For a tame pet or a
        // wounded animal the same item is a heal, and crowd control must never block
        // healing your wolf just because the pen is busy.
        if (animal instanceof net.minecraft.world.entity.TamableAnimal tame && tame.isTame()) return false;
        if (animal.getHealth() < animal.getMaxHealth()) return false;
        if (animal.getAge() != 0 || !animal.canFallInLove()) return false;
        int hard = TamekindConfig.breedingCrowdHardLimit;
        if (hard <= 0) return false;
        double radius = TamekindConfig.breedingCrowdRadius;
        AABB box = animal.getBoundingBox().inflate(radius);
        int sameType = animal.level().getEntitiesOfClass(Animal.class, box,
                other -> other.isAlive() && other.getType() == animal.getType()).size();
        if (sameType >= hard) return true;
        int soft = Math.min(TamekindConfig.breedingCrowdSoftLimit, hard);
        if (soft <= 0 || sameType < soft) return false;
        double chance = (double) (sameType - soft) / Math.max(1, hard - soft);
        return animal.getRandom().nextDouble() < chance;
    }

    private static void inject(Animal animal) {
        if (!TamekindConfig.enabled || hasTamekindGoal(animal)) return;
        MobGoalSelectorAccessor accessor = (MobGoalSelectorAccessor) animal;
        var selector = accessor.tamekind$goalSelector();
        // Priorities and the reasoning behind each are in GoalPriorities. They are
        // claims against a table vanilla already owns, not free choices.
        selector.addGoal(GoalPriorities.BABY_ANCHOR, new BabyAnchorGoal(animal));
        selector.addGoal(GoalPriorities.PANIC, new PanicGoal(animal));
        selector.addGoal(GoalPriorities.ALERT_FREEZE, new AlertFreezeGoal(animal));
        selector.addGoal(GoalPriorities.LOST_BABY, new LostBabyGoal(animal));
        selector.addGoal(GoalPriorities.HERD_FOLLOW, new HerdFollowGoal(animal));
        selector.addGoal(GoalPriorities.SHELTER, new HabitatShelterGoal(animal));
        // Above graze/drink on purpose: the alpha on watch must not put its head down.
        selector.addGoal(GoalPriorities.SENTINEL, new SentinelWatchGoal(animal));
        selector.addGoal(GoalPriorities.GRAZE, new GrazeRestGoal(animal));
        selector.addGoal(GoalPriorities.DRINK, new DrinkGoal(animal));
        selector.addGoal(GoalPriorities.HOME_RETURN, new HomeReturnGoal(animal));
        selector.addGoal(GoalPriorities.MOTHER_BOND, new MotherBondGoal(animal));
        selector.addGoal(GoalPriorities.FOLLOW_TRUSTED, new FollowTrustedPlayerGoal(animal));
        selector.addGoal(GoalPriorities.MATING_DISPLAY, new MatingDisplayGoal(animal));
        selector.addGoal(GoalPriorities.WALLOW, new WallowGoal(animal));
        selector.addGoal(GoalPriorities.PET_DANGER_RELAY, new PetDangerRelayGoal(animal));
        selector.addGoal(GoalPriorities.PET_IDLE_BOND, new PetIdleBondGoal(animal));
        selector.addGoal(GoalPriorities.MOUNT_OBEDIENCE, new MountObedienceGoal(animal));
        selector.addGoal(GoalPriorities.ALPHA_PRIDE, new AlphaPrideGoal(animal));
        selector.addGoal(GoalPriorities.AGE_SCALE, new AgeScaleGoal(animal));
        selector.addGoal(GoalPriorities.HERD_TRAIL, new HerdTrailGoal(animal));
        selector.addGoal(GoalPriorities.CONDITION, new ConditionGoal(animal));

        logGoalTableOnce(animal);
    }

    /**
     * Logs the live goal table and any same-priority flag collisions for the first
     * animal injected after a config load. One animal is enough — the table is identical
     * per species — and doing it once keeps a busy farm from flooding the log.
     */
    private static void logGoalTableOnce(Animal animal) {
        if (!TamekindConfig.debugLogs || !LOGGED_GOAL_TABLE.compareAndSet(false, true)) return;
        TamekindMod.LOGGER.info("[Tamekind] goal table for {} (first injected animal):",
                animal.getType().toShortString());
        for (GoalDiagnostics.Entry e : GoalDiagnostics.inspect(animal)) {
            TamekindMod.LOGGER.info("  {} {} {} [{}]",
                    String.format("%3d", e.priority()),
                    e.tamekind() ? "tamekind" : "        ",
                    e.name(), e.flags());
        }
        var conflicts = GoalDiagnostics.conflicts(animal);
        if (conflicts.isEmpty()) {
            TamekindMod.LOGGER.info("  no same-priority flag collisions");
            return;
        }
        for (GoalDiagnostics.Conflict c : conflicts) {
            TamekindMod.LOGGER.warn("  COLLISION at priority {}: {} vs {} share {} "
                            + "(the goal selector cannot break this tie)",
                    c.priority(), c.first(), c.second(), c.sharedFlags());
        }
    }

    private static boolean hasTamekindGoal(Animal animal) {
        for (WrappedGoal goal : ((MobGoalSelectorAccessor) animal).tamekind$goalSelector().getAvailableGoals()) {
            if (goal.getGoal() instanceof TamekindGoal) return true;
        }
        return false;
    }
}
