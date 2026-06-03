package com.tamekind.ai.goal;

import com.tamekind.config.TamekindConfig;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;

import java.util.EnumSet;

public final class AgeScaleGoal extends Goal implements TamekindGoal {
    private static final Identifier MODIFIER_ID =
            Identifier.fromNamespaceAndPath("tamekind", "age_scale");
    private static final int BABY_START_AGE = -24000;
    private final Animal animal;
    private int nextCheckTick;
    private double appliedFactor = Double.NaN;

    public AgeScaleGoal(Animal animal) {
        this.animal = animal;
        setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (!TamekindConfig.enabled || !TamekindConfig.ageScalingEnabled) {
            removeModifier();
            return false;
        }
        if (animal.tickCount < nextCheckTick) return false;
        nextCheckTick = animal.tickCount + 20;
        AttributeInstance attr = animal.getAttribute(Attributes.SCALE);
        if (attr == null) return false;
        if (!animal.isBaby()) {
            removeModifier();
            return false;
        }
        int age = animal.getAge();
        double progress = age <= BABY_START_AGE ? 0.0
                : age >= 0 ? 1.0
                : (age - BABY_START_AGE) / (double) -BABY_START_AGE;
        double start = TamekindConfig.babyStartScaleMultiplier;
        double factor = start + (1.0 - start) * progress;
        double offset = factor - 1.0;
        if (Math.abs(offset - appliedFactor) < 0.01) return false;
        attr.removeModifier(MODIFIER_ID);
        if (Math.abs(offset) > 0.001) {
            attr.addOrUpdateTransientModifier(new AttributeModifier(
                    MODIFIER_ID, offset, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        appliedFactor = offset;
        return false;
    }

    private void removeModifier() {
        if (Double.isNaN(appliedFactor)) return;
        AttributeInstance attr = animal.getAttribute(Attributes.SCALE);
        if (attr != null) attr.removeModifier(MODIFIER_ID);
        appliedFactor = Double.NaN;
    }
}
