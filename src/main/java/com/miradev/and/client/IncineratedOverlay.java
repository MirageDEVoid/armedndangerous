package com.miradev.and.client;

import com.miradev.and.Reference;
import com.miradev.and.init.ModEffects;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class IncineratedOverlay {

    private static final int RAMP_TICKS = 100;
    private static final int FADE_TICKS = 20;

    private static final float[] SHALLOW = {1.00F, 0.45F, 0.10F};
    private static final float[] DEEP    = {0.80F, 0.06F, 0.02F};
    private static final float MIN_ALPHA = 0.30F, MAX_ALPHA = 0.70F;
    private static final float MIN_INSET = 0.20F, MAX_INSET = 0.36F;

    @SubscribeEvent
    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.VIGNETTE.id(), "incinerated", IncineratedOverlay::render);
    }

    private static void render(ForgeGui gui, GuiGraphics g, float partialTick, int w, int h) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        MobEffectInstance fx = player.getEffect(ModEffects.INCINERATED.get());
        if (fx == null) return;

        float linear = Mth.clamp((IncineratedClientEffects.burnTicks + partialTick) / RAMP_TICKS, 0F, 1F);
        float depth = linear * linear * (3F - 2F * linear);

        int remaining = fx.isInfiniteDuration() ? FADE_TICKS : fx.getDuration();
        float fade = Mth.clamp(remaining / (float) FADE_TICKS, 0F, 1F);
        float breathe = 0.88F + 0.12F * Mth.sin((player.tickCount + partialTick) * 0.3F);

        float alpha = Mth.lerp(depth, MIN_ALPHA, MAX_ALPHA) * fade * breathe;
        if (alpha <= 0.001F) return;
        float inset = Mth.lerp(depth, MIN_INSET, MAX_INSET);
        float r = Mth.lerp(depth, SHALLOW[0], DEEP[0]);
        float gr = Mth.lerp(depth, SHALLOW[1], DEEP[1]);
        float b = Mth.lerp(depth, SHALLOW[2], DEEP[2]);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f m = g.pose().last().pose();
        drawRing(m, w, h, inset, r, gr, b, alpha * 0.6F);
        drawRing(m, w, h, inset * 0.5F, r, gr, b, alpha * 0.4F);

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void drawRing(Matrix4f m, int w, int h, float insetFraction,
                                 float r, float g, float b, float alpha) {
        float in = Math.min(w, h) * insetFraction;
        float[][] outer = {{0, 0}, {w, 0}, {w, h}, {0, h}, {0, 0}};
        float[][] inner = {{in, in}, {w - in, in}, {w - in, h - in}, {in, h - in}, {in, in}};

        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < 5; i++) {
            buf.vertex(m, outer[i][0], outer[i][1], 0).color(r, g, b, alpha).endVertex();
            buf.vertex(m, inner[i][0], inner[i][1], 0).color(r, g, b, 0F).endVertex();
        }
        BufferUploader.drawWithShader(buf.end());
    }
}