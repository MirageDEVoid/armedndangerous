package com.miradev.and.mixin;

import com.miradev.and.item.ANDGunItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.client.handler.GunRenderingHandler;

@Mixin(GunRenderingHandler.class)
public class MixinGunRenderingHandler {

    @Inject(
            method = "renderMuzzleFlash",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void and$cancelTpFlashForAndGuns(
            LivingEntity entity,
            PoseStack poseStack,
            MultiBufferSource buffer,
            ItemStack weapon,
            ItemDisplayContext display,
            float partialTicks,
            CallbackInfo ci
    ) {
        if (weapon.getItem() instanceof ANDGunItem && !display.firstPerson()) {
            ci.cancel();
        }
    }
}