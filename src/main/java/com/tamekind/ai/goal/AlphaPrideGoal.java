package com.tamekind.ai.goal;

import com.tamekind.ai.HerdCoordinator;
import com.tamekind.config.TamekindConfig;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;

import java.util.EnumSet;

public final class AlphaPrideGoal extends Goal implements TamekindGoal {
    private static final Identifier MODIFIER_ID =
            Identifier.fromNamespaceAndPath("tamekind", "alpha_pride");
    private final Animal animal;
    private boolean applied;
    private int nextCheckTick;

    public AlphaPrideGoal(Animal animal) {
        this.animal = animal;
        setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (!TamekindConfig.enabled || TamekindConfig.alphaScaleBonus <= 0) {
            removeModifier();
            return false;
        }
        if (animal.tickCount < nextCheckTick) return false;
        nextCheckTick = animal.tickCount + 40;
        Animal leader = HerdCoordinator.leaderFor(animal);
        int herdSize = HerdCoordinator.herdSize(animal);
        if (leader == animal && herdSize >= TamekindConfig.alphaMinHerdSize) {
            applyModifier();
        } else {
            removeModifier();
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
    }

    private void removeModifier() {
        if (!applied) return;
        AttributeInstance attr = animal.getAttribute(Attributes.SCALE);
        if (attr != null) attr.removeModifier(MODIFIER_ID);
        applied = false;
    }
}
