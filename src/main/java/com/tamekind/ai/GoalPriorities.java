package com.tamekind.ai;

/**
 * The one place Tamekind's goal priorities are decided, and the reasoning for them.
 *
 * <p>Tamekind injects into the *same* {@code goalSelector} vanilla already populated,
 * so priorities are not free choices — they are claims against a table vanilla owns.
 * The vanilla animal layout below was read out of the 26.1.2 bytecode, not recalled:
 *
 * <pre>
 *   0  FloatGoal                      JUMP              &lt;- no MOVE: safe to share
 *   1  PanicGoal                      MOVE
 *   2  BreedGoal                      MOVE LOOK
 *   3  TemptGoal                      MOVE LOOK
 *   4  FollowParentGoal               (declares none)   &lt;- effectively a free slot
 *   5  EatBlockGoal                   MOVE LOOK JUMP    (sheep)
 *   6  WaterAvoidingRandomStrollGoal  MOVE
 *   7  LookAtPlayerGoal               LOOK
 *   8  RandomLookAroundGoal           MOVE LOOK
 * </pre>
 *
 * <p>Two rules follow from how {@code GoalSelector} resolves contention. A goal may
 * only take a conflicting flag from a goal of <em>strictly lower</em> priority
 * ({@code WrappedGoal.canBeReplacedBy} compares {@code <}), so:
 *
 * <ol>
 *   <li><b>Equal priority plus a shared flag is a coin flip.</b> Whichever goal starts
 *       first keeps the flag until it releases it. That is non-determinism, not
 *       precedence, and it is the bug class that dominated Warband's 1.4.0 fixes.</li>
 *   <li><b>To reliably beat a vanilla goal, sit at a strictly lower number.</b> Sitting
 *       at the same number is not enough, and sitting higher means never winning.</li>
 * </ol>
 *
 * <p>Vanilla occupies 0-8 contiguously, so a mod adding this many movement goals cannot
 * avoid every tie without displacing vanilla behaviour. Tamekind does not displace
 * vanilla, so some ties are accepted deliberately — but only where the two goals are
 * same-intent or mutually exclusive. Those are marked below. Use
 * {@code /tamekind goals} in game to see the live table, including other mods' goals.
 */
public final class GoalPriorities {

    private GoalPriorities() {
    }

    /** Vanilla's highest-priority animal goal. Nothing may claim a MOVE goal above it. */
    public static final int VANILLA_FLOAT = 0;
    /** Vanilla's random wander. Anything that must reliably move an animal beats this. */
    public static final int VANILLA_STROLL = 6;

    // ── Movement band ─────────────────────────────────────────────────────────

    /**
     * Shares 0 with {@code FloatGoal}, which is safe: FloatGoal declares only JUMP, so
     * there is no shared flag and both run at once. Verified against the 26.1.2 jar; if
     * a future version gives FloatGoal MOVE, this must move and a baby could otherwise
     * be held out of a swim.
     */
    public static final int BABY_ANCHOR = 0;

    /** Ties vanilla {@code PanicGoal}. Accepted: identical intent, either one flees. */
    public static final int PANIC = 1;

    /**
     * Ties vanilla {@code BreedGoal}. Accepted: mutually exclusive in practice, since
     * BreedGoal requires {@code isInLove} and {@code skipMovementGoals} yields on it.
     */
    public static final int ALERT_FREEZE = 2;

    /** Ties vanilla {@code TemptGoal}. A known real tie; see docs/TESTING.md. */
    public static final int LOST_BABY = 3;

    /**
     * The only genuinely free slot in vanilla's table, spent on the headline continuous
     * movement behaviour. At 4 this reliably beats {@code EatBlockGoal} (5) and
     * {@code WaterAvoidingRandomStrollGoal} (6) instead of coin-flipping against the
     * latter, which is what made herd following intermittent.
     */
    public static final int HERD_FOLLOW = 4;

    /** Ties vanilla {@code EatBlockGoal} on sheep only, and EatBlock is brief. */
    public static final int SHELTER = 5;

    public static final int SENTINEL = 7;
    public static final int GRAZE = 8;
    public static final int DRINK = 9;
    public static final int HOME_RETURN = 10;
    public static final int MOTHER_BOND = 11;
    public static final int FOLLOW_TRUSTED = 12;
    public static final int MATING_DISPLAY = 13;
    public static final int WALLOW = 14;
    public static final int PET_DANGER_RELAY = 15;

    // ── Flagless bookkeeping band ─────────────────────────────────────────────
    // These declare no goal flags, so they contend with nothing and their numbers are
    // arbitrary. Parked well clear of the movement band so the two never get confused.

    public static final int PET_IDLE_BOND = 30;
    public static final int MOUNT_OBEDIENCE = 31;
    public static final int ALPHA_PRIDE = 32;
    public static final int AGE_SCALE = 33;
    public static final int HERD_TRAIL = 34;
    public static final int CONDITION = 35;
}
