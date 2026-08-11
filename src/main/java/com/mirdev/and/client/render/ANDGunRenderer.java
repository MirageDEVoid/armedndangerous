package com.mirdev.and.client.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.opengl.GL11;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import ttv.migami.jeg.client.render.gun.animated.AnimatedGunRenderer;
import ttv.migami.jeg.item.AnimatedGunItem;

import javax.annotation.Nullable;

public class ANDGunRenderer extends AnimatedGunRenderer {

    private final GeoItemRenderer<AnimatedGunItem> tpRenderer;

    public ANDGunRenderer(ResourceLocation fpPath, ANDGunModel tpModel) {
        super(fpPath);
        this.tpRenderer = new GeoItemRenderer<AnimatedGunItem>(tpModel) {
            @Override
            public void actuallyRender(PoseStack poseStack, AnimatedGunItem animatable, BakedGeoModel model,
                                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                       boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                       float red, float green, float blue, float alpha) {
                super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer,
                        true, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
            }

            @Override
            protected void renderInGui(ItemDisplayContext transformType, PoseStack poseStack,
                                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
                setupLightingForGuiRender();

                MultiBufferSource.BufferSource directBuffer =
                        Minecraft.getInstance().renderBuffers().bufferSource();

                directBuffer.endBatch();

                GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);

                RenderType renderType = getRenderType(this.animatable, getTextureLocation(this.animatable),
                        directBuffer, Minecraft.getInstance().getFrameTime());
                VertexConsumer buffer = directBuffer.getBuffer(renderType);

                poseStack.pushPose();
                defaultRender(poseStack, this.animatable, directBuffer, renderType, buffer,
                        0, Minecraft.getInstance().getFrameTime(), packedLight);

                directBuffer.endBatch();

                RenderSystem.enableDepthTest();
                Lighting.setupFor3DItems();
                poseStack.popPose();
            }
        };
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context,
                             PoseStack poseStack, MultiBufferSource bufferSource,
                             int packedLight, int packedOverlay) {
        if (!context.firstPerson()
                && !context.equals(ItemDisplayContext.FIXED)
                && !context.equals(ItemDisplayContext.GROUND)) {
            tpRenderer.renderByItem(stack, context, poseStack, bufferSource, packedLight, packedOverlay);
        } else {
            super.renderByItem(stack, context, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }
}