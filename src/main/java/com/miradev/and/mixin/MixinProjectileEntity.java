package com.miradev.and.mixin;

import com.miradev.and.common.*;
import com.miradev.and.entity.ResidueEntity;
import com.miradev.and.registry.ResidueRegistry;
import com.miradev.and.registry.ResidueTriggerRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ttv.migami.jeg.entity.projectile.ProjectileEntity;

import javax.annotation.Nullable;

@Mixin(value = ProjectileEntity.class, remap = false)
public class MixinProjectileEntity {

    @Unique private boolean and$residueHandled = false;

    @Inject(method = "onHitEntity", at = @At("TAIL"))
    private void and$onResidueHitEntity(Entity entity, Vec3 hitVec, Vec3 startVec, Vec3 endVec, boolean headshot, CallbackInfo ci) {
        ProjectileEntity self = (ProjectileEntity) (Object) this;
        ResidueTrigger trigger = and$trigger(self);
        if (trigger == null) return;

        ResidueTrigger.Hit hit = (headshot && trigger.onHeadshot() != null) ? trigger.onHeadshot() : trigger.onBody();
        if (hit == null) return;
        and$apply(self, trigger, hit, hitVec.x, hitVec.y, hitVec.z, null);
    }

    @Inject(method = "onHitBlock", at = @At("TAIL"))
    private void and$onResidueHitBlock(BlockState state, BlockPos pos, Direction face, double x, double y, double z, CallbackInfo ci) {
        ProjectileEntity self = (ProjectileEntity) (Object) this;
        ResidueTrigger trigger = and$trigger(self);
        if (trigger == null || trigger.onBlock() == null) return;
        and$apply(self, trigger, trigger.onBlock(), x, y, z, face);
    }

    @Unique @Nullable
    private ResidueTrigger and$trigger(ProjectileEntity self) {
        if (this.and$residueHandled) return null;
        return ResidueTriggerRegistry.get(ResidueTriggerRegistry.keyFor(self.getWeapon()));
    }

    @Unique
    private void and$apply(ProjectileEntity self, ResidueTrigger trigger, ResidueTrigger.Hit hit,
                           double x, double y, double z, @Nullable Direction face) {
        ResidueProfile type = ResidueRegistry.get(trigger.residue());
        LivingEntity shooter = self.getShooter();
        if (type == null || shooter == null) return;
        this.and$residueHandled = true;

        Level level = self.level();
        ResidueEntity residue = face == null
                ? ResidueEntity.tryCreate(level, x, y, z, type, shooter)
                : ResidueEntity.tryCreate(level, x, y, z, face, type, shooter);
        if (residue != null) {
            if (hit.ignite()) residue.activate();
            level.addFreshEntity(residue);
        }

        if (hit.igniteOwned()) ResidueEntity.igniteOwned(shooter, type.id());
        if (hit.explosion() != null) {
            ResidueTrigger.Explosion e = hit.explosion();
            level.explode(shooter, x, y, z, e.power(), e.fire(),
                    e.breakBlocks() ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
        }
    }

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