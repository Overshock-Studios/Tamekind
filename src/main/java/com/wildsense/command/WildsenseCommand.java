package com.wildsense.command;

import com.wildsense.ai.AiLod;
import com.wildsense.ai.AnimalMemory;
import com.wildsense.ai.AnimalMemoryStore;
import com.wildsense.ai.HerdCoordinator;
import com.wildsense.ai.goal.WildsenseGoal;
import com.wildsense.config.WildsenseConfig;
import com.wildsense.mixin.MobGoalSelectorAccessor;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

public final class WildsenseCommand {
    private static final double ANIMAL_SCAN_SIZE = 24.0;

    private WildsenseCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("wildsense")
                        .then(Commands.literal("animal")
                                .executes(WildsenseCommand::reportAnimal))
                        .then(Commands.literal("config")
                                .executes(WildsenseCommand::reportConfig))
                        .then(Commands.literal("profile")
                                .then(Commands.argument("name", com.mojang.brigadier.arguments.StringArgumentType.word())
                                        .suggests((c, b) -> {
                                            b.suggest("vanilla+");
                                            b.suggest("realism");
                                            b.suggest("simulation");
                                            return b.buildFuture();
                                        })
                                        .executes(WildsenseCommand::applyProfile)))));
    }

    private static int reportAnimal(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        AABB box = AABB.ofSize(source.getPosition(), ANIMAL_SCAN_SIZE, ANIMAL_SCAN_SIZE, ANIMAL_SCAN_SIZE);
        Animal animal = level.getEntitiesOfClass(Animal.class, box).stream()
                .min(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(source.getPosition())))
                .orElse(null);
        if (animal == null) {
            source.sendFailure(Component.literal("[Wildsense] No animal within " + (int) ANIMAL_SCAN_SIZE + " blocks."));
            return 0;
        }

        long now = level.getGameTime();
        AnimalMemory memory = AnimalMemoryStore.get(animal);
        memory.tick(now);
        Vec3 danger = memory.dangerPos(now);
        ServerPlayer player = source.getPlayer();
        double trust = player == null ? 0.0 : memory.trustScore(player.getUUID(), now);
        Animal leader = HerdCoordinator.leaderFor(animal);
        Animal adult = HerdCoordinator.nearestAdultForBaby(animal, WildsenseConfig.babyAnchorSearchRadius);

        source.sendSuccess(() -> Component.literal(String.format(
                "[Wildsense] %s id=%d lod=%s baby=%s herdable=%s herdSize=%d goals=%d",
                animal.getType().toShortString(),
                animal.getId(),
                AiLod.computeFresh(animal),
                animal.isBaby(),
                HerdCoordinator.isHerdable(animal),
                HerdCoordinator.herdSize(animal),
                wildsenseGoalCount(animal))), false);
        source.sendSuccess(() -> Component.literal(String.format(
                "  health=%.1f/%.1f pos=%d %d %d navigationDone=%s",
                animal.getHealth(),
                animal.getMaxHealth(),
                animal.getBlockX(),
                animal.getBlockY(),
                animal.getBlockZ(),
                animal.getNavigation().isDone())), false);
        source.sendSuccess(() -> Component.literal(String.format(
                "  danger=%s dangerTicks=%d trustForYou=%.2f activeTrustedPlayers=%d",
                formatVec(danger),
                memory.dangerTicksRemaining(now),
                trust,
                memory.activeTrustCount(now))), false);
        source.sendSuccess(() -> Component.literal(String.format(
                "  leader=%s nearestAdult=%s",
                formatAnimal(leader),
                formatAnimal(adult))), false);
        return 1;
    }

    private static int applyProfile(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String name = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "name");
        WildsenseConfig.Profile profile = WildsenseConfig.Profile.fromString(name);
        if (profile == null) {
            source.sendFailure(Component.literal("[Wildsense] Unknown profile '" + name + "'. Use vanilla+, realism, or simulation."));
            return 0;
        }
        WildsenseConfig.applyProfile(profile);
        source.sendSuccess(() -> Component.literal("[Wildsense] Applied profile: " + profile.name().toLowerCase(java.util.Locale.ROOT)), true);
        return 1;
    }

    private static int reportConfig(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        source.sendSuccess(() -> Component.literal(String.format(
                "[Wildsense] enabled=%s fullAiRange=%d simpleAiRange=%d aiLodCacheTicks=%d panicRadius=%d alertRadius=%d dangerSpreadCooldown=%d",
                WildsenseConfig.enabled,
                WildsenseConfig.fullAiRange,
                WildsenseConfig.simpleAiRange,
                WildsenseConfig.aiLodCacheTicks,
                WildsenseConfig.panicRadius,
                WildsenseConfig.alertRadius,
                WildsenseConfig.herdDangerSpreadCooldownTicks)), false);
        source.sendSuccess(() -> Component.literal(String.format(
                "  herd=%s habitat=%s dailyRhythm=%s trust=%s babyAnchoring=%s stampede=%s",
                WildsenseConfig.herdEnabled,
                WildsenseConfig.habitatEnabled,
                WildsenseConfig.dailyRhythmEnabled,
                WildsenseConfig.trustEnabled,
                WildsenseConfig.babyAnchoringEnabled,
                WildsenseConfig.stampedeEnabled)), false);
        source.sendSuccess(() -> Component.literal(String.format(
                "  breedingCrowdControl=%s crowdRadius=%d crowdHardLimit=%d",
                WildsenseConfig.breedingCrowdControlEnabled,
                WildsenseConfig.breedingCrowdRadius,
                WildsenseConfig.breedingCrowdHardLimit)), false);
        source.sendSuccess(() -> Component.literal(String.format(
                "  respectLeashed=%s respectMounted=%s respectBreeding=%s",
                WildsenseConfig.respectLeashedAnimals,
                WildsenseConfig.respectMountedAnimals,
                WildsenseConfig.respectBreedingAnimals)), false);
        return 1;
    }

    private static int wildsenseGoalCount(Animal animal) {
        int count = 0;
        for (WrappedGoal goal : ((MobGoalSelectorAccessor) animal).wildsense$goalSelector().getAvailableGoals()) {
            if (goal.getGoal() instanceof WildsenseGoal) {
                count++;
            }
        }
        return count;
    }

    private static String formatVec(Vec3 vec) {
        if (vec == null) return "none";
        BlockPos pos = BlockPos.containing(vec);
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private static String formatAnimal(Animal animal) {
        if (animal == null) return "none";
        return animal.getType().toShortString() + "#" + animal.getId();
    }
}
