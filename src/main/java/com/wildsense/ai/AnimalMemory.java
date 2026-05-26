package com.wildsense.ai;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class AnimalMemory {
    private static final String DANGER = "Danger";
    private static final String TRUST = "Trust";
    private static final String X = "X";
    private static final String Y = "Y";
    private static final String Z = "Z";
    private static final String UNTIL = "Until";
    private static final String SCORE = "Score";
    private static final String PLAYER = "Player";

    private Vec3 dangerPos;
    private long dangerUntil;
    private final Map<UUID, TrustEntry> trustedPlayers = new HashMap<>();

    public void rememberDanger(Vec3 pos, long untilTick) {
        this.dangerPos = pos;
        this.dangerUntil = untilTick;
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
        trustedPlayers.clear();

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
