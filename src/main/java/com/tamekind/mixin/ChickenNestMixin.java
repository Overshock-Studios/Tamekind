package com.tamekind.mixin;

import com.tamekind.compat.TamekindTags;
import com.tamekind.config.TamekindConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.animal.chicken.Chicken;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Chicken.class)
public abstract class ChickenNestMixin {

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void tamekind$gateEgg(CallbackInfo ci) {
        if (!TamekindConfig.enabled) return;
        Chicken self = (Chicken) (Object) this;
        if (self.level().isClientSide() || self.isBaby() || self.isChickenJockey) return;
        if (self.eggTime > 1) return;
        BlockPos pos = self.blockPosition();
        if (self.level().getBlockState(pos.below()).is(TamekindTags.NEST_BLOCKS)) return;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (self.level().getBlockState(pos.relative(d)).is(TamekindTags.NEST_BLOCKS)) return;
        }
        self.eggTime = 200 + self.getRandom().nextInt(200);
    }
}
