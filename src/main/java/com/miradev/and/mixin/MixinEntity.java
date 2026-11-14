package com.miradev.and.mixin;

import com.miradev.and.init.ModEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MixinEntity {

    /** Client-only: makes burning-effect entities render as on fire. Never touches server logic. */
    @Inject(method = "isOnFire", at = @At("RETURN"), cancellable = true)
    private void and$incineratedLooksOnFire(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) return;
        Entity self = (Entity) (Object) this;
        Level level = self.level();
        if (level == null || !level.isClientSide) return;
        if (self instanceof LivingEntity living && !living.fireImmune()
                && living.hasEffect(ModEffects.INCINERATED.get())) {
            cir.setReturnValue(true);
        }
    }
}