package com.tamekind.ai;

import com.tamekind.ai.goal.TamekindGoal;
import com.tamekind.mixin.MobGoalSelectorAccessor;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.animal.Animal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Reports what is actually in an animal's goal selector at runtime.
 *
 * <p>The static table in {@link GoalPriorities} records what Tamekind *intends*. This
 * reads what is really there — including vanilla's goals and any other mod's — and
 * finds contention that no compile-time check can see, because flags and priorities of
 * third-party goals are only knowable in a live world.
 *
 * <p>A conflict is two goals at the <em>same</em> priority sharing at least one flag.
 * {@code GoalSelector} cannot break that tie (a flag only transfers from a strictly
 * lower priority), so whichever starts first keeps the flag and the outcome is a coin
 * flip rather than a decision.
 */
public final class GoalDiagnostics {

    private GoalDiagnostics() {
    }

    /** One line per goal in the selector, ordered by priority. */
    public record Entry(int priority, String name, boolean tamekind, boolean running, String flags) {
    }

    /** Two goals that will contend non-deterministically. */
    public record Conflict(int priority, String first, String second, String sharedFlags) {
    }

    public static List<Entry> inspect(Animal animal) {
        List<Entry> out = new ArrayList<>();
        for (WrappedGoal wrapped : goals(animal)) {
            Goal inner = wrapped.getGoal();
            out.add(new Entry(
                    wrapped.getPriority(),
                    inner.getClass().getSimpleName(),
                    inner instanceof TamekindGoal,
                    wrapped.isRunning(),
                    flagString(wrapped.getFlags())));
        }
        out.sort(Comparator.comparingInt(Entry::priority).thenComparing(Entry::name));
        return out;
    }

    /**
     * Finds every same-priority flag collision. Reported regardless of which mod owns
     * the goals: a tie between two vanilla goals is just as non-deterministic, and
     * knowing that keeps blame honest.
     */
    public static List<Conflict> conflicts(Animal animal) {
        Map<Integer, List<WrappedGoal>> byPriority = new TreeMap<>();
        for (WrappedGoal wrapped : goals(animal)) {
            byPriority.computeIfAbsent(wrapped.getPriority(), ignored -> new ArrayList<>()).add(wrapped);
        }

        List<Conflict> out = new ArrayList<>();
        for (Map.Entry<Integer, List<WrappedGoal>> slot : byPriority.entrySet()) {
            List<WrappedGoal> sharing = slot.getValue();
            for (int i = 0; i < sharing.size(); i++) {
                for (int j = i + 1; j < sharing.size(); j++) {
                    EnumSet<Goal.Flag> shared = EnumSet.copyOf(sharing.get(i).getFlags());
                    shared.retainAll(sharing.get(j).getFlags());
                    if (shared.isEmpty()) continue;
                    out.add(new Conflict(
                            slot.getKey(),
                            sharing.get(i).getGoal().getClass().getSimpleName(),
                            sharing.get(j).getGoal().getClass().getSimpleName(),
                            flagString(shared)));
                }
            }
        }
        return out;
    }

    /**
     * Movement goals that outrank Tamekind's, i.e. sit at a strictly lower priority
     * while holding MOVE. These win every time, so a Tamekind behaviour that "never
     * happens" is usually explained here rather than by its own {@code canUse}.
     */
    public static List<Entry> outranking(Animal animal, int tamekindPriority) {
        List<Entry> out = new ArrayList<>();
        for (WrappedGoal wrapped : goals(animal)) {
            if (wrapped.getPriority() >= tamekindPriority) continue;
            if (!wrapped.getFlags().contains(Goal.Flag.MOVE)) continue;
            if (wrapped.getGoal() instanceof TamekindGoal) continue;
            out.add(new Entry(wrapped.getPriority(), wrapped.getGoal().getClass().getSimpleName(),
                    false, wrapped.isRunning(), flagString(wrapped.getFlags())));
        }
        out.sort(Comparator.comparingInt(Entry::priority));
        return out;
    }

    private static Iterable<WrappedGoal> goals(Animal animal) {
        return ((MobGoalSelectorAccessor) animal).tamekind$goalSelector().getAvailableGoals();
    }

    private static String flagString(EnumSet<Goal.Flag> flags) {
        if (flags.isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        for (Goal.Flag f : flags) {
            if (!sb.isEmpty()) sb.append('+');
            sb.append(f.name());
        }
        return sb.toString();
    }
}
