package com.tamekind.command;

import com.tamekind.ai.AiLod;
import com.tamekind.ai.AnimalMemory;
import com.tamekind.ai.AnimalMemoryStore;
import com.tamekind.ai.HerdCoordinator;
import com.tamekind.ai.goal.TamekindGoal;
import com.tamekind.config.TamekindConfig;
import com.tamekind.mixin.MobGoalSelectorAccessor;
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

public final class TamekindCommand {
    private static final double ANIMAL_SCAN_SIZE = 24.0;

    private TamekindCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("tamekind")
                        .then(Commands.literal("animal")
                                .executes(TamekindCommand::reportAnimal))
                        .then(Commands.literal("config")
                                .executes(TamekindCommand::reportConfig))
                        .then(Commands.literal("leader")
                                .executes(TamekindCommand::reportLeader))
                        .then(Commands.literal("list")
                                .executes(TamekindCommand::listNearby))
                        .then(Commands.literal("reload")
                                .executes(TamekindCommand::reloadConfig))
                        .then(Commands.literal("home")
                                .then(Commands.literal("clear")
                                        .executes(TamekindCommand::clearHome))
                                .then(Commands.literal("set")
                                        .executes(TamekindCommand::setHome)))
                        .then(Commands.literal("forget")
                                .executes(TamekindCommand::forgetNearest))
                        .then(Commands.literal("dump")
                                .executes(TamekindCommand::dumpNearest))
                        .then(Commands.literal("goals")
                                .executes(TamekindCommand::reportGoals))
                        .then(Commands.literal("trust")
                                .executes(TamekindCommand::reportTrust)
                                .then(Commands.literal("map")
                                        .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                                                .executes(TamekindCommand::trustMap))))
                        .then(Commands.literal("season")
                                .executes(TamekindCommand::reportSeason))
                        .then(Commands.literal("disable")
                                .then(Commands.argument("type", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                                        .executes(TamekindCommand::toggleDisable)))
                        .then(Commands.literal("profile")
                                .executes(TamekindCommand::reportProfile)
                                .then(Commands.argument("name", com.mojang.brigadier.arguments.StringArgumentType.word())
                                        .suggests((c, b) -> {
                                            b.suggest("vanilla+");
                                            b.suggest("realism");
                                            b.suggest("simulation");
                                            return b.buildFuture();
                                        })
                                        .executes(TamekindCommand::applyProfile)))));
    }

    private static int reportAnimal(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        AABB box = AABB.ofSize(source.getPosition(), ANIMAL_SCAN_SIZE, ANIMAL_SCAN_SIZE, ANIMAL_SCAN_SIZE);
        Animal animal = level.getEntitiesOfClass(Animal.class, box).stream()
                .min(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(source.getPosition())))
                .orElse(null);
        if (animal == null) {
            source.sendFailure(Component.literal("[Tamekind] No animal within " + (int) ANIMAL_SCAN_SIZE + " blocks."));
            return 0;
        }

        long now = level.getGameTime();
        AnimalMemory memory = AnimalMemoryStore.get(animal);
        memory.tick(now);
        Vec3 danger = memory.dangerPos(now);
        ServerPlayer player = source.getPlayer();
        double trust = player == null ? 0.0 : memory.trustScore(player.getUUID(), now);
        Animal leader = HerdCoordinator.leaderFor(animal);
        Animal adult = HerdCoordinator.nearestAdultForBaby(animal, TamekindConfig.babyAnchorSearchRadius);

        source.sendSuccess(() -> Component.literal(String.format(
                "[Tamekind] %s id=%d lod=%s baby=%s herdable=%s herdSize=%d goals=%d",
                animal.getType().toShortString(),
                animal.getId(),
                AiLod.computeFresh(animal),
                animal.isBaby(),
                HerdCoordinator.isHerdable(animal),
                HerdCoordinator.herdSize(animal),
                tamekindGoalCount(animal))), false);
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
                "  leader=%s nearestAdult=%s isAlpha=%s temperament=%s",
                formatAnimal(leader),
                formatAnimal(adult),
                leader == animal,
                com.tamekind.ai.AnimalTemperament.forAnimal(animal).lowerName())), false);
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
            source.sendFailure(Component.literal("[Tamekind] Invalid entity id: " + id));
            return 0;
        }
        var type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(identifier);
        if (type == null) {
            source.sendFailure(Component.literal("[Tamekind] Unknown entity type: " + identifier));
            return 0;
        }
        boolean nowDisabled = com.tamekind.ai.TamekindAnimalRules.toggleRuntimeDisable(type);
        source.sendSuccess(() -> Component.literal("[Tamekind] " + identifier + (nowDisabled ? " disabled" : " enabled")), true);
        return 1;
    }

    private static int trustMap(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer target = net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player");
        ServerLevel level = source.getLevel();
        AABB box = AABB.ofSize(source.getPosition(), 48.0, 48.0, 48.0);
        long now = level.getGameTime();
        int total = 0, trusting = 0;
        double sum = 0.0;
        for (Animal a : level.getEntitiesOfClass(Animal.class, box)) {
            total++;
            double t = AnimalMemoryStore.get(a).trustScore(target.getUUID(), now);
            if (t > 0.0) { trusting++; sum += t; }
        }
        final int tot = total, tr = trusting;
        final double avg = tr == 0 ? 0.0 : sum / tr;
        source.sendSuccess(() -> Component.literal(String.format(
                "[Tamekind] within 48 of caller: %d animals, %d trust %s (avg=%.2f)",
                tot, tr, target.getName().getString(), avg)), false);
        return 1;
    }

    private static int reportSeason(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        var s = com.tamekind.ai.BreedingSeason.current(ctx.getSource().getLevel());
        boolean breeding = com.tamekind.ai.BreedingSeason.isBreedingSeason(ctx.getSource().getLevel());
        ctx.getSource().sendSuccess(() -> Component.literal(
                "[Tamekind] season=" + s.name().toLowerCase(java.util.Locale.ROOT)
                        + " breeding=" + breeding
                        + " (gated=" + TamekindConfig.seasonalBreedingEnabled + ")"), false);
        return 1;
    }

    private static int reportTrust(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("[Tamekind] Trust command requires a player."));
            return 0;
        }
        ServerLevel level = source.getLevel();
        AABB box = AABB.ofSize(source.getPosition(), ANIMAL_SCAN_SIZE, ANIMAL_SCAN_SIZE, ANIMAL_SCAN_SIZE);
        Animal animal = level.getEntitiesOfClass(Animal.class, box).stream()
                .min(Comparator.comparingDouble(c -> c.distanceToSqr(source.getPosition())))
                .orElse(null);
        if (animal == null) {
            source.sendFailure(Component.literal("[Tamekind] No animal nearby."));
            return 0;
        }
        long now = level.getGameTime();
        double trust = AnimalMemoryStore.get(animal).trustScore(player.getUUID(), now);
        source.sendSuccess(() -> Component.literal(String.format(
                "[Tamekind] %s#%d trusts %s = %.2f",
                animal.getType().toShortString(), animal.getId(), player.getName().getString(), trust)), false);
        return 1;
    }

    private static int dumpNearest(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        AABB box = AABB.ofSize(source.getPosition(), ANIMAL_SCAN_SIZE, ANIMAL_SCAN_SIZE, ANIMAL_SCAN_SIZE);
        Animal animal = level.getEntitiesOfClass(Animal.class, box).stream()
                .min(Comparator.comparingDouble(c -> c.distanceToSqr(source.getPosition())))
                .orElse(null);
        if (animal == null) {
            source.sendFailure(Component.literal("[Tamekind] No animal nearby."));
            return 0;
        }
        long now = level.getGameTime();
        AnimalMemory m = AnimalMemoryStore.get(animal);
        StringBuilder sb = new StringBuilder();
        sb.append(animal.getType().toShortString()).append('#').append(animal.getId());
        sb.append("\n  lod=").append(AiLod.computeFresh(animal));
        sb.append(" baby=").append(animal.isBaby());
        sb.append(" herdable=").append(HerdCoordinator.isHerdable(animal));
        sb.append(" herdSize=").append(HerdCoordinator.herdSize(animal));
        sb.append("\n  danger=").append(m.dangerPos(now)).append(" remaining=").append(m.dangerTicksRemaining(now));
        sb.append("\n  guarding=").append(m.isGuarding(now));
        sb.append("\n  home=").append(m.home());
        sb.append("\n  sharedShelter=").append(m.sharedShelter(now));
        sb.append("\n  sharedGraze=").append(m.sharedGraze(now));
        sb.append("\n  sharedWater=").append(m.sharedWater(now));
        sb.append("\n  trustedPlayers=").append(m.activeTrustCount(now));
        Animal leader = HerdCoordinator.leaderFor(animal);
        sb.append("\n  leader=").append(formatAnimal(leader));
        sb.append(" isAlpha=").append(leader == animal);
        sb.append("\n  sentinel=").append(formatAnimal(HerdCoordinator.sentinelFor(animal)));
        sb.append(" onWatch=").append(HerdCoordinator.sentinelFor(animal) == animal);
        sb.append(" isolated=").append(HerdCoordinator.isIsolated(animal));
        sb.append("\n  temperament=").append(com.tamekind.ai.AnimalTemperament.forAnimal(animal).lowerName());
        sb.append(" inheritedScale=").append(Double.isNaN(m.inheritedScale()) ? "wild"
                : String.format("%.3f", m.inheritedScale()));
        sb.append(" trailPoints=").append(m.trailSize());
        sb.append("\n  cullsWitnessed=").append(m.cullCount(now));
        sb.append(" standsGround=").append(com.tamekind.ai.Disposition.standsGround(animal));
        sb.append("\n  condition=").append(TamekindConfig.conditionEnabled
                ? String.format("%.2f", m.condition()) : "disabled");
        source.sendSuccess(() -> Component.literal("[Tamekind] " + sb), false);
        return 1;
    }

    /**
     * Dumps the nearest animal's live goal table with priorities, flags and running
     * state, then flags every same-priority flag collision.
     *
     * <p>This reads the real selector, so it includes vanilla's goals and any other
     * mod's — contention that no compile-time check can see. When a Tamekind behaviour
     * "never happens", this is the first thing to look at: the cause is usually a goal
     * at a strictly lower priority holding MOVE, not the behaviour's own conditions.
     */
    private static int reportGoals(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        AABB box = AABB.ofSize(source.getPosition(), ANIMAL_SCAN_SIZE, ANIMAL_SCAN_SIZE, ANIMAL_SCAN_SIZE);
        Animal animal = level.getEntitiesOfClass(Animal.class, box).stream()
                .min(Comparator.comparingDouble(c -> c.distanceToSqr(source.getPosition())))
                .orElse(null);
        if (animal == null) {
            source.sendFailure(Component.literal("[Tamekind] No animal nearby."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(String.format(
                "[Tamekind] goal table for %s#%d  (prio | owner | goal | flags | running)",
                animal.getType().toShortString(), animal.getId())), false);
        for (com.tamekind.ai.GoalDiagnostics.Entry e : com.tamekind.ai.GoalDiagnostics.inspect(animal)) {
            source.sendSuccess(() -> Component.literal(String.format(
                    "  %3d %-9s %-28s %-14s %s",
                    e.priority(),
                    e.tamekind() ? "tamekind" : "vanilla",
                    e.name(),
                    e.flags(),
                    e.running() ? "RUNNING" : "")), false);
        }

        var conflicts = com.tamekind.ai.GoalDiagnostics.conflicts(animal);
        if (conflicts.isEmpty()) {
            source.sendSuccess(() -> Component.literal("  no same-priority flag collisions"), false);
            return 1;
        }
        source.sendSuccess(() -> Component.literal(String.format(
                "  %d same-priority flag collision(s) - the selector cannot break these ties:",
                conflicts.size())), false);
        for (com.tamekind.ai.GoalDiagnostics.Conflict c : conflicts) {
            source.sendSuccess(() -> Component.literal(String.format(
                    "  COLLISION prio %d: %s vs %s share %s",
                    c.priority(), c.first(), c.second(), c.sharedFlags())), false);
        }
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
            source.sendFailure(Component.literal("[Tamekind] No animal nearby."));
            return 0;
        }
        AnimalMemoryStore.get(animal).forget();
        source.sendSuccess(() -> Component.literal("[Tamekind] Forgot memory for " + animal.getType().toShortString() + "#" + animal.getId()), false);
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
            source.sendFailure(Component.literal("[Tamekind] No animal nearby."));
            return 0;
        }
        Animal leader = HerdCoordinator.leaderFor(animal);
        long now = level.getGameTime();
        BlockPos shared = leader == null ? null : AnimalMemoryStore.get(leader).sharedShelter(now);
        source.sendSuccess(() -> Component.literal(String.format(
                "[Tamekind] %s#%d leader=%s isAlpha=%s herdSize=%d sharedShelter=%s",
                animal.getType().toShortString(),
                animal.getId(),
                formatAnimal(leader),
                leader == animal,
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
                "[Tamekind] within 64: total=%d herdable=%d  LOD full=%d simple=%d sleep=%d",
                t, h, f, s, sl)), false);
        return 1;
    }

    private static int setHome(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        AABB box = AABB.ofSize(source.getPosition(), ANIMAL_SCAN_SIZE, ANIMAL_SCAN_SIZE, ANIMAL_SCAN_SIZE);
        Animal animal = level.getEntitiesOfClass(Animal.class, box).stream()
                .min(Comparator.comparingDouble(c -> c.distanceToSqr(source.getPosition())))
                .orElse(null);
        if (animal == null) {
            source.sendFailure(Component.literal("[Tamekind] No animal nearby."));
            return 0;
        }
        BlockPos pos = BlockPos.containing(source.getPosition());
        AnimalMemoryStore.get(animal).setHome(pos);
        source.sendSuccess(() -> Component.literal("[Tamekind] Set home for "
                + animal.getType().toShortString() + "#" + animal.getId()
                + " to " + pos.getX() + " " + pos.getY() + " " + pos.getZ()), false);
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
            source.sendFailure(Component.literal("[Tamekind] No animal within " + (int) ANIMAL_SCAN_SIZE + " blocks."));
            return 0;
        }
        AnimalMemoryStore.get(animal).setHome(null);
        source.sendSuccess(() -> Component.literal("[Tamekind] Cleared home for " + animal.getType().toShortString() + "#" + animal.getId()), false);
        return 1;
    }

    private static int reloadConfig(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        try {
            TamekindConfig.load(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("tamekind.properties"));
            // Let the goal table be logged again, so toggling debugLogs on and reloading
            // is enough to see it without restarting the server.
            com.tamekind.ai.PassiveGoalInjector.resetGoalTableLog();
            source.sendSuccess(() -> Component.literal("[Tamekind] Config reloaded."), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("[Tamekind] Reload failed: " + e.getMessage()));
            return 0;
        }
    }

    private static int reportProfile(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.literal("[Tamekind] Active profile: " + TamekindConfig.activeProfile), false);
        return 1;
    }

    private static int applyProfile(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String name = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "name");
        TamekindConfig.Profile profile = TamekindConfig.Profile.fromString(name);
        if (profile == null) {
            source.sendFailure(Component.literal("[Tamekind] Unknown profile '" + name + "'. Use vanilla+, realism, or simulation."));
            return 0;
        }
        TamekindConfig.applyProfile(profile);
        source.sendSuccess(() -> Component.literal("[Tamekind] Applied profile: " + profile.name().toLowerCase(java.util.Locale.ROOT)), true);
        return 1;
    }

    private static int reportConfig(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        source.sendSuccess(() -> Component.literal(String.format(
                "[Tamekind] enabled=%s fullAiRange=%d simpleAiRange=%d aiLodCacheTicks=%d panicRadius=%d alertRadius=%d dangerSpreadCooldown=%d",
                TamekindConfig.enabled,
                TamekindConfig.fullAiRange,
                TamekindConfig.simpleAiRange,
                TamekindConfig.aiLodCacheTicks,
                TamekindConfig.panicRadius,
                TamekindConfig.alertRadius,
                TamekindConfig.herdDangerSpreadCooldownTicks)), false);
        source.sendSuccess(() -> Component.literal(String.format(
                "  herd=%s habitat=%s dailyRhythm=%s trust=%s babyAnchoring=%s stampede=%s",
                TamekindConfig.herdEnabled,
                TamekindConfig.habitatEnabled,
                TamekindConfig.dailyRhythmEnabled,
                TamekindConfig.trustEnabled,
                TamekindConfig.babyAnchoringEnabled,
                TamekindConfig.stampedeEnabled)), false);
        source.sendSuccess(() -> Component.literal(String.format(
                "  breedingCrowdControl=%s crowdRadius=%d crowdHardLimit=%d",
                TamekindConfig.breedingCrowdControlEnabled,
                TamekindConfig.breedingCrowdRadius,
                TamekindConfig.breedingCrowdHardLimit)), false);
        source.sendSuccess(() -> Component.literal(String.format(
                "  respectLeashed=%s respectMounted=%s respectBreeding=%s respectNamed=%s",
                TamekindConfig.respectLeashedAnimals,
                TamekindConfig.respectMountedAnimals,
                TamekindConfig.respectBreedingAnimals,
                TamekindConfig.respectNamedAnimals)), false);
        source.sendSuccess(() -> Component.literal(String.format(
                "  graze: radius=%d interval=%d duration=%d  drink: radius=%d interval=%d duration=%d",
                TamekindConfig.grazeSearchRadius,
                TamekindConfig.grazeMinIntervalTicks,
                TamekindConfig.grazeDurationTicks,
                TamekindConfig.drinkSearchRadius,
                TamekindConfig.drinkMinIntervalTicks,
                TamekindConfig.drinkDurationTicks)), false);
        source.sendSuccess(() -> Component.literal(String.format(
                "  parentGuard=%s ticks=%d radius=%d  breedingSoftLimit=%d",
                TamekindConfig.parentGuardEnabled,
                TamekindConfig.parentGuardTicks,
                TamekindConfig.parentGuardRadius,
                TamekindConfig.breedingCrowdSoftLimit)), false);
        source.sendSuccess(() -> Component.literal(String.format(
                "  homeReturn=%s min=%d max=%d interval=%d speed=%.2f",
                TamekindConfig.homeReturnEnabled,
                TamekindConfig.homeReturnMinDistance,
                TamekindConfig.homeReturnMaxDistance,
                TamekindConfig.homeReturnIntervalTicks,
                TamekindConfig.homeReturnSpeed)), false);
        source.sendSuccess(() -> Component.literal(String.format(
                "  alertFreeze: min=%d random=%d driftSpeed=%.2f",
                TamekindConfig.alertFreezeMinTicks,
                TamekindConfig.alertFreezeRandomTicks,
                TamekindConfig.alertDriftSpeed)), false);
        return 1;
    }

    private static int tamekindGoalCount(Animal animal) {
        int count = 0;
        for (WrappedGoal goal : ((MobGoalSelectorAccessor) animal).tamekind$goalSelector().getAvailableGoals()) {
            if (goal.getGoal() instanceof TamekindGoal) {
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
