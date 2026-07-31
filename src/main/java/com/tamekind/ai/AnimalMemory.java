package com.tamekind.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class AnimalMemory {

    /** A position with an expiry, which is the shape of most of this class. */
    private record Timed<P>(P pos, long until) {
        static <P> com.mojang.serialization.Codec<Timed<P>> codec(
                com.mojang.serialization.Codec<P> posCodec) {
            return com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
                    posCodec.fieldOf("Pos").forGetter(Timed::pos),
                    com.mojang.serialization.Codec.LONG.fieldOf("Until").forGetter(Timed::until)
            ).apply(i, Timed::new));
        }
    }

    private record Trust(java.util.UUID player, double score, long until) {
        static final com.mojang.serialization.Codec<Trust> CODEC =
                com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
                        net.minecraft.core.UUIDUtil.CODEC.fieldOf("Player").forGetter(Trust::player),
                        com.mojang.serialization.Codec.DOUBLE.fieldOf("Score").forGetter(Trust::score),
                        com.mojang.serialization.Codec.LONG.fieldOf("Until").forGetter(Trust::until)
                ).apply(i, Trust::new));
    }

    private static final com.mojang.serialization.Codec<Timed<Vec3>> DANGER_CODEC =
            Timed.codec(Vec3.CODEC);
    private static final com.mojang.serialization.Codec<Timed<BlockPos>> SHARED_CODEC =
            Timed.codec(BlockPos.CODEC);

    /**
     * Persistence for the Fabric attachment this lives in.
     *
     * <p>Every field is optional with a sane default, so an animal from a world saved
     * before this existed decodes cleanly rather than failing and losing its memory. The
     * trail is deliberately absent: it is transient by design, and a stale route is worse
     * than no route.
     */
    public static final com.mojang.serialization.Codec<AnimalMemory> CODEC =
            com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
                    DANGER_CODEC.optionalFieldOf("Danger").forGetter(m ->
                            m.dangerPos == null ? java.util.Optional.<Timed<Vec3>>empty()
                                    : java.util.Optional.of(new Timed<>(m.dangerPos, m.dangerUntil))),
                    BlockPos.CODEC.optionalFieldOf("Home").forGetter(m ->
                            java.util.Optional.ofNullable(m.homePos)),
                    SHARED_CODEC.optionalFieldOf("SharedShelter").forGetter(m ->
                            m.sharedShelter == null ? java.util.Optional.<Timed<BlockPos>>empty()
                                    : java.util.Optional.of(new Timed<>(m.sharedShelter, m.sharedShelterUntil))),
                    SHARED_CODEC.optionalFieldOf("SharedGraze").forGetter(m ->
                            m.sharedGraze == null ? java.util.Optional.<Timed<BlockPos>>empty()
                                    : java.util.Optional.of(new Timed<>(m.sharedGraze, m.sharedGrazeUntil))),
                    SHARED_CODEC.optionalFieldOf("SharedWater").forGetter(m ->
                            m.sharedWater == null ? java.util.Optional.<Timed<BlockPos>>empty()
                                    : java.util.Optional.of(new Timed<>(m.sharedWater, m.sharedWaterUntil))),
                    com.mojang.serialization.Codec.LONG.optionalFieldOf("GuardUntil", 0L)
                            .forGetter(m -> m.guardUntil),
                    com.mojang.serialization.Codec.LONG.optionalFieldOf("NextSpread", 0L)
                            .forGetter(m -> m.nextDangerSpreadAt),
                    com.mojang.serialization.Codec.LONG.optionalFieldOf("VengeanceUntil", 0L)
                            .forGetter(m -> m.vengeanceUntil),
                    com.mojang.serialization.Codec.LONG.optionalFieldOf("CullDecayAt", 0L)
                            .forGetter(m -> m.cullDecayAt),
                    com.mojang.serialization.Codec.INT.optionalFieldOf("CullCount", 0)
                            .forGetter(m -> m.cullCount),
                    // Absent means a wild roll, which is why this is Optional and not a
                    // NaN written into the save file.
                    com.mojang.serialization.Codec.DOUBLE.optionalFieldOf("InheritedScale")
                            .forGetter(m -> Double.isNaN(m.inheritedScale)
                                    ? java.util.Optional.<Double>empty()
                                    : java.util.Optional.of(m.inheritedScale)),
                    com.mojang.serialization.Codec.DOUBLE.optionalFieldOf("Condition", 1.0)
                            .forGetter(m -> m.condition),
                    Trust.CODEC.listOf().optionalFieldOf("Trust", java.util.List.of())
                            .forGetter(m -> m.trustedPlayers.entrySet().stream()
                                    .map(e -> new Trust(e.getKey(), e.getValue().score(), e.getValue().untilTick()))
                                    .toList())
            ).apply(i, AnimalMemory::fromCodec));

    private static AnimalMemory fromCodec(
            java.util.Optional<Timed<Vec3>> danger,
            java.util.Optional<BlockPos> home,
            java.util.Optional<Timed<BlockPos>> shelter,
            java.util.Optional<Timed<BlockPos>> graze,
            java.util.Optional<Timed<BlockPos>> water,
            long guardUntil, long nextSpread, long vengeanceUntil, long cullDecayAt,
            int cullCount, java.util.Optional<Double> inheritedScale, double condition,
            java.util.List<Trust> trust) {
        AnimalMemory m = new AnimalMemory();
        danger.ifPresent(d -> { m.dangerPos = d.pos(); m.dangerUntil = d.until(); });
        home.ifPresent(p -> m.homePos = p);
        shelter.ifPresent(d -> { m.sharedShelter = d.pos(); m.sharedShelterUntil = d.until(); });
        graze.ifPresent(d -> { m.sharedGraze = d.pos(); m.sharedGrazeUntil = d.until(); });
        water.ifPresent(d -> { m.sharedWater = d.pos(); m.sharedWaterUntil = d.until(); });
        m.guardUntil = guardUntil;
        m.nextDangerSpreadAt = nextSpread;
        m.vengeanceUntil = vengeanceUntil;
        m.cullDecayAt = cullDecayAt;
        m.cullCount = cullCount;
        m.inheritedScale = inheritedScale.orElse(Double.NaN);
        m.condition = Math.min(1.0, Math.max(0.0, condition));
        for (Trust t : trust) {
            if (t.score() > 0.0) {
                m.trustedPlayers.put(t.player(), new TrustEntry(Math.min(1.0, t.score()), t.until()));
            }
        }
        return m;
    }


    private Vec3 dangerPos;
    private long dangerUntil;
    private long nextDangerSpreadAt;
    private long guardUntil;
    private BlockPos homePos;
    private BlockPos sharedShelter;
    private long sharedShelterUntil;
    private BlockPos sharedGraze;
    private long sharedGrazeUntil;
    private BlockPos sharedWater;
    private long sharedWaterUntil;
    private final Map<UUID, TrustEntry> trustedPlayers = new HashMap<>();

    /** Scale inherited from both parents, or NaN when this animal was not bred. */
    private double inheritedScale = Double.NaN;
    /** Herd-mates seen killed recently; drives territorial retaliation. */
    private int cullCount;
    private long cullDecayAt;
    /** While set, this animal stands its ground instead of fleeing. */
    private long vengeanceUntil;
    /** Body condition, 0..1. Only meaningful when {@code conditionEnabled}. */
    private double condition = 1.0;
    private long conditionTickedAt;

    /**
     * Recent alpha positions, oldest first. Transient on purpose: a stale trail
     * across a reload would path followers at coordinates the leader left minutes ago.
     */
    private final java.util.ArrayDeque<BlockPos> trail = new java.util.ArrayDeque<>();
    private long lastTrailAt;

    public void rememberDanger(Vec3 pos, long untilTick) {
        this.dangerPos = pos;
        this.dangerUntil = untilTick;
    }

    public void setHome(BlockPos pos) {
        this.homePos = pos == null ? null : pos.immutable();
    }

    public BlockPos home() {
        return homePos;
    }

    public void setSharedShelter(BlockPos pos, long untilTick) {
        this.sharedShelter = pos == null ? null : pos.immutable();
        this.sharedShelterUntil = untilTick;
    }

    public BlockPos sharedShelter(long gameTime) {
        return gameTime <= sharedShelterUntil ? sharedShelter : null;
    }

    public void setSharedGraze(BlockPos pos, long untilTick) {
        this.sharedGraze = pos == null ? null : pos.immutable();
        this.sharedGrazeUntil = untilTick;
    }

    public BlockPos sharedGraze(long gameTime) {
        return gameTime <= sharedGrazeUntil ? sharedGraze : null;
    }

    public void setSharedWater(BlockPos pos, long untilTick) {
        this.sharedWater = pos == null ? null : pos.immutable();
        this.sharedWaterUntil = untilTick;
    }

    public BlockPos sharedWater(long gameTime) {
        return gameTime <= sharedWaterUntil ? sharedWater : null;
    }

    public void removeTrust(java.util.UUID playerId, double amount) {
        TrustEntry current = trustedPlayers.get(playerId);
        if (current == null) return;
        double newScore = current.score - amount;
        if (newScore <= 0.0) {
            trustedPlayers.remove(playerId);
        } else {
            trustedPlayers.put(playerId, new TrustEntry(newScore, current.untilTick));
        }
    }

    public void forget() {
        dangerPos = null;
        dangerUntil = 0L;
        homePos = null;
        guardUntil = 0L;
        sharedShelter = null;
        sharedShelterUntil = 0L;
        sharedGraze = null;
        sharedGrazeUntil = 0L;
        sharedWater = null;
        sharedWaterUntil = 0L;
        cullCount = 0;
        cullDecayAt = 0L;
        vengeanceUntil = 0L;
        trail.clear();
        lastTrailAt = 0L;
        // inheritedScale survives /tamekind forget: it is the animal's descent, not a
        // memory it can be talked out of.
    }

    public void markGuarding(long untilTick) {
        if (untilTick > guardUntil) guardUntil = untilTick;
    }

    // ── Heritable size ────────────────────────────────────────────────────────

    /** The bred-in scale, or NaN when this animal should fall back to its UUID seed. */
    public double inheritedScale() {
        return inheritedScale;
    }

    public void setInheritedScale(double scale) {
        this.inheritedScale = scale;
    }

    // ── Territorial retaliation ───────────────────────────────────────────────

    /**
     * Records one herd-mate death. Returns the running count, which resets once the
     * decay window lapses so a herd culled steadily over hours never accumulates.
     */
    public int recordCull(long gameTime, int decayTicks) {
        if (gameTime > cullDecayAt) cullCount = 0;
        cullCount++;
        cullDecayAt = gameTime + Math.max(1, decayTicks);
        return cullCount;
    }

    public int cullCount(long gameTime) {
        return gameTime > cullDecayAt ? 0 : cullCount;
    }

    public void markVengeful(long untilTick) {
        if (untilTick > vengeanceUntil) vengeanceUntil = untilTick;
    }

    /** True while this animal stands its ground rather than fleeing. */
    public boolean isVengeful(long gameTime) {
        return gameTime <= vengeanceUntil;
    }

    // ── Body condition (opt-in, non-lethal) ───────────────────────────────────

    public double condition() {
        return condition;
    }

    /**
     * Drains condition at most once per {@code intervalTicks}. Never falls below the
     * configured floor: condition slows and discourages an animal, it never kills one.
     */
    public void decayCondition(long gameTime, int intervalTicks, double amount, double floor) {
        if (gameTime - conditionTickedAt < Math.max(1, intervalTicks)) return;
        conditionTickedAt = gameTime;
        condition = Math.max(floor, condition - amount);
    }

    public void restoreCondition(double amount) {
        condition = Math.min(1.0, condition + amount);
    }

    // ── Alpha trail ───────────────────────────────────────────────────────────

    /** Appends a trail point, rate-limited, keeping at most {@code maxPoints}. */
    public void pushTrail(BlockPos pos, long gameTime, int everyTicks, int maxPoints) {
        // The empty check has to come first: lastTrailAt starts at 0, so on a young world
        // the rate limit would swallow the very first point and leave followers with
        // nothing to walk.
        if (!trail.isEmpty() && gameTime - lastTrailAt < Math.max(1, everyTicks)) return;
        lastTrailAt = gameTime;
        if (!trail.isEmpty() && trail.peekLast().distSqr(pos) < 4.0) return;
        trail.addLast(pos.immutable());
        while (trail.size() > Math.max(1, maxPoints)) trail.pollFirst();
    }

    /** The oldest recorded point still within {@code maxDistSqr} of the follower. */
    public BlockPos trailPointFor(BlockPos follower, double maxDistSqr) {
        for (BlockPos p : trail) {
            if (follower.distSqr(p) <= maxDistSqr) return p;
        }
        return null;
    }

    public void clearTrail() {
        trail.clear();
    }

    /** Number of recorded trail points. Surfaced for {@code /tamekind dump} and tests. */
    public int trailSize() {
        return trail.size();
    }

    public boolean isGuarding(long gameTime) {
        return gameTime <= guardUntil;
    }

    public boolean canSpreadDanger(long gameTime, int cooldownTicks) {
        if (gameTime < nextDangerSpreadAt) return false;
        nextDangerSpreadAt = gameTime + Math.max(1, cooldownTicks);
        return true;
    }

    public Vec3 dangerPos(long gameTime) {
        return gameTime <= dangerUntil ? dangerPos : null;
    }

    public long dangerTicksRemaining(long gameTime) {
        return dangerPos == null ? 0L : Math.max(0L, dangerUntil - gameTime);
    }

    public void addTrust(UUID playerId, double amount, long untilTick) {
        TrustEntry current = trustedPlayers.get(playerId);
        double score = current == null ? 0.0 : current.score;
        trustedPlayers.put(playerId, new TrustEntry(Math.min(1.0, score + amount), untilTick));
    }

    public boolean trusts(UUID playerId, long gameTime) {
        return trustScore(playerId, gameTime) > 0.0;
    }

    public double trustScore(UUID playerId, long gameTime) {
        TrustEntry entry = trustedPlayers.get(playerId);
        if (entry == null || gameTime > entry.untilTick) return 0.0;
        long ticksLeft = entry.untilTick - gameTime;
        int half = Math.max(1, com.tamekind.config.TamekindConfig.trustTicks / 2);
        if (ticksLeft >= half) return entry.score;
        return entry.score * ((double) ticksLeft / half);
    }

    public int activeTrustCount(long gameTime) {
        int count = 0;
        for (TrustEntry entry : trustedPlayers.values()) {
            if (gameTime <= entry.untilTick && entry.score > 0.0) {
                count++;
            }
        }
        return count;
    }

    public void tick(long gameTime) {
        if (gameTime > dangerUntil) {
            dangerPos = null;
        }
        Iterator<Map.Entry<UUID, TrustEntry>> iterator = trustedPlayers.entrySet().iterator();
        while (iterator.hasNext()) {
            if (gameTime > iterator.next().getValue().untilTick) {
                iterator.remove();
            }
        }
    }



    private record TrustEntry(double score, long untilTick) {
    }
}
