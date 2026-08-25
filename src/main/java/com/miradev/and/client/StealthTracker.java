package com.miradev.and.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class StealthTracker {

    private static final double MAX_RANGE = 64.0;

    @Nullable
    public static Mob getLookedAtMob() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return null;

        float partialTick = mc.getDeltaFrameTime();
        Vec3 start = player.getEyePosition(partialTick);
        Vec3 look  = player.getViewVector(partialTick);
        Vec3 end   = start.add(look.scale(MAX_RANGE));

        AABB searchBox = player.getBoundingBox()
                .expandTowards(look.scale(MAX_RANGE))
                .inflate(1.0);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                mc.level,
                player,
                start,
                end,
                searchBox,
                entity -> entity instanceof Mob && entity.isAlive() && !entity.isSpectator()
        );

        if (hit == null) return null;
        return (Mob) hit.getEntity();
    }

    public static boolean hasDetectedPlayer(LocalPlayer player, Mob mob) {
        // Primary check – matches MixinProjectileEntity
        if (mob.getTarget() == player) {
            return true;
        }

        if (mob.isAggressive() && mob.getSensing().hasLineOfSight(player)) {
            return true;
        }

        return false;
    }

    public static boolean isInFollowRange(LocalPlayer player, Mob mob) {
        double followRange = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
        double distanceSq  = player.distanceToSqr(mob);
        return distanceSq <= followRange * followRange;
    }
}