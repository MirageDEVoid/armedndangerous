package com.miradev.and.client;

import com.miradev.and.ArmedNDangerous;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.client.handler.AimingHandler;
import ttv.migami.jeg.client.handler.CrosshairHandler;
import ttv.migami.jeg.client.handler.GunRenderingHandler;
import ttv.migami.jeg.client.render.crosshair.Crosshair;
import ttv.migami.jeg.common.Gun;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.util.GunCompositeStatHelper;

@Mod.EventBusSubscriber(modid = ArmedNDangerous.MODID, value = Dist.CLIENT)
public class StealthOverlay {

    private static final int BAR_WIDTH  = 16;
    private static final int BAR_HEIGHT = 3;
    private static final int HALF_BAR   = BAR_WIDTH / 2;
    private static final int PADDING    = 3;

    // Smooth fade — driven via RGB under inversion blend
    private static float currentAlpha = 0.0F;
    private static final float FADE_IN_SPEED  = 0.08F;
    private static final float FADE_OUT_SPEED = 0.05F;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        // --- Condition checks ---
        boolean shouldShow = false;
        if (StealthAdvantageChecker.isHoldingStealthGun()) {
            Mob target = StealthTracker.getLookedAtMob();
            if (target != null && StealthTracker.isOutsideFollowRange(player, target)) {
                shouldShow = true;
            }
        }

        // --- Smooth fade ---
        if (shouldShow) {
            currentAlpha = Math.min(1.0F, currentAlpha + FADE_IN_SPEED);
        } else {
            currentAlpha = Math.max(0.0F, currentAlpha - FADE_OUT_SPEED);
        }

        if (currentAlpha <= 0.0F) return;

        // --- Position ---
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        int crosshairBottom = getCrosshairBottomOffset(mc, player, event.getPartialTick());

        int x = (screenW / 2) - HALF_BAR;
        int y = (screenH / 2) + crosshairBottom + PADDING;

        // --- Draw using BufferBuilder so blend state is respected ---
        // Inversion blend: same mode as the DynamicCrosshair / vanilla crosshair.
        // Fade is driven by RGB (not alpha) since inversion blend ignores SRC_ALPHA:
        //   a=0 → src=(0,0,0) → result=dst      (invisible)
        //   a=1 → src=(1,1,1) → result=1-dst    (fully inverted)
        float a = currentAlpha;

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR,
                GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
        );
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f matrix = event.getGuiGraphics().pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, x,            y + BAR_HEIGHT, 0).color(a, a, a, 1.0F).endVertex();
        buffer.vertex(matrix, x + BAR_WIDTH, y + BAR_HEIGHT, 0).color(a, a, a, 1.0F).endVertex();
        buffer.vertex(matrix, x + BAR_WIDTH, y,             0).color(a, a, a, 1.0F).endVertex();
        buffer.vertex(matrix, x,            y,              0).color(a, a, a, 1.0F).endVertex();
        BufferUploader.drawWithShader(buffer.end());

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static int getCrosshairBottomOffset(Minecraft mc, LocalPlayer player, float partialTick) {
        Crosshair crosshair = CrosshairHandler.get().getCurrentCrosshair();
        boolean isDynamic = crosshair != null
                && crosshair.getLocation().toString().equals("jeg:dynamic");

        if (!isDynamic) return 8;

        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof GunItem gunItem)) return 8;

        Gun modifiedGun = gunItem.getModifiedGun(held);
        float size1 = 7F;

        float aiming           = (float) AimingHandler.get().getNormalisedAdsProgress();
        float sprintTransition = (float) GunRenderingHandler.get().getSprintTransition(partialTick);

        float baseSpread = GunCompositeStatHelper.getCompositeSpread(held, modifiedGun);
        float minSpread  = modifiedGun.getGeneral().isAlwaysSpread() ? baseSpread : 0F;
        float aimingMult = (float) Mth.lerp(aiming, 1.0F, 0.5F);
        float spread     = Mth.clamp(
                (float) Mth.lerp(sprintTransition * 0.5, minSpread, baseSpread) * aimingMult,
                0F, 32F);

        float spreadMultiplier =
                Config.CLIENT.display.dynamicCrosshairSpreadMultiplier.get().floatValue();

        float scale = 1F + spread * (2F * spreadMultiplier);
        float scaleSize   = scale / 6F + 1.15F;
        float tightness   = (float) (0.8 - Config.CLIENT.display.dynamicCrosshairBaseSpread.get() / 2.0);
        float spreadTrans = (float) (Mth.lerp(0.95f, scaleSize - 1F, Math.log(scaleSize)) * 2.8F);

        return (int) (scaleSize * (size1 / 2F + spreadTrans - tightness));
    }
}