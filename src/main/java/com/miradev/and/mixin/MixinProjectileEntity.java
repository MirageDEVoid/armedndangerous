package com.miradev.and.mixin;

import com.miradev.and.common.ANDAdvantages;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ttv.migami.jeg.entity.projectile.ProjectileEntity;

@Mixin(value = ProjectileEntity.class, remap = false)
public class MixinProjectileEntity {

    @Inject(method = "advantageMultiplier", at = @At("RETURN"), cancellable = true)
    private void and$applyCustomAdvantage(Entity entity, CallbackInfoReturnable<Float> cir) {
        ProjectileEntity self = (ProjectileEntity)(Object)this;

        ResourceLocation advantage = self.getAdvantage();

        if (ANDAdvantages.STEALTH.equals(advantage)) {
            cir.setReturnValue(stealthMultiplier(self, entity));
        }
    }

    private static float stealthMultiplier(ProjectileEntity projectile, Entity target) {
        LivingEntity rawShooter = projectile.getShooter();
        if (!(rawShooter instanceof Player shooter)) return 1.0F;
        if (!(target instanceof LivingEntity livingTarget)) return 1.0F;
        if (!(livingTarget instanceof Mob mob)) return 1.0F;

        LivingEntity mobTarget = mob.getTarget();

        if (mobTarget == shooter) {
            return 1.0F;
        }

        if (mobTarget != null) {
            return 1.5F;
        }

        return 2.5F;
    }
}