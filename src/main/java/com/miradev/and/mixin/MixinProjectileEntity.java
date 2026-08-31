package com.miradev.and.mixin;

import com.miradev.and.common.ANDAdvantages;
import com.miradev.and.common.HazardZoneManager;
import com.miradev.and.common.HazardZoneType;
import com.miradev.and.entity.HazardZoneEntity;
import com.miradev.and.registry.HazardZoneRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ttv.migami.jeg.entity.projectile.ProjectileEntity;

@Mixin(value = ProjectileEntity.class, remap = false)
public class MixinProjectileEntity {

    @Inject(method = "onHitEntity", at = @At("TAIL"))
    private void and$onHazardHitEntity(Entity entity, Vec3 hitVec, Vec3 startVec, Vec3 endVec, boolean headshot, CallbackInfo ci) {
        ProjectileEntity self = (ProjectileEntity)(Object)this;
        ItemStack weapon = self.getWeapon();

        if (weapon.getTag() == null) return;
        if (!weapon.getTag().getBoolean("HazardOnHit")) return;

        ResourceLocation hazardTypeId = ResourceLocation.tryParse(weapon.getTag().getString("HazardType"));
        if (hazardTypeId == null) return;

        HazardZoneType type = HazardZoneRegistry.get(hazardTypeId);
        if (type == null) return;

        if (self.getShooter() != null) {
            HazardZoneEntity zone = new HazardZoneEntity(self.level(), hitVec.x, hitVec.y, hitVec.z, type, self.getShooter());
            self.level().addFreshEntity(zone);
            HazardZoneManager.register(zone);

            if (headshot && weapon.getTag().getBoolean("IgnitesOwnedHazardsOnHeadshot")) {
                HazardZoneManager.igniteOwned(self.getShooter(), hazardTypeId);
            }
        }
    }

    @Inject(method = "onHitBlock", at = @At("TAIL"))
    private void and$onHazardHitBlock(BlockState state, BlockPos pos, Direction face, double x, double y, double z, CallbackInfo ci) {
        ProjectileEntity self = (ProjectileEntity)(Object)this;
        ItemStack weapon = self.getWeapon();

        if (weapon.getTag() == null || !weapon.getTag().getBoolean("HazardOnHit")) return;

        ResourceLocation hazardTypeId = ResourceLocation.tryParse(weapon.getTag().getString("HazardType"));
        HazardZoneType type = hazardTypeId != null ? HazardZoneRegistry.get(hazardTypeId) : null;
        if (type == null || self.getShooter() == null) return;

        HazardZoneEntity zone = new HazardZoneEntity(self.level(), x, y, z, type, self.getShooter());
        self.level().addFreshEntity(zone);
        HazardZoneManager.register(zone);
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