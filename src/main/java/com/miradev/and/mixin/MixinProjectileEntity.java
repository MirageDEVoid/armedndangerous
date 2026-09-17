package com.miradev.and.mixin;

import com.miradev.and.common.ANDAdvantages;
import com.miradev.and.common.HazardTriggerBinding;
import com.miradev.and.common.HazardZoneManager;
import com.miradev.and.common.HazardZoneType;
import com.miradev.and.entity.HazardZoneEntity;
import com.miradev.and.registry.HazardTriggerRegistry;
import com.miradev.and.registry.HazardZoneRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ttv.migami.jeg.entity.projectile.ProjectileEntity;

@Mixin(value = ProjectileEntity.class, remap = false)
public class MixinProjectileEntity {

    @Unique
    private boolean and$hasSpawnedHazard = false;

    @Inject(method = "onHitEntity", at = @At("TAIL"))
    private void and$onHazardHitEntity(Entity entity, Vec3 hitVec, Vec3 startVec, Vec3 endVec, boolean headshot, CallbackInfo ci) {
        if (!headshot) return;
        if (this.and$hasSpawnedHazard) return;

        ProjectileEntity self = (ProjectileEntity)(Object)this;
        ItemStack weapon = self.getWeapon();

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(weapon.getItem());
        if (itemId == null) return;

        HazardTriggerBinding binding = HazardTriggerRegistry.get(itemId.toString());
        if (binding == null) return;

        HazardZoneType type = HazardZoneRegistry.get(binding.hazardType);
        if (type == null || self.getShooter() == null) return;

        HazardZoneEntity zone = HazardZoneEntity.tryCreate(self.level(), hitVec.x, hitVec.y, hitVec.z, type, self.getShooter());
        if (zone == null) return;

        this.and$hasSpawnedHazard = true;

        zone.ignite();
        self.level().addFreshEntity(zone);
        HazardZoneManager.register(zone);

        if (binding.ignitesOwnedOnHeadshot) {
            HazardZoneManager.igniteOwned(self.getShooter(), binding.hazardType);
        }

        if (binding.explosionPower > 0.0F) {
            self.level().explode(
                    self.getShooter(),
                    hitVec.x, hitVec.y, hitVec.z,
                    binding.explosionPower,
                    binding.explosionCausesFire,
                    binding.explosionDestroysBlocks
                            ? net.minecraft.world.level.Level.ExplosionInteraction.TNT
                            : net.minecraft.world.level.Level.ExplosionInteraction.NONE
            );
        }
    }

    @Inject(method = "onHitBlock", at = @At("TAIL"))
    private void and$onHazardHitBlock(BlockState state, BlockPos pos, Direction face, double x, double y, double z, CallbackInfo ci) {
        if (this.and$hasSpawnedHazard) return;

        ProjectileEntity self = (ProjectileEntity)(Object)this;
        ItemStack weapon = self.getWeapon();

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(weapon.getItem());
        if (itemId == null) return;

        HazardTriggerBinding binding = HazardTriggerRegistry.get(itemId.toString());
        if (binding == null) return;

        HazardZoneType type = HazardZoneRegistry.get(binding.hazardType);
        if (type == null || self.getShooter() == null) return;

        double fallDist = y - (pos.getY() + 1);
        if (fallDist < 1.5) {
            for (int i = 0; i < 6; i++) {
                self.level().addParticle(ParticleTypes.LAVA,
                        x + (self.level().random.nextDouble() - 0.5) * 0.4,
                        y + 0.1,
                        z + (self.level().random.nextDouble() - 0.5) * 0.4,
                        0, 0.05, 0);
            }
        }

        HazardZoneEntity zone = HazardZoneEntity.tryCreate(self.level(), x, y, z, face, type, self.getShooter());
        if (zone == null) return;

        this.and$hasSpawnedHazard = true;

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