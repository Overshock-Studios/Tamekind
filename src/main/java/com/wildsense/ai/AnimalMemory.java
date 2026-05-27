package com.tamekind.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class AnimalMemory {
    private static final String DANGER = "Danger";
    private static final String HOME = "Home";
    private static final String TRUST = "Trust";
    private static final String X = "X";
    private static final String Y = "Y";
    private static final String Z = "Z";
    private static final String UNTIL = "Until";
    private static final String SCORE = "Score";
    private static final String PLAYER = "Player";

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
    }

    public void markGuarding(long untilTick) {
        if (untilTick > guardUntil) guardUntil = untilTick;
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
        return entry.score;
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

    public void save(ValueOutput output) {
        if (dangerPos != null) {
            ValueOutput danger = output.child(DANGER);
            danger.putDouble(X, dangerPos.x);
            danger.putDouble(Y, dangerPos.y);
            danger.putDouble(Z, dangerPos.z);
            danger.putLong(UNTIL, dangerUntil);
        }
        if (guardUntil > 0) output.putLong("GuardUntil", guardUntil);
        if (nextDangerSpreadAt > 0) output.putLong("NextSpread", nextDangerSpreadAt);
        if (sharedShelter != null && sharedShelterUntil > 0) {
            ValueOutput shared = output.child("SharedShelter");
            shared.putInt(X, sharedShelter.getX());
            shared.putInt(Y, sharedShelter.getY());
            shared.putInt(Z, sharedShelter.getZ());
            shared.putLong(UNTIL, sharedShelterUntil);
        }
        if (sharedGraze != null && sharedGrazeUntil > 0) {
            ValueOutput shared = output.child("SharedGraze");
            shared.putInt(X, sharedGraze.getX());
            shared.putInt(Y, sharedGraze.getY());
            shared.putInt(Z, sharedGraze.getZ());
            shared.putLong(UNTIL, sharedGrazeUntil);
        }
        if (sharedWater != null && sharedWaterUntil > 0) {
            ValueOutput shared = output.child("SharedWater");
            shared.putInt(X, sharedWater.getX());
            shared.putInt(Y, sharedWater.getY());
            shared.putInt(Z, sharedWater.getZ());
            shared.putLong(UNTIL, sharedWaterUntil);
        }
        if (homePos != null) {
            ValueOutput home = output.child(HOME);
            home.putInt(X, homePos.getX());
            home.putInt(Y, homePos.getY());
            home.putInt(Z, homePos.getZ());
        }
        if (!trustedPlayers.isEmpty()) {
            ValueOutput.ValueOutputList trust = output.childrenList(TRUST);
            for (Map.Entry<UUID, TrustEntry> entry : trustedPlayers.entrySet()) {
                ValueOutput saved = trust.addChild();
                saved.putString(PLAYER, entry.getKey().toString());
                saved.putDouble(SCORE, entry.getValue().score);
                saved.putLong(UNTIL, entry.getValue().untilTick);
            }
        }
    }

    public void load(ValueInput input, long gameTime) {
        dangerPos = null;
        dangerUntil = 0L;
        homePos = null;
        guardUntil = input.getLongOr("GuardUntil", 0L);
        nextDangerSpreadAt = input.getLongOr("NextSpread", 0L);
        sharedShelter = null;
        sharedShelterUntil = 0L;
        sharedGraze = null;
        sharedGrazeUntil = 0L;
        sharedWater = null;
        sharedWaterUntil = 0L;
        trustedPlayers.clear();

        ValueInput shared = input.childOrEmpty("SharedShelter");
        long sharedUntil = shared.getLongOr(UNTIL, 0L);
        if (sharedUntil > gameTime) {
            sharedShelter = new BlockPos(
                    shared.getIntOr(X, 0),
                    shared.getIntOr(Y, 0),
                    shared.getIntOr(Z, 0));
            sharedShelterUntil = sharedUntil;
        }

        ValueInput sg = input.childOrEmpty("SharedGraze");
        long sgUntil = sg.getLongOr(UNTIL, 0L);
        if (sgUntil > gameTime) {
            sharedGraze = new BlockPos(
                    sg.getIntOr(X, 0),
                    sg.getIntOr(Y, 0),
                    sg.getIntOr(Z, 0));
            sharedGrazeUntil = sgUntil;
        }

        ValueInput sw = input.childOrEmpty("SharedWater");
        long swUntil = sw.getLongOr(UNTIL, 0L);
        if (swUntil > gameTime) {
            sharedWater = new BlockPos(
                    sw.getIntOr(X, 0),
                    sw.getIntOr(Y, 0),
                    sw.getIntOr(Z, 0));
            sharedWaterUntil = swUntil;
        }

        ValueInput home = input.childOrEmpty(HOME);
        int hx = home.getIntOr(X, Integer.MIN_VALUE);
        if (hx != Integer.MIN_VALUE) {
            homePos = new BlockPos(hx, home.getIntOr(Y, 0), home.getIntOr(Z, 0));
        }

        ValueInput danger = input.childOrEmpty(DANGER);
        long savedDangerUntil = danger.getLongOr(UNTIL, 0L);
        if (savedDangerUntil > gameTime) {
            dangerPos = new Vec3(
                    danger.getDoubleOr(X, 0.0),
                    danger.getDoubleOr(Y, 0.0),
                    danger.getDoubleOr(Z, 0.0));
            dangerUntil = savedDangerUntil;
        }

        for (ValueInput saved : input.childrenListOrEmpty(TRUST)) {
            try {
                UUID uuid = UUID.fromString(saved.getStringOr(PLAYER, ""));
                double score = saved.getDoubleOr(SCORE, 0.0);
                long until = saved.getLongOr(UNTIL, 0L);
                if (score > 0.0 && until > gameTime) {
                    trustedPlayers.put(uuid, new TrustEntry(Math.min(1.0, score), until));
                }
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed third-party or old data.
            }
        }
    }

    private record TrustEntry(double score, long untilTick) {
    }
}
