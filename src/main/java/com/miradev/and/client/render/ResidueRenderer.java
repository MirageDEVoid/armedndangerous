package com.miradev.and.client.render;

import com.miradev.and.Reference;
import com.miradev.and.common.ResidueProfile;
import com.miradev.and.entity.ResidueEntity;
import com.miradev.and.init.ModEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ResidueRenderer extends EntityRenderer<ResidueEntity> {

    public ResidueRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    private static final int TRANSITION_DURATION = 20;
    private static final float GROUND_OFFSET = 0.01F;

    private static final int SPAWN_GROW_DURATION = 10;
    private static final int DESPAWN_SHRINK_DURATION = 10;
    private static final int BURNOUT_COLOR_DURATION = 20;

    private static final int FULL_BRIGHT = LightTexture.pack(15, 15);

    private float[] computeColor(ResidueEntity e, float pt) {
        ResidueProfile.Look lit = e.getIgnitedLook();
        int base = e.getLook().color();
        if (lit == null || !e.isActive()) return rgb(base);

        float age = e.getEffectiveAge() + pt;
        float remaining = e.getLifetime() - age;
        if (remaining < BURNOUT_COLOR_DURATION) {
            return lerpColor(base, lit.color(), Mth.clamp(remaining / BURNOUT_COLOR_DURATION, 0F, 1F));
        }
        float progress = Mth.clamp((age - e.getIgniteTick()) / TRANSITION_DURATION, 0F, 1F);
        return lerpColor(base, lit.color(), progress);
    }

    private static float[] rgb(int c) {
        return new float[]{(c >> 16 & 255) / 255F, (c >> 8 & 255) / 255F, (c & 255) / 255F};
    }

    private static float[] lerpColor(int a, int b, float t) {
        float[] x = rgb(a), y = rgb(b);
        return new float[]{Mth.lerp(t, x[0], y[0]), Mth.lerp(t, x[1], y[1]), Mth.lerp(t, x[2], y[2])};
    }

    private float computeLifecycleScale(ResidueEntity entity, float partialTicks) {
        float effectiveAge = entity.getEffectiveAge() + partialTicks;
        float duration = entity.getLifetime();

        if (effectiveAge < SPAWN_GROW_DURATION) {
            return Mth.clamp(effectiveAge / SPAWN_GROW_DURATION, 0F, 1F);
        }

        float remaining = duration - effectiveAge;
        if (remaining < DESPAWN_SHRINK_DURATION) {
            return Mth.clamp(remaining / DESPAWN_SHRINK_DURATION, 0F, 1F);
        }

        return 1F;
    }

    @Override
    public void render(ResidueEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {

        if (entity.getAge() < entity.getFallDelayTicks()) {
            return;
        }

        float radius = (float) entity.getRadius();
        boolean lit = entity.isActive() && entity.getIgnitedLook() != null;
        ResidueProfile.Look look = lit ? entity.getIgnitedLook() : entity.getLook();
        float[] color = computeColor(entity, partialTicks);
        int emissiveLight = lit ? FULL_BRIGHT : packedLight;

        float scaledRadius = radius * computeLifecycleScale(entity, partialTicks);

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getModelManager()
                .getAtlas(TextureAtlas.LOCATION_BLOCKS)
                .getSprite(look.sprite());

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        poseStack.pushPose();
        poseStack.translate(0, GROUND_OFFSET, 0);

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
        Matrix4f matrix = poseStack.last().pose();

        float thickness = entity.getThickness();
        float y0 = 0F;
        float y1 = thickness;

        // top face
        quad(consumer, matrix,
                -scaledRadius, y1, -scaledRadius,
                -scaledRadius, y1,  scaledRadius,
                scaledRadius, y1,  scaledRadius,
                scaledRadius, y1, -scaledRadius,
                color, emissiveLight, 0, 1, 0,
                u0, v0, u0, v1, u1, v1, u1, v0, 0.85F);

        // bottom face
        quad(consumer, matrix,
                -scaledRadius, y0,  scaledRadius,
                -scaledRadius, y0, -scaledRadius,
                scaledRadius, y0, -scaledRadius,
                scaledRadius, y0,  scaledRadius,
                color, emissiveLight, 0, -1, 0,
                u0, v0, u0, v1, u1, v1, u1, v0, 0.85F);

        float vThin = v0 + (v1 - v0) * (thickness / Math.max(radius, 0.1F));

        // sides
        quad(consumer, matrix,
                -scaledRadius, y0, -scaledRadius,
                -scaledRadius, y1, -scaledRadius,
                scaledRadius, y1, -scaledRadius,
                scaledRadius, y0, -scaledRadius,
                color, emissiveLight, 0, 0, -1,
                u0, v0, u0, vThin, u1, vThin, u1, v0, 0.85F);

        quad(consumer, matrix,
                scaledRadius, y0,  scaledRadius,
                scaledRadius, y1,  scaledRadius,
                -scaledRadius, y1,  scaledRadius,
                -scaledRadius, y0,  scaledRadius,
                color, emissiveLight, 0, 0, 1,
                u0, v0, u0, vThin, u1, vThin, u1, v0, 0.85F);

        quad(consumer, matrix,
                -scaledRadius, y0,  scaledRadius,
                -scaledRadius, y1,  scaledRadius,
                -scaledRadius, y1, -scaledRadius,
                -scaledRadius, y0, -scaledRadius,
                color, emissiveLight, -1, 0, 0,
                u0, v0, u0, vThin, u1, vThin, u1, v0, 0.85F);

        quad(consumer, matrix,
                scaledRadius, y0, -scaledRadius,
                scaledRadius, y1, -scaledRadius,
                scaledRadius, y1,  scaledRadius,
                scaledRadius, y0,  scaledRadius,
                color, emissiveLight, 1, 0, 0,
                u0, v0, u0, vThin, u1, vThin, u1, v0, 0.85F);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private void quad(VertexConsumer consumer, Matrix4f matrix,
                      float x1, float y1, float z1, float x2, float y2, float z2,
                      float x3, float y3, float z3, float x4, float y4, float z4,
                      float[] color, int packedLight, float nx, float ny, float nz,
                      float u1, float v1, float u2, float v2, float u3, float v3, float u4, float v4,
                      float alpha) {
        consumer.vertex(matrix, x1, y1, z1).color(color[0], color[1], color[2], alpha).uv(u1, v1)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(nx, ny, nz).endVertex();
        consumer.vertex(matrix, x2, y2, z2).color(color[0], color[1], color[2], alpha).uv(u2, v2)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(nx, ny, nz).endVertex();
        consumer.vertex(matrix, x3, y3, z3).color(color[0], color[1], color[2], alpha).uv(u3, v3)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(nx, ny, nz).endVertex();
        consumer.vertex(matrix, x4, y4, z4).color(color[0], color[1], color[2], alpha).uv(u4, v4)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(nx, ny, nz).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(ResidueEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    private static float[] lerpColor(float[] from, float[] to, float t) {
        return new float[] {
                Mth.lerp(t, from[0], to[0]),
                Mth.lerp(t, from[1], to[1]),
                Mth.lerp(t, from[2], to[2])
        };
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.RESIDUE.get(), ResidueRenderer::new);
    }
}