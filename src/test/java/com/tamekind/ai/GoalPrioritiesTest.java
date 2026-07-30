package com.tamekind.ai;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the goal priority map.
 *
 * <p>Tamekind injects into the selector vanilla already owns, so a priority is a claim
 * against vanilla's table. These tests encode that table — read out of the 26.1.2
 * bytecode — and fail if a new goal lands somewhere that would make behaviour a coin
 * flip. They cannot see other mods' goals; {@code /tamekind goals} covers that at
 * runtime.
 */
class GoalPrioritiesTest {

    /** Goal flags, mirrored so this stays a pure unit test. */
    private enum F { MOVE, LOOK, JUMP }

    /** Vanilla's animal goal table, verified against Sheep in 26.1.2. */
    private static final Map<Integer, Map<String, EnumSet<F>>> VANILLA = new LinkedHashMap<>();

    static {
        vanilla(0, "FloatGoal", EnumSet.of(F.JUMP));
        vanilla(1, "PanicGoal", EnumSet.of(F.MOVE));
        vanilla(2, "BreedGoal", EnumSet.of(F.MOVE, F.LOOK));
        vanilla(3, "TemptGoal", EnumSet.of(F.MOVE, F.LOOK));
        vanilla(4, "FollowParentGoal", EnumSet.noneOf(F.class));
        vanilla(5, "EatBlockGoal", EnumSet.of(F.MOVE, F.LOOK, F.JUMP));
        vanilla(6, "WaterAvoidingRandomStrollGoal", EnumSet.of(F.MOVE));
        vanilla(7, "LookAtPlayerGoal", EnumSet.of(F.LOOK));
        vanilla(8, "RandomLookAroundGoal", EnumSet.of(F.MOVE, F.LOOK));
    }

    private static void vanilla(int priority, String name, EnumSet<F> flags) {
        VANILLA.computeIfAbsent(priority, k -> new LinkedHashMap<>()).put(name, flags);
    }

    /** Tamekind's movement-holding goals and the flags they declare. */
    private static Map<String, EnumSet<F>> tamekindMovementGoals() {
        Map<String, EnumSet<F>> m = new LinkedHashMap<>();
        m.put("BABY_ANCHOR", EnumSet.of(F.MOVE, F.LOOK));
        m.put("PANIC", EnumSet.of(F.MOVE));
        m.put("ALERT_FREEZE", EnumSet.of(F.MOVE, F.LOOK));
        m.put("LOST_BABY", EnumSet.of(F.MOVE, F.LOOK));
        m.put("HERD_FOLLOW", EnumSet.of(F.MOVE));
        m.put("SHELTER", EnumSet.of(F.MOVE));
        m.put("SENTINEL", EnumSet.of(F.MOVE, F.LOOK));
        m.put("GRAZE", EnumSet.of(F.MOVE, F.LOOK));
        m.put("DRINK", EnumSet.of(F.MOVE, F.LOOK));
        m.put("HOME_RETURN", EnumSet.of(F.MOVE));
        m.put("MOTHER_BOND", EnumSet.of(F.MOVE, F.LOOK));
        m.put("FOLLOW_TRUSTED", EnumSet.of(F.MOVE));
        m.put("MATING_DISPLAY", EnumSet.of(F.MOVE, F.LOOK));
        m.put("WALLOW", EnumSet.of(F.MOVE, F.LOOK));
        m.put("PET_DANGER_RELAY", EnumSet.of(F.LOOK));
        return m;
    }

    private static final List<String> FLAGLESS = List.of(
            "PET_IDLE_BOND", "MOUNT_OBEDIENCE", "ALPHA_PRIDE", "AGE_SCALE", "HERD_TRAIL", "CONDITION");

    private static int priority(String constant) {
        try {
            Field f = GoalPriorities.class.getField(constant);
            return f.getInt(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("GoalPriorities." + constant + " is missing", e);
        }
    }

    @Test
    void noTwoTamekindGoalsShareAPriority() {
        Map<Integer, String> seen = new HashMap<>();
        List<String> all = new ArrayList<>(tamekindMovementGoals().keySet());
        all.addAll(FLAGLESS);
        for (String name : all) {
            int p = priority(name);
            String clash = seen.put(p, name);
            assertTrue(clash == null,
                    "priority " + p + " claimed by both " + clash + " and " + name);
        }
    }

    @Test
    void nothingClaimsAPriorityAboveVanillaFloat() {
        // FloatGoal at 0 is the drowning guard. Nothing may sit above it, or an animal
        // could be held out of a swim by a goal FloatGoal cannot preempt.
        for (String name : tamekindMovementGoals().keySet()) {
            assertTrue(priority(name) >= GoalPriorities.VANILLA_FLOAT,
                    name + " sits above vanilla FloatGoal");
        }
    }

    @Test
    void sharingPriorityZeroWithFloatGoalIsFlagSafe() {
        // BabyAnchorGoal deliberately shares 0 with FloatGoal. That is only safe because
        // FloatGoal declares JUMP alone. If this ever fails, FloatGoal gained a flag and
        // BABY_ANCHOR must move.
        EnumSet<F> floatFlags = VANILLA.get(0).get("FloatGoal");
        assertEquals(EnumSet.of(F.JUMP), floatFlags,
                "FloatGoal's flags changed; re-check GoalPriorities.BABY_ANCHOR");
        EnumSet<F> babyAnchor = EnumSet.copyOf(tamekindMovementGoals().get("BABY_ANCHOR"));
        babyAnchor.retainAll(floatFlags);
        assertTrue(babyAnchor.isEmpty(),
                "BabyAnchorGoal shares a flag with FloatGoal at priority 0");
    }

    @Test
    void herdFollowReliablyBeatsVanillaWander() {
        // The whole point of moving it to the free slot 4: at 6 it tied random stroll and
        // won a coin flip instead of leading the herd.
        assertTrue(priority("HERD_FOLLOW") < GoalPriorities.VANILLA_STROLL,
                "herd following must outrank vanilla's random stroll to be reliable");
    }

    @Test
    void herdFollowUsesTheOnlyGapInVanillasTable() {
        // Priority 4 is free because vanilla's FollowParentGoal declares no flags.
        assertEquals(4, priority("HERD_FOLLOW"));
        assertTrue(VANILLA.get(4).get("FollowParentGoal").isEmpty(),
                "FollowParentGoal gained flags; priority 4 is no longer free");
    }

    @Test
    void urgentReactionsOutrankRoutine() {
        int panic = priority("PANIC");
        for (String routine : List.of("GRAZE", "DRINK", "HOME_RETURN", "MATING_DISPLAY", "WALLOW")) {
            assertTrue(panic < priority(routine), "panic must outrank " + routine);
        }
        assertTrue(priority("SENTINEL") < priority("GRAZE"),
                "the lookout must not put its head down to graze");
    }

    @Test
    void flaglessGoalsAreParkedClearOfTheMovementBand() {
        int highestMovement = tamekindMovementGoals().keySet().stream()
                .mapToInt(GoalPrioritiesTest::priority).max().orElseThrow();
        for (String name : FLAGLESS) {
            assertTrue(priority(name) > highestMovement,
                    name + " is a flagless ticker and belongs above the movement band");
        }
    }

    /**
     * Records the collisions that exist and are accepted. Vanilla occupies 0-8
     * contiguously, so a mod adding this many movement goals cannot avoid every tie
     * without displacing vanilla behaviour. Each entry here is a deliberate decision; a
     * new one appearing means someone claimed a priority without thinking it through.
     */
    @Test
    void onlyTheKnownAcceptedCollisionsExist() {
        Map<String, EnumSet<F>> mine = tamekindMovementGoals();
        List<String> found = new ArrayList<>();
        for (Map.Entry<String, EnumSet<F>> entry : mine.entrySet()) {
            Map<String, EnumSet<F>> atSamePriority = VANILLA.get(priority(entry.getKey()));
            if (atSamePriority == null) continue;
            for (Map.Entry<String, EnumSet<F>> v : atSamePriority.entrySet()) {
                EnumSet<F> shared = EnumSet.copyOf(entry.getValue());
                shared.retainAll(v.getValue());
                if (!shared.isEmpty()) found.add(entry.getKey() + " vs " + v.getKey());
            }
        }
        List<String> accepted = List.of(
                // Same intent: whichever holds MOVE, the animal flees.
                "PANIC vs PanicGoal",
                // Mutually exclusive: BreedGoal needs isInLove, and skipMovementGoals
                // makes AlertFreezeGoal yield on exactly that.
                "ALERT_FREEZE vs BreedGoal",
                // Real and unresolved: leading a baby with wheat vs it running to its
                // parent. Tracked in docs/TESTING.md.
                "LOST_BABY vs TemptGoal",
                // Sheep only, and EatBlockGoal releases quickly.
                "SHELTER vs EatBlockGoal",
                // LOOK only, so the sentinel still holds MOVE and keeps its post.
                "SENTINEL vs LookAtPlayerGoal",
                "GRAZE vs RandomLookAroundGoal");
        assertEquals(accepted, found,
                "the set of accepted vanilla collisions changed - justify or re-slot");
    }

    @Test
    void everyPriorityConstantIsAPublicStaticFinalInt() {
        for (Field f : GoalPriorities.class.getFields()) {
            assertTrue(Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers()),
                    f.getName() + " must be static final");
            assertEquals(int.class, f.getType(), f.getName() + " must be an int");
        }
        assertFalse(GoalPriorities.class.getFields().length == 0, "no priorities declared");
    }
}
