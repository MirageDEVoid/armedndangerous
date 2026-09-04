package com.miradev.and.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.object.Color;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import ttv.migami.jeg.client.render.gun.animated.AnimatedGunRenderer;
import ttv.migami.jeg.common.Gun;
import ttv.migami.jeg.item.AnimatedGunItem;
import ttv.migami.jeg.item.attachment.IAttachment;
import ttv.migami.jeg.item.attachment.item.PaintJobCanItem;
import ttv.migami.jeg.util.DyeUtils;

import java.util.Optional;

public class ANDGunRenderer extends AnimatedGunRenderer {

    private final GeoItemRenderer<AnimatedGunItem> tpRenderer;
    private final ANDGunModel tpModel;

    private ResourceLocation lastTexture = null;
    private ResourceLocation lastModel   = null;

    public ANDGunRenderer(ResourceLocation fpPath, ANDGunModel tpModel) {
        super(fpPath);
        this.tpModel = tpModel;

        this.tpRenderer = new GeoItemRenderer<>(tpModel) {

            @Override
            public Color getRenderColor(AnimatedGunItem animatable, float partialTick, int packedLight) {
                return Color.ofOpaque(DyeUtils.getStoredDyeRGB(this.currentItemStack));
            }

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

                RenderSystem.setShaderLights(
                        new Vector3f(0.2f, 1.0f, -0.7f).normalize(),
                        new Vector3f(-0.2f, 1.0f, 0.7f).normalize()
                );

                RenderType renderType = getRenderType(this.animatable, getTextureLocation(this.animatable),
                        bufferSource, Minecraft.getInstance().getFrameTime());
                VertexConsumer buffer = bufferSource.getBuffer(renderType);

                poseStack.pushPose();
                defaultRender(poseStack, this.animatable, bufferSource, renderType, buffer,
                        0, Minecraft.getInstance().getFrameTime(), packedLight);
                poseStack.popPose();

                RenderSystem.enableDepthTest();
            }
        };
    }

    private ResourceLocation getValidTexture(ResourceLocation texture, ItemStack stack) {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
        if (resource.isPresent()) {
            return texture;
        }

        return new ResourceLocation(
                stack.getItem().getCreatorModId(stack) != null
                        ? stack.getItem().getCreatorModId(stack)
                        : "and",
                "textures/animated/gun/" + stack.getItem() + ".png"
        );
    }

    private ResourceLocation getValidModel(ResourceLocation model, ItemStack stack) {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(model);
        if (resource.isPresent()) {
            return model;
        }
        return new ResourceLocation(
                stack.getItem().getCreatorModId(stack) != null
                        ? stack.getItem().getCreatorModId(stack)
                        : "and",
                "geo/item/" + stack.getItem() + "_tp.geo.json"
        );
    }

    private void updateTpResources(ItemStack stack) {
        String modId = stack.getItem().getCreatorModId(stack);
        if (modId == null) modId = "and";

        ResourceLocation newTexture;
        ResourceLocation newModel;

        if (stack.hasTag()
                && Gun.getAttachment(IAttachment.Type.PAINT_JOB, stack).getItem() instanceof PaintJobCanItem paintJobCanItem) {

            String paintJob = paintJobCanItem.getPaintJob();

            newTexture = new ResourceLocation(modId,
                    "textures/animated/gun/paintjob/" + paintJob + "/" + stack.getItem() + ".png");

            newModel = new ResourceLocation(modId,
                    "geo/item/gun/paintjob/" + paintJob + "/" + stack.getItem() + "_tp.geo.json");
        } else {
            newTexture = new ResourceLocation(modId,
                    "textures/animated/gun/" + stack.getItem() + ".png");
            newModel = null;
        }

        newTexture = getValidTexture(newTexture, stack);

        if (!newTexture.equals(lastTexture)) {
            lastTexture = newTexture;
            tpModel.setCurrentTexture(newTexture);
        }

        if (newModel != null) {
            newModel = getValidModel(newModel, stack);
            if (!newModel.equals(lastModel)) {
                lastModel = newModel;
                tpModel.setCurrentModel(newModel);
            }
        } else if (lastModel != null) {
            lastModel = null;
            tpModel.setCurrentModel(null);
        }
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context,
                             PoseStack poseStack, MultiBufferSource bufferSource,
                             int packedLight, int packedOverlay) {

        if (context == ItemDisplayContext.GUI
                || context == ItemDisplayContext.FIXED
                || context == ItemDisplayContext.GROUND
                || !context.firstPerson()) {

            updateTpResources(stack);

            tpRenderer.renderByItem(stack, context, poseStack, bufferSource, packedLight, packedOverlay);
        } else {
            super.renderByItem(stack, context, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }
}