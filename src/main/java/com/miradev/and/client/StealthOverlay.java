package com.miradev.and.client;

import com.miradev.and.Reference;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class StealthOverlay {

    private static final ResourceLocation NOT_SPOT =
            new ResourceLocation(Reference.MOD_ID, "textures/gui/not_spot.png");
    private static final ResourceLocation SPOT_NOT_TARGET =
            new ResourceLocation(Reference.MOD_ID, "textures/gui/spot_not_target.png");

    private static final int ICON_SIZE = 16;
    private static final int HALF_ICON = ICON_SIZE / 2;
    private static final int PADDING   = 6;

    private static final float MAX_ALPHA     = 0.75F;
    private static final float FADE_IN_SPEED  = 0.12F;
    private static final float FADE_OUT_SPEED = 0.08F;

    private static float currentAlpha = 0.0F;
    private static ResourceLocation currentIcon = null;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        ResourceLocation desiredIcon = null;

        if (StealthAdvantageChecker.isHoldingStealthGun()) {
            Mob target = StealthTracker.getLookedAtMob();
            if (target != null && !StealthTracker.hasDetectedPlayer(player, target)) {
                if (StealthTracker.isInFollowRange(player, target)) {
                    desiredIcon = SPOT_NOT_TARGET;
                } else {
                    desiredIcon = NOT_SPOT;
                }
            }
        }

        if (desiredIcon != null) {
            if (currentIcon != desiredIcon) {
                currentAlpha = Math.min(currentAlpha, 0.30F);
                currentIcon = desiredIcon;
            }
            currentAlpha = Math.min(MAX_ALPHA, currentAlpha + FADE_IN_SPEED);
        } else {
            currentAlpha = Math.max(0.0F, currentAlpha - FADE_OUT_SPEED);
            if (currentAlpha <= 0.001F) {
                currentIcon = null;
                currentAlpha = 0.0F;
            }
        }

        if (currentIcon == null || currentAlpha <= 0.001F) return;

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        int x = (screenW / 2) + PADDING;
        int y = (screenH / 2) - HALF_ICON;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        event.getGuiGraphics().setColor(1.0F, 1.0F, 1.0F, currentAlpha);
        event.getGuiGraphics().blit(currentIcon, x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

        event.getGuiGraphics().setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }
}