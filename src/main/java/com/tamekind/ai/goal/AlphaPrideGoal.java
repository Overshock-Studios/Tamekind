package com.tamekind.ai.goal;

import com.tamekind.TamekindMod;
import com.tamekind.ai.HerdCoordinator;
import com.tamekind.config.TamekindConfig;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;

import java.util.EnumSet;

/**
 * Makes the herd alpha visibly larger.
 *
 * <p>The bonus is deliberately sticky. Election runs over whoever is inside
 * {@code herdSearchRadius} right now, so a herd that is grazing across a field keeps
 * changing who the lowest-UUID member in range is, and a naive check flipped the alpha
 * every two seconds. Because this modifier scales the entity, that flip resized the
 * animal on the same cadence, which is what "the mobs randomly change size" looked
 * like. It also changes the hitbox, so an animal in a tight pen could be pushed into a
 * block and suffocate.
 *
 * <p>The bonus therefore applies immediately but is only withdrawn after several
 * consecutive checks agree the animal is no longer alpha, which turns a 2 second flicker
 * into a deliberate handover.
 */
public final class AlphaPrideGoal extends Goal implements TamekindGoal {
    private static final Identifier MODIFIER_ID =
            Identifier.fromNamespaceAndPath("tamekind", "alpha_pride");
    private static final int CHECK_INTERVAL = 40;
    /** Consecutive non-alpha checks before the bonus is dropped. 5 checks is about 10s. */
    private static final int DEMOTION_GRACE = 5;

    private final Animal animal;
    private boolean applied;
    private int nextCheckTick;
    private int demotions;

    public AlphaPrideGoal(Animal animal) {
        this.animal = animal;
        setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (!TamekindConfig.enabled || TamekindConfig.alphaScaleBonus <= 0) {
            removeModifier("disabled");
            return false;
        }
        if (animal.tickCount < nextCheckTick) return false;
        nextCheckTick = animal.tickCount + CHECK_INTERVAL;

        boolean alpha = HerdCoordinator.leaderFor(animal) == animal
                && HerdCoordinator.herdSize(animal) >= TamekindConfig.alphaMinHerdSize;
        if (alpha) {
            demotions = 0;
            applyModifier();
        } else if (applied && ++demotions >= DEMOTION_GRACE) {
            removeModifier("demoted");
        }
        return false;
    }

    private void applyModifier() {
        if (applied) return;
        AttributeInstance attr = animal.getAttribute(Attributes.SCALE);
        if (attr == null) return;
        attr.addOrUpdateTransientModifier(new AttributeModifier(
                MODIFIER_ID, TamekindConfig.alphaScaleBonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        applied = true;
        logScale("alpha bonus applied");
    }

    private void removeModifier(String why) {
        if (!applied) return;
        AttributeInstance attr = animal.getAttribute(Attributes.SCALE);
        if (attr != null) attr.removeModifier(MODIFIER_ID);
        applied = false;
        demotions = 0;
        logScale("alpha bonus removed (" + why + ")");
    }

    private void logScale(String what) {
        if (!TamekindConfig.debugLogs) return;
        AttributeInstance attr = animal.getAttribute(Attributes.SCALE);
        TamekindMod.LOGGER.info("[Tamekind] {} {}#{} base={} final={}",
                what, animal.getType().toShortString(), animal.getId(),
                attr == null ? "?" : String.format("%.3f", attr.getBaseValue()),
                attr == null ? "?" : String.format("%.3f", attr.getValue()));
    }
}
