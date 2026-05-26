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
                        .then(Commands.literal("leader")
                                .executes(WildsenseCommand::reportLeader))
                        .then(Commands.literal("list")
                                .executes(WildsenseCommand::listNearby))
                        .then(Commands.literal("reload")
                                .executes(WildsenseCommand::reloadConfig))
                        .then(Commands.literal("home")
                                .then(Commands.literal("clear")
                                        .executes(WildsenseCommand::clearHome)))
                        .then(Commands.literal("forget")
                                .executes(WildsenseCommand::forgetNearest))
                        .then(Commands.literal("trust")
                                .executes(WildsenseCommand::reportTrust))
                        .then(Commands.literal("disable")
                                .then(Commands.argument("type", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                                        .executes(WildsenseCommand::toggleDisable)))
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
        BlockPos home = memory.home();
        boolean guarding = memory.isGuarding(now);
        source.sendSuccess(() -> Component.literal(String.format(
                "  home=%s guarding=%s",
                home == null ? "none" : home.getX() + " " + home.getY() + " " + home.getZ(),
                guarding)), false);
        return 1;
    }

    private static int toggleDisable(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String id = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "type").trim();
        net.minecraft.resources.Identifier identifier;
        try {
            identifier = net.minecraft.resources.Identifier.parse(id.contains(":") ? id : "minecraft:" + id);
        } catch (Exception e) {
            source.sendFailure(Component.literal("[Wildsense] Invalid entity id: " + id));
            return 0;
        }
        var type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(identifier);
        if (type == null) {
            source.sendFailure(Component.literal("[Wildsense] Unknown entity type: " + identifier));
            return 0;
        }
        boolean nowDisabled = com.wildsense.ai.WildsenseAnimalRules.toggleRuntimeDisable(type);
        source.sendSuccess(() -> Component.literal("[Wildsense] " + identifier + (nowDisabled ? " disabled" : " enabled")), true);
        return 1;
    }

    private static int reportTrust(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("[Wildsense] Trust command requires a player."));
            return 0;
        }
        ServerLevel level = source.getLevel();
        AABB box = AABB.ofSize(source.getPosition(), ANIMAL_SCAN_SIZE, ANIMAL_SCAN_SIZE, ANIMAL_SCAN_SIZE);
        Animal animal = level.getEntitiesOfClass(Animal.class, box).stream()
                .min(Comparator.comparingDouble(c -> c.distanceToSqr(source.getPosition())))
                .orElse(null);
        if (animal == null) {
            source.sendFailure(Component.literal("[Wildsense] No animal nearby."));
            return 0;
        }
        long now = level.getGameTime();
        double trust = AnimalMemoryStore.get(animal).trustScore(player.getUUID(), now);
        source.sendSuccess(() -> Component.literal(String.format(
                "[Wildsense] %s#%d trusts %s = %.2f",
                animal.getType().toShortString(), animal.getId(), player.getName().getString(), trust)), false);
        return 1;
    }

    private static int forgetNearest(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        AABB box = AABB.ofSize(source.getPosition(), ANIMAL_SCAN_SIZE, ANIMAL_SCAN_SIZE, ANIMAL_SCAN_SIZE);
        Animal animal = level.getEntitiesOfClass(Animal.class, box).stream()
                .min(Comparator.comparingDouble(c -> c.distanceToSqr(source.getPosition())))
                .orElse(null);
        if (animal == null) {
            source.sendFailure(Component.literal("[Wildsense] No animal nearby."));
            return 0;
        }
        AnimalMemoryStore.get(animal).forget();
        source.sendSuccess(() -> Component.literal("[Wildsense] Forgot memory for " + animal.getType().toShortString() + "#" + animal.getId()), false);
        return 1;
    }

    private static int reportLeader(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        AABB box = AABB.ofSize(source.getPosition(), ANIMAL_SCAN_SIZE, ANIMAL_SCAN_SIZE, ANIMAL_SCAN_SIZE);
        Animal animal = level.getEntitiesOfClass(Animal.class, box).stream()
                .min(Comparator.comparingDouble(c -> c.distanceToSqr(source.getPosition())))
                .orElse(null);
        if (animal == null) {
            source.sendFailure(Component.literal("[Wildsense] No animal nearby."));
            return 0;
        }
        Animal leader = HerdCoordinator.leaderFor(animal);
        long now = level.getGameTime();
        BlockPos shared = leader == null ? null : AnimalMemoryStore.get(leader).sharedShelter(now);
        source.sendSuccess(() -> Component.literal(String.format(
                "[Wildsense] %s#%d leader=%s herdSize=%d sharedShelter=%s",
                animal.getType().toShortString(),
                animal.getId(),
                formatAnimal(leader),
                HerdCoordinator.herdSize(animal),
                shared == null ? "none" : shared.getX() + " " + shared.getY() + " " + shared.getZ())), false);
        return 1;
    }

    private static int listNearby(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        AABB box = AABB.ofSize(source.getPosition(), 64.0, 64.0, 64.0);
        int total = 0, full = 0, simple = 0, sleep = 0, herdable = 0;
        for (Animal a : level.getEntitiesOfClass(Animal.class, box)) {
            total++;
            if (HerdCoordinator.isHerdable(a)) herdable++;
            switch (AiLod.computeFresh(a)) {
                case FULL -> full++;
                case SIMPLE -> simple++;
                case SLEEP -> sleep++;
            }
        }
        final int t = total, f = full, s = simple, sl = sleep, h = herdable;
        source.sendSuccess(() -> Component.literal(String.format(
                "[Wildsense] within 64: total=%d herdable=%d  LOD full=%d simple=%d sleep=%d",
                t, h, f, s, sl)), false);
        return 1;
    }

    private static int clearHome(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
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
        AnimalMemoryStore.get(animal).setHome(null);
        source.sendSuccess(() -> Component.literal("[Wildsense] Cleared home for " + animal.getType().toShortString() + "#" + animal.getId()), false);
        return 1;
    }

    private static int reloadConfig(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        try {
            WildsenseConfig.load(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("wildsense.properties"));
            source.sendSuccess(() -> Component.literal("[Wildsense] Config reloaded."), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("[Wildsense] Reload failed: " + e.getMessage()));
            return 0;
        }
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
                "  respectLeashed=%s respectMounted=%s respectBreeding=%s respectNamed=%s",
                WildsenseConfig.respectLeashedAnimals,
                WildsenseConfig.respectMountedAnimals,
                WildsenseConfig.respectBreedingAnimals,
                WildsenseConfig.respectNamedAnimals)), false);
        source.sendSuccess(() -> Component.literal(String.format(
                "  graze: radius=%d interval=%d duration=%d  drink: radius=%d interval=%d duration=%d",
                WildsenseConfig.grazeSearchRadius,
                WildsenseConfig.grazeMinIntervalTicks,
                WildsenseConfig.grazeDurationTicks,
                WildsenseConfig.drinkSearchRadius,
                WildsenseConfig.drinkMinIntervalTicks,
                WildsenseConfig.drinkDurationTicks)), false);
        source.sendSuccess(() -> Component.literal(String.format(
                "  parentGuard=%s ticks=%d radius=%d  breedingSoftLimit=%d",
                WildsenseConfig.parentGuardEnabled,
                WildsenseConfig.parentGuardTicks,
                WildsenseConfig.parentGuardRadius,
                WildsenseConfig.breedingCrowdSoftLimit)), false);
        source.sendSuccess(() -> Component.literal(String.format(
                "  homeReturn=%s min=%d max=%d interval=%d speed=%.2f",
                WildsenseConfig.homeReturnEnabled,
                WildsenseConfig.homeReturnMinDistance,
                WildsenseConfig.homeReturnMaxDistance,
                WildsenseConfig.homeReturnIntervalTicks,
                WildsenseConfig.homeReturnSpeed)), false);
        source.sendSuccess(() -> Component.literal(String.format(
                "  alertFreeze: min=%d random=%d driftSpeed=%.2f",
                WildsenseConfig.alertFreezeMinTicks,
                WildsenseConfig.alertFreezeRandomTicks,
                WildsenseConfig.alertDriftSpeed)), false);
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
