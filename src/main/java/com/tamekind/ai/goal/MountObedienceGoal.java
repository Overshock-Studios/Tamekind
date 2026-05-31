package com.tamekind.ai.goal;

import com.tamekind.ai.AnimalMemoryStore;
import com.tamekind.config.TamekindConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public final class MountObedienceGoal extends Goal implements TamekindGoal {
    private static final Identifier MODIFIER_ID =
            Identifier.fromNamespaceAndPath("tamekind", "mount_obedience");
    private final Animal mount;
    private boolean modifierApplied;

    public MountObedienceGoal(Animal mount) {
        this.mount = mount;
        setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (!TamekindConfig.enabled || !TamekindConfig.trustEnabled) return false;
        Entity passenger = mount.getControllingPassenger();
        if (!(passenger instanceof Player player)) {
            removeModifier();
            return false;
        }
        double trust = AnimalMemoryStore.get(mount).trustScore(player.getUUID(), mount.level().getGameTime());
        if (trust < TamekindConfig.calmerBreedingTrustThreshold) {
            removeModifier();
            return false;
        }
        applyModifier(trust);
        return false;
    }

    private void applyModifier(double trust) {
        AttributeInstance attr = mount.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr == null) return;
        if (modifierApplied) return;
        double bonus = 0.15 * trust;
        attr.addOrUpdateTransientModifier(new AttributeModifier(
                MODIFIER_ID, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        modifierApplied = true;
    }

    private void removeModifier() {
        if (!modifierApplied) return;
        AttributeInstance attr = mount.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr != null) attr.removeModifier(MODIFIER_ID);
        modifierApplied = false;
    }
}
