package com.wildsense.config;

import com.wildsense.WildsenseMod;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class WildsenseConfig {
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
    public static int grazeSearchRadius = 5;
    public static int grazeMinIntervalTicks = 20 * 12;
    public static int grazeDurationTicks = 20 * 4;

    public static double panicSpeed = 1.35;
    public static double herdFollowSpeed = 1.05;
    public static double babyAnchorSpeed = 1.2;
    public static double shelterSpeed = 1.0;
    public static double stampedeKnockback = 0.35;
    public static double stampedeHerdScaling = 0.08;
    public static double trustPerFeeding = 0.22;
    public static double herdTrustShareMultiplier = 0.35;
    public static double trustedPlayerFleeReduction = 0.65;

    private WildsenseConfig() {
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
        Properties properties = defaults();
        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                properties.load(in);
            } catch (IOException e) {
                WildsenseMod.LOGGER.warn("Failed to read {}, using defaults", path, e);
            }
        } else {
            try {
                Files.createDirectories(path.getParent());
                try (OutputStream out = Files.newOutputStream(path)) {
                    properties.store(out, "Wildsense configuration");
                }
            } catch (IOException e) {
                WildsenseMod.LOGGER.warn("Failed to create {}", path, e);
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
        grazeSearchRadius = integer(properties, "grazeSearchRadius", grazeSearchRadius);
        grazeMinIntervalTicks = integer(properties, "grazeMinIntervalTicks", grazeMinIntervalTicks);
        grazeDurationTicks = integer(properties, "grazeDurationTicks", grazeDurationTicks);

        panicSpeed = decimal(properties, "panicSpeed", panicSpeed);
        herdFollowSpeed = decimal(properties, "herdFollowSpeed", herdFollowSpeed);
        babyAnchorSpeed = decimal(properties, "babyAnchorSpeed", babyAnchorSpeed);
        shelterSpeed = decimal(properties, "shelterSpeed", shelterSpeed);
        stampedeKnockback = decimal(properties, "stampedeKnockback", stampedeKnockback);
        stampedeHerdScaling = decimal(properties, "stampedeHerdScaling", stampedeHerdScaling);
        trustPerFeeding = decimal(properties, "trustPerFeeding", trustPerFeeding);
        herdTrustShareMultiplier = decimal(properties, "herdTrustShareMultiplier", herdTrustShareMultiplier);
        trustedPlayerFleeReduction = decimal(properties, "trustedPlayerFleeReduction", trustedPlayerFleeReduction);
    }

    private static Properties defaults() {
        Properties properties = new Properties();
        properties.setProperty("enabled", Boolean.toString(enabled));
        properties.setProperty("herdEnabled", Boolean.toString(herdEnabled));
        properties.setProperty("alertEnabled", Boolean.toString(alertEnabled));
        properties.setProperty("panicEnabled", Boolean.toString(panicEnabled));
        properties.setProperty("habitatEnabled", Boolean.toString(habitatEnabled));
        properties.setProperty("trustEnabled", Boolean.toString(trustEnabled));
        properties.setProperty("stampedeEnabled", Boolean.toString(stampedeEnabled));
        properties.setProperty("babyAnchoringEnabled", Boolean.toString(babyAnchoringEnabled));
        properties.setProperty("breedingCrowdControlEnabled", Boolean.toString(breedingCrowdControlEnabled));
        properties.setProperty("breedingCrowdMessageEnabled", Boolean.toString(breedingCrowdMessageEnabled));
        properties.setProperty("dailyRhythmEnabled", Boolean.toString(dailyRhythmEnabled));
        properties.setProperty("respectLeashedAnimals", Boolean.toString(respectLeashedAnimals));
        properties.setProperty("respectMountedAnimals", Boolean.toString(respectMountedAnimals));
        properties.setProperty("respectBreedingAnimals", Boolean.toString(respectBreedingAnimals));
        properties.setProperty("fullAiRange", Integer.toString(fullAiRange));
        properties.setProperty("simpleAiRange", Integer.toString(simpleAiRange));
        properties.setProperty("aiLodCacheTicks", Integer.toString(aiLodCacheTicks));
        properties.setProperty("alertRadius", Integer.toString(alertRadius));
        properties.setProperty("panicRadius", Integer.toString(panicRadius));
        properties.setProperty("herdSearchRadius", Integer.toString(herdSearchRadius));
        properties.setProperty("shelterSearchRadius", Integer.toString(shelterSearchRadius));
        properties.setProperty("memoryTicks", Integer.toString(memoryTicks));
        properties.setProperty("herdDangerSpreadCooldownTicks", Integer.toString(herdDangerSpreadCooldownTicks));
        properties.setProperty("trustTicks", Integer.toString(trustTicks));
        properties.setProperty("minStampedeHerdSize", Integer.toString(minStampedeHerdSize));
        properties.setProperty("babyAnchorSearchRadius", Integer.toString(babyAnchorSearchRadius));
        properties.setProperty("panicCandidateCount", Integer.toString(panicCandidateCount));
        properties.setProperty("panicEscapeDistance", Integer.toString(panicEscapeDistance));
        properties.setProperty("panicDropCheckDepth", Integer.toString(panicDropCheckDepth));
        properties.setProperty("breedingCrowdRadius", Integer.toString(breedingCrowdRadius));
        properties.setProperty("breedingCrowdHardLimit", Integer.toString(breedingCrowdHardLimit));
        properties.setProperty("grazeSearchRadius", Integer.toString(grazeSearchRadius));
        properties.setProperty("grazeMinIntervalTicks", Integer.toString(grazeMinIntervalTicks));
        properties.setProperty("grazeDurationTicks", Integer.toString(grazeDurationTicks));
        properties.setProperty("panicSpeed", Double.toString(panicSpeed));
        properties.setProperty("herdFollowSpeed", Double.toString(herdFollowSpeed));
        properties.setProperty("babyAnchorSpeed", Double.toString(babyAnchorSpeed));
        properties.setProperty("shelterSpeed", Double.toString(shelterSpeed));
        properties.setProperty("stampedeKnockback", Double.toString(stampedeKnockback));
        properties.setProperty("stampedeHerdScaling", Double.toString(stampedeHerdScaling));
        properties.setProperty("trustPerFeeding", Double.toString(trustPerFeeding));
        properties.setProperty("herdTrustShareMultiplier", Double.toString(herdTrustShareMultiplier));
        properties.setProperty("trustedPlayerFleeReduction", Double.toString(trustedPlayerFleeReduction));
        return properties;
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
