package com.tamekind.config;

import com.tamekind.TamekindMod;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class TamekindConfig {
    public static boolean enabled = true;
    public static boolean herdEnabled = true;
    public static boolean alertEnabled = true;
    public static boolean panicEnabled = true;
    public static boolean habitatEnabled = true;
    public static boolean trustEnabled = true;
    public static boolean stampedeEnabled = true;
    public static boolean babyAnchoringEnabled = true;
    public static boolean breedingCrowdControlEnabled = true;
    public static boolean breedingCrowdMessageEnabled = true;
    public static boolean dailyRhythmEnabled = true;
    public static boolean respectLeashedAnimals = true;
    public static boolean respectMountedAnimals = true;
    public static boolean respectBreedingAnimals = true;
    public static boolean respectNamedAnimals = true;

    public static int fullAiRange = 48;
    public static int simpleAiRange = 96;
    public static int aiLodCacheTicks = 20;
    public static int alertRadius = 18;
    public static int panicRadius = 8;
    public static int herdSearchRadius = 16;
    public static int shelterSearchRadius = 10;
    public static int memoryTicks = 20 * 30;
    public static int herdDangerSpreadCooldownTicks = 20;
    public static int trustTicks = 20 * 60 * 20;
    public static int minStampedeHerdSize = 5;
    public static int babyAnchorSearchRadius = 14;
    public static int panicCandidateCount = 10;
    public static int panicEscapeDistance = 12;
    public static int panicDropCheckDepth = 4;
    public static int breedingCrowdRadius = 6;
    public static int breedingCrowdHardLimit = 24;
    public static int breedingCrowdSoftLimit = 8;
    public static int grazeSearchRadius = 5;
    public static int grazeMinIntervalTicks = 20 * 12;
    public static int grazeDurationTicks = 20 * 4;
    public static int drinkSearchRadius = 8;
    public static int drinkMinIntervalTicks = 20 * 60;
    public static int drinkDurationTicks = 20 * 3;
    public static int parentGuardTicks = 20 * 8;
    public static int parentGuardRadius = 12;
    public static boolean parentGuardEnabled = true;
    public static boolean homeReturnEnabled = true;
    public static int homeReturnMaxDistance = 64;
    public static int homeReturnMinDistance = 16;
    public static int homeReturnIntervalTicks = 20 * 30;
    public static double homeReturnSpeed = 0.9;
    public static int alertFreezeMinTicks = 30;
    public static int alertFreezeRandomTicks = 30;
    public static double alertDriftSpeed = 0.65;

    public static double panicSpeed = 1.35;
    public static double babyPanicSpeedMultiplier = 0.75;
    public static double babyPanicRadiusMultiplier = 0.7;
    public static volatile String activeProfile = "default";
    public static double herdFollowSpeed = 1.05;
    public static double babyAnchorSpeed = 1.2;
    public static double shelterSpeed = 1.0;
    public static double stampedeKnockback = 0.35;
    public static double stampedeHerdScaling = 0.08;
    public static double trustPerFeeding = 0.22;
    public static double trustLossPerHit = 0.5;
    public static double trustHitForgivenessThreshold = 0.6;
    public static double lowHpThresholdFraction = 0.3;
    public static double limpSpeedMultiplier = 0.6;
    public static boolean followTrustedEnabled = true;
    public static int followTrustedRadius = 24;
    public static double followTrustedMinTrust = 0.5;
    public static double followTrustedSpeed = 1.0;
    public static boolean stampedeCropDamageEnabled = false;
    public static double stampedeCropDamageChance = 0.05;
    public static boolean panicSoundEnabled = true;
    public static float panicSoundVolume = 0.4f;
    public static int idleBondTickInterval = 20 * 30;
    public static double idleBondTrustGain = 0.01;
    public static int idleBondRadius = 8;
    public static boolean sizeVarianceEnabled = true;
    public static double sizeVarianceRange = 0.10;
    public static boolean scaredNoCoverEnabled = true;
    public static int scaredNoCoverDurationTicks = 60;
    public static boolean lightningPanicEnabled = true;
    public static int lightningPanicRadius = 32;
    public static double mountFoodTrustMultiplier = 4.0;
    public static double calmerBreedingTrustThreshold = 0.4;
    public static int calmerBreedingLoveTicks = 1200;
    public static int hibernateRange = 160;
    public static boolean seasonalBreedingEnabled = false;
    public static int seasonLengthDays = 30;
    public static String breedingSeason = "spring";
    public static double herdTrustShareMultiplier = 0.35;
    public static double trustedPlayerFleeReduction = 0.65;

    private TamekindConfig() {
    }

    public enum Profile {
        VANILLA_PLUS,
        REALISM,
        SIMULATION;

        public static Profile fromString(String name) {
            if (name == null) return null;
            String n = name.trim().toLowerCase(java.util.Locale.ROOT).replace('-', '_');
            return switch (n) {
                case "vanilla+", "vanilla_plus", "quiet" -> VANILLA_PLUS;
                case "realism", "survival" -> REALISM;
                case "simulation", "high_simulation", "high" -> SIMULATION;
                default -> null;
            };
        }
    }

    public static void applyProfile(Profile profile) {
        activeProfile = profile.name().toLowerCase(java.util.Locale.ROOT);
        switch (profile) {
            case VANILLA_PLUS -> {
                enabled = true;
                herdEnabled = true;
                alertEnabled = true;
                panicEnabled = true;
                habitatEnabled = false;
                trustEnabled = true;
                stampedeEnabled = false;
                babyAnchoringEnabled = true;
                breedingCrowdControlEnabled = true;
                dailyRhythmEnabled = false;
                fullAiRange = 32;
                simpleAiRange = 64;
                aiLodCacheTicks = 40;
                alertRadius = 12;
                panicRadius = 6;
                herdSearchRadius = 12;
            }
            case REALISM -> {
                enabled = true;
                herdEnabled = true;
                alertEnabled = true;
                panicEnabled = true;
                habitatEnabled = true;
                trustEnabled = true;
                stampedeEnabled = true;
                babyAnchoringEnabled = true;
                breedingCrowdControlEnabled = true;
                dailyRhythmEnabled = true;
                fullAiRange = 48;
                simpleAiRange = 96;
                aiLodCacheTicks = 20;
                alertRadius = 18;
                panicRadius = 8;
                herdSearchRadius = 16;
            }
            case SIMULATION -> {
                enabled = true;
                herdEnabled = true;
                alertEnabled = true;
                panicEnabled = true;
                habitatEnabled = true;
                trustEnabled = true;
                stampedeEnabled = true;
                babyAnchoringEnabled = true;
                breedingCrowdControlEnabled = true;
                dailyRhythmEnabled = true;
                fullAiRange = 64;
                simpleAiRange = 128;
                aiLodCacheTicks = 10;
                alertRadius = 24;
                panicRadius = 10;
                herdSearchRadius = 24;
            }
        }
    }

    public static void load(Path path) {
        Properties properties = new Properties();
        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                properties.load(in);
            } catch (IOException e) {
                TamekindMod.LOGGER.warn("Failed to read {}, using defaults", path, e);
            }
        } else {
            try {
                Files.createDirectories(path.getParent());
                Files.writeString(path, toPropertiesText(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                TamekindMod.LOGGER.warn("Failed to create {}", path, e);
            }
        }

        enabled = bool(properties, "enabled", enabled);
        herdEnabled = bool(properties, "herdEnabled", herdEnabled);
        alertEnabled = bool(properties, "alertEnabled", alertEnabled);
        panicEnabled = bool(properties, "panicEnabled", panicEnabled);
        habitatEnabled = bool(properties, "habitatEnabled", habitatEnabled);
        trustEnabled = bool(properties, "trustEnabled", trustEnabled);
        stampedeEnabled = bool(properties, "stampedeEnabled", stampedeEnabled);
        babyAnchoringEnabled = bool(properties, "babyAnchoringEnabled", babyAnchoringEnabled);
        breedingCrowdControlEnabled = bool(properties, "breedingCrowdControlEnabled", breedingCrowdControlEnabled);
        breedingCrowdMessageEnabled = bool(properties, "breedingCrowdMessageEnabled", breedingCrowdMessageEnabled);
        dailyRhythmEnabled = bool(properties, "dailyRhythmEnabled", dailyRhythmEnabled);
        respectLeashedAnimals = bool(properties, "respectLeashedAnimals", respectLeashedAnimals);
        respectMountedAnimals = bool(properties, "respectMountedAnimals", respectMountedAnimals);
        respectBreedingAnimals = bool(properties, "respectBreedingAnimals", respectBreedingAnimals);
        respectNamedAnimals = bool(properties, "respectNamedAnimals", respectNamedAnimals);

        fullAiRange = integer(properties, "fullAiRange", fullAiRange);
        simpleAiRange = integer(properties, "simpleAiRange", simpleAiRange);
        aiLodCacheTicks = integer(properties, "aiLodCacheTicks", aiLodCacheTicks);
        alertRadius = integer(properties, "alertRadius", alertRadius);
        panicRadius = integer(properties, "panicRadius", panicRadius);
        herdSearchRadius = integer(properties, "herdSearchRadius", herdSearchRadius);
        shelterSearchRadius = integer(properties, "shelterSearchRadius", shelterSearchRadius);
        memoryTicks = integer(properties, "memoryTicks", memoryTicks);
        herdDangerSpreadCooldownTicks = integer(properties, "herdDangerSpreadCooldownTicks", herdDangerSpreadCooldownTicks);
        trustTicks = integer(properties, "trustTicks", trustTicks);
        minStampedeHerdSize = integer(properties, "minStampedeHerdSize", minStampedeHerdSize);
        babyAnchorSearchRadius = integer(properties, "babyAnchorSearchRadius", babyAnchorSearchRadius);
        panicCandidateCount = integer(properties, "panicCandidateCount", panicCandidateCount);
        panicEscapeDistance = integer(properties, "panicEscapeDistance", panicEscapeDistance);
        panicDropCheckDepth = integer(properties, "panicDropCheckDepth", panicDropCheckDepth);
        breedingCrowdRadius = integer(properties, "breedingCrowdRadius", breedingCrowdRadius);
        breedingCrowdHardLimit = integer(properties, "breedingCrowdHardLimit", breedingCrowdHardLimit);
        breedingCrowdSoftLimit = integer(properties, "breedingCrowdSoftLimit", breedingCrowdSoftLimit);
        grazeSearchRadius = integer(properties, "grazeSearchRadius", grazeSearchRadius);
        grazeMinIntervalTicks = integer(properties, "grazeMinIntervalTicks", grazeMinIntervalTicks);
        grazeDurationTicks = integer(properties, "grazeDurationTicks", grazeDurationTicks);
        drinkSearchRadius = integer(properties, "drinkSearchRadius", drinkSearchRadius);
        drinkMinIntervalTicks = integer(properties, "drinkMinIntervalTicks", drinkMinIntervalTicks);
        drinkDurationTicks = integer(properties, "drinkDurationTicks", drinkDurationTicks);
        parentGuardTicks = integer(properties, "parentGuardTicks", parentGuardTicks);
        parentGuardRadius = integer(properties, "parentGuardRadius", parentGuardRadius);
        parentGuardEnabled = bool(properties, "parentGuardEnabled", parentGuardEnabled);
        homeReturnEnabled = bool(properties, "homeReturnEnabled", homeReturnEnabled);
        homeReturnMaxDistance = integer(properties, "homeReturnMaxDistance", homeReturnMaxDistance);
        homeReturnMinDistance = integer(properties, "homeReturnMinDistance", homeReturnMinDistance);
        homeReturnIntervalTicks = integer(properties, "homeReturnIntervalTicks", homeReturnIntervalTicks);
        homeReturnSpeed = decimal(properties, "homeReturnSpeed", homeReturnSpeed);
        alertFreezeMinTicks = integer(properties, "alertFreezeMinTicks", alertFreezeMinTicks);
        alertFreezeRandomTicks = integer(properties, "alertFreezeRandomTicks", alertFreezeRandomTicks);
        alertDriftSpeed = decimal(properties, "alertDriftSpeed", alertDriftSpeed);

        panicSpeed = decimal(properties, "panicSpeed", panicSpeed);
        babyPanicSpeedMultiplier = decimal(properties, "babyPanicSpeedMultiplier", babyPanicSpeedMultiplier);
        babyPanicRadiusMultiplier = decimal(properties, "babyPanicRadiusMultiplier", babyPanicRadiusMultiplier);
        herdFollowSpeed = decimal(properties, "herdFollowSpeed", herdFollowSpeed);
        babyAnchorSpeed = decimal(properties, "babyAnchorSpeed", babyAnchorSpeed);
        shelterSpeed = decimal(properties, "shelterSpeed", shelterSpeed);
        stampedeKnockback = decimal(properties, "stampedeKnockback", stampedeKnockback);
        stampedeHerdScaling = decimal(properties, "stampedeHerdScaling", stampedeHerdScaling);
        trustPerFeeding = decimal(properties, "trustPerFeeding", trustPerFeeding);
        trustLossPerHit = decimal(properties, "trustLossPerHit", trustLossPerHit);
        trustHitForgivenessThreshold = decimal(properties, "trustHitForgivenessThreshold", trustHitForgivenessThreshold);
        lowHpThresholdFraction = decimal(properties, "lowHpThresholdFraction", lowHpThresholdFraction);
        limpSpeedMultiplier = decimal(properties, "limpSpeedMultiplier", limpSpeedMultiplier);
        followTrustedEnabled = bool(properties, "followTrustedEnabled", followTrustedEnabled);
        followTrustedRadius = integer(properties, "followTrustedRadius", followTrustedRadius);
        followTrustedMinTrust = decimal(properties, "followTrustedMinTrust", followTrustedMinTrust);
        followTrustedSpeed = decimal(properties, "followTrustedSpeed", followTrustedSpeed);
        stampedeCropDamageEnabled = bool(properties, "stampedeCropDamageEnabled", stampedeCropDamageEnabled);
        stampedeCropDamageChance = decimal(properties, "stampedeCropDamageChance", stampedeCropDamageChance);
        panicSoundEnabled = bool(properties, "panicSoundEnabled", panicSoundEnabled);
        panicSoundVolume = (float) decimal(properties, "panicSoundVolume", panicSoundVolume);
        idleBondTickInterval = integer(properties, "idleBondTickInterval", idleBondTickInterval);
        idleBondTrustGain = decimal(properties, "idleBondTrustGain", idleBondTrustGain);
        idleBondRadius = integer(properties, "idleBondRadius", idleBondRadius);
        sizeVarianceEnabled = bool(properties, "sizeVarianceEnabled", sizeVarianceEnabled);
        sizeVarianceRange = decimal(properties, "sizeVarianceRange", sizeVarianceRange);
        mountFoodTrustMultiplier = decimal(properties, "mountFoodTrustMultiplier", mountFoodTrustMultiplier);
        calmerBreedingTrustThreshold = decimal(properties, "calmerBreedingTrustThreshold", calmerBreedingTrustThreshold);
        calmerBreedingLoveTicks = integer(properties, "calmerBreedingLoveTicks", calmerBreedingLoveTicks);
        hibernateRange = integer(properties, "hibernateRange", hibernateRange);
        seasonalBreedingEnabled = bool(properties, "seasonalBreedingEnabled", seasonalBreedingEnabled);
        seasonLengthDays = integer(properties, "seasonLengthDays", seasonLengthDays);
        breedingSeason = properties.getProperty("breedingSeason", breedingSeason).trim().toLowerCase(java.util.Locale.ROOT);
        herdTrustShareMultiplier = decimal(properties, "herdTrustShareMultiplier", herdTrustShareMultiplier);
        trustedPlayerFleeReduction = decimal(properties, "trustedPlayerFleeReduction", trustedPlayerFleeReduction);
    }

    private static String toPropertiesText() {
        return """
                # Tamekind configuration
                # Changes take effect on world reload, /tamekind reload, or server restart.

                # ── Master switches ───────────────────────────────────────────────
                # Master kill switch. If false, no Tamekind behavior runs at all.
                enabled=%s
                # Per-feature toggles. Disable to skip that whole subsystem.
                herdEnabled=%s
                alertEnabled=%s
                panicEnabled=%s
                habitatEnabled=%s
                trustEnabled=%s
                stampedeEnabled=%s
                babyAnchoringEnabled=%s
                breedingCrowdControlEnabled=%s
                # If true, a message is shown when feeding is denied by crowd control.
                breedingCrowdMessageEnabled=%s
                # Master switch for daily rhythm (graze, rest, drink, home return).
                dailyRhythmEnabled=%s
                # Master switch for protective-parent behavior.
                parentGuardEnabled=%s
                # Master switch for home-position memory and return drift.
                homeReturnEnabled=%s

                # ── Farm respect ──────────────────────────────────────────────────
                # If true, the listed animals skip all Tamekind movement goals so
                # vanilla farms (pens, leads, breeding, name-tagged pets) stay predictable.
                respectLeashedAnimals=%s
                respectMountedAnimals=%s
                respectBreedingAnimals=%s
                respectNamedAnimals=%s

                # ── AI level-of-detail ────────────────────────────────────────────
                # Distance (blocks) from the nearest player at which animals run the full AI.
                fullAiRange=%d
                # Beyond fullAiRange but within simpleAiRange, animals run cheaper goals.
                simpleAiRange=%d
                # Cached LOD lifetime in ticks; per-entity jitter is added on top.
                aiLodCacheTicks=%d
                # Beyond this distance, animals in extreme-temperature biomes hibernate further.
                hibernateRange=%d

                # ── Threat detection ──────────────────────────────────────────────
                # Block radius for the alert/freeze scan.
                alertRadius=%d
                # Block radius for direct panic; threats within this trigger flight.
                panicRadius=%d
                # Block radius for finding herd-mates.
                herdSearchRadius=%d
                # Block radius for the shelter search.
                shelterSearchRadius=%d
                # How long (ticks) a danger memory lingers.
                memoryTicks=%d
                # Cooldown (ticks) before a herd re-broadcasts danger.
                herdDangerSpreadCooldownTicks=%d

                # ── Alert / freeze ────────────────────────────────────────────────
                # Minimum freeze duration. Babies use half; freezers use 3x and skip drift.
                alertFreezeMinTicks=%d
                # Additional random ticks added on top of the minimum.
                alertFreezeRandomTicks=%d
                # Speed during the slow drift-away half of the alert.
                alertDriftSpeed=%s

                # ── Panic ─────────────────────────────────────────────────────────
                panicSpeed=%s
                # Babies use these multipliers on panic speed and radius.
                babyPanicSpeedMultiplier=%s
                babyPanicRadiusMultiplier=%s
                # Number of escape positions scored per panic re-pick.
                panicCandidateCount=%d
                # Preferred escape distance from the threat in blocks.
                panicEscapeDistance=%d
                # Max drop depth (blocks) considered when settling an escape candidate.
                panicDropCheckDepth=%d
                # If true, panic plays a sound cue at start.
                panicSoundEnabled=%s
                panicSoundVolume=%s

                # ── Stampede ──────────────────────────────────────────────────────
                # Herd size required before panic-bumps apply.
                minStampedeHerdSize=%d
                # Base horizontal push applied to nearby pushables.
                stampedeKnockback=%s
                # Extra knockback per herd member above minStampedeHerdSize.
                stampedeHerdScaling=%s
                # If true, panicking animals can trample crops they pass over. Off by default.
                stampedeCropDamageEnabled=%s
                # Probability per tick that a crop under the foot is trampled.
                stampedeCropDamageChance=%s

                # ── Habitat / daily rhythm ────────────────────────────────────────
                shelterSpeed=%s
                grazeSearchRadius=%d
                # Minimum interval (ticks) between graze attempts.
                grazeMinIntervalTicks=%d
                # Base graze duration. Night triples, comfort biome 1.5x, SS winter halves, spring 1.3x.
                grazeDurationTicks=%d
                drinkSearchRadius=%d
                drinkMinIntervalTicks=%d
                drinkDurationTicks=%d
                # Speed when a follower drifts back home.
                homeReturnSpeed=%s
                # Min/max distance (blocks) at which home-return triggers.
                homeReturnMinDistance=%d
                homeReturnMaxDistance=%d
                # Interval (ticks) between home-return attempts.
                homeReturnIntervalTicks=%d
                # Herd-follow speed.
                herdFollowSpeed=%s

                # ── Babies & parents ──────────────────────────────────────────────
                babyAnchorSearchRadius=%d
                babyAnchorSpeed=%s
                # Ticks an adult stays in "guarding" mode after a same-type baby is hit.
                parentGuardTicks=%d
                parentGuardRadius=%d

                # ── Breeding ──────────────────────────────────────────────────────
                # Radius (blocks) sampled for crowd control around the fed animal.
                breedingCrowdRadius=%d
                # Below softLimit no refusal; between soft and hard refusal ramps linearly; at hard always refuses.
                breedingCrowdSoftLimit=%d
                breedingCrowdHardLimit=%d
                # When trust >= threshold, feeding sets love mode for this many ticks.
                calmerBreedingTrustThreshold=%s
                calmerBreedingLoveTicks=%d
                # Optional seasonal gate for automatic / unattended breeding.
                seasonalBreedingEnabled=%s
                # Length of each internal season in days (used when Serene Seasons is not present).
                seasonLengthDays=%d
                # Allowed season: spring, summer, autumn, winter, or "any".
                breedingSeason=%s

                # ── Trust ─────────────────────────────────────────────────────────
                trustTicks=%d
                # Trust added per food item fed.
                trustPerFeeding=%s
                # Trust removed per hit from the attacker.
                trustLossPerHit=%s
                # When the attacker's trust >= threshold, hits don't trigger danger memory.
                trustHitForgivenessThreshold=%s
                # Share of trustPerFeeding propagated to nearby herd-mates.
                herdTrustShareMultiplier=%s
                # How much trust reduces flee distance from a sprinting trusted player (0..1).
                trustedPlayerFleeReduction=%s
                # Passive trust gain per idle bond tick when near owner / nearest player.
                idleBondTickInterval=%d
                idleBondTrustGain=%s
                idleBondRadius=%d

                # ── Health & disposition ──────────────────────────────────────────
                # Below this fraction of max HP, animals limp and prefer shelter.
                lowHpThresholdFraction=%s
                limpSpeedMultiplier=%s

                # ── Pets & mounts ─────────────────────────────────────────────────
                # If true, trusted animals slow-follow a nearby trusted player.
                followTrustedEnabled=%s
                followTrustedRadius=%d
                followTrustedMinTrust=%s
                followTrustedSpeed=%s
                # Mount feeding multiplies the trust duration by this factor.
                mountFoodTrustMultiplier=%s

                # ── Visual ────────────────────────────────────────────────────────
                # If true, each animal spawns with a small deterministic size variation.
                sizeVarianceEnabled=%s
                # Maximum +/- scale variance (e.g. 0.10 = 90%%..110%%).
                sizeVarianceRange=%s
                """.formatted(
                        enabled, herdEnabled, alertEnabled, panicEnabled, habitatEnabled, trustEnabled,
                        stampedeEnabled, babyAnchoringEnabled, breedingCrowdControlEnabled,
                        breedingCrowdMessageEnabled, dailyRhythmEnabled, parentGuardEnabled, homeReturnEnabled,
                        respectLeashedAnimals, respectMountedAnimals, respectBreedingAnimals, respectNamedAnimals,
                        fullAiRange, simpleAiRange, aiLodCacheTicks, hibernateRange,
                        alertRadius, panicRadius, herdSearchRadius, shelterSearchRadius,
                        memoryTicks, herdDangerSpreadCooldownTicks,
                        alertFreezeMinTicks, alertFreezeRandomTicks, Double.toString(alertDriftSpeed),
                        Double.toString(panicSpeed), Double.toString(babyPanicSpeedMultiplier),
                        Double.toString(babyPanicRadiusMultiplier),
                        panicCandidateCount, panicEscapeDistance, panicDropCheckDepth,
                        panicSoundEnabled, Float.toString(panicSoundVolume),
                        minStampedeHerdSize, Double.toString(stampedeKnockback),
                        Double.toString(stampedeHerdScaling),
                        stampedeCropDamageEnabled, Double.toString(stampedeCropDamageChance),
                        Double.toString(shelterSpeed),
                        grazeSearchRadius, grazeMinIntervalTicks, grazeDurationTicks,
                        drinkSearchRadius, drinkMinIntervalTicks, drinkDurationTicks,
                        Double.toString(homeReturnSpeed), homeReturnMinDistance, homeReturnMaxDistance,
                        homeReturnIntervalTicks, Double.toString(herdFollowSpeed),
                        babyAnchorSearchRadius, Double.toString(babyAnchorSpeed),
                        parentGuardTicks, parentGuardRadius,
                        breedingCrowdRadius, breedingCrowdSoftLimit, breedingCrowdHardLimit,
                        Double.toString(calmerBreedingTrustThreshold), calmerBreedingLoveTicks,
                        seasonalBreedingEnabled, seasonLengthDays, breedingSeason,
                        trustTicks, Double.toString(trustPerFeeding), Double.toString(trustLossPerHit),
                        Double.toString(trustHitForgivenessThreshold),
                        Double.toString(herdTrustShareMultiplier),
                        Double.toString(trustedPlayerFleeReduction),
                        idleBondTickInterval, Double.toString(idleBondTrustGain), idleBondRadius,
                        Double.toString(lowHpThresholdFraction), Double.toString(limpSpeedMultiplier),
                        followTrustedEnabled, followTrustedRadius,
                        Double.toString(followTrustedMinTrust), Double.toString(followTrustedSpeed),
                        Double.toString(mountFoodTrustMultiplier),
                        sizeVarianceEnabled, Double.toString(sizeVarianceRange));
    }

    private static boolean bool(Properties properties, String key, boolean fallback) {
        return Boolean.parseBoolean(properties.getProperty(key, Boolean.toString(fallback)));
    }

    private static int integer(Properties properties, String key, int fallback) {
        try {
            return Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)).trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static double decimal(Properties properties, String key, double fallback) {
        try {
            return Double.parseDouble(properties.getProperty(key, Double.toString(fallback)).trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
