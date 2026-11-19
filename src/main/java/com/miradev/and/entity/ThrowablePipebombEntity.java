package com.miradev.and.entity;

import com.miradev.and.common.FragmentProfile;
import com.miradev.and.fragment.FragmentConfig;
import com.miradev.and.fragment.FragmentationHelper;
import com.miradev.and.registry.FragmentRegistry;
import com.miradev.and.registry.ResidueTriggerRegistry;
import com.miradev.and.init.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import ttv.migami.jeg.entity.projectile.ProjectileEntity;
import ttv.migami.jeg.entity.throwable.ThrowableGrenadeEntity;

public class ThrowablePipebombEntity extends ThrowableGrenadeEntity {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean warnedMissingProfile = false;
    private static boolean spawnLightSupported = true;
    
    public ThrowablePipebombEntity(EntityType<? extends ThrowablePipebombEntity> type, Level level) {
        super(type, level);
    }

    public ThrowablePipebombEntity(EntityType<? extends ThrowablePipebombEntity> type, Level level,
                                   LivingEntity owner, int timeLeft) {
        super(type, level, owner);
        this.setItem(new ItemStack(ModItems.PIPEBOMB.get()));
        this.setMaxLife(timeLeft);
    }

    @Override
    public void onDeath() {
        if (!(this.level() instanceof ServerLevel server)) return;

        trySpawnLight(this.blockPosition());

        // 1. blast, helper reset iframe before ts
        float radius = FragmentConfig.PIPEBOMB_BLAST_RADIUS.get().floatValue();
        if (radius > 0F) {
            ProjectileEntity.createExplosion(this, radius, true);
        }

        // 2. data fragments, no profile = nothing
        FragmentProfile profile = FragmentRegistry.get(ResidueTriggerRegistry.keyFor(this.getItem()));
        if (profile == null) {
            if (!warnedMissingProfile) {
                warnedMissingProfile = true;
                LOGGER.warn("[extra] no fragmentation profile bound to '{}', explosion only",
                        ResidueTriggerRegistry.keyFor(this.getItem()));
            }
            return;
        }

        FragmentationHelper.Result result = FragmentationHelper.burst(
                server, FragmentationHelper.originFor(this), null, this.getOwner(), this, profile);
    }

    private void trySpawnLight(BlockPos pos) {
        if (!spawnLightSupported) return;
        try {
            this.spawnLight(pos);
        } catch (NoSuchMethodError e) {
            spawnLightSupported = false;
            LOGGER.warn("JEG is older than 0.14, skipping explosion light");
        }
    }
}