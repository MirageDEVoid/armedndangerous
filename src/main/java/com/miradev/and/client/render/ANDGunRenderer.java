package com.miradev.and.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.object.Color;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import ttv.migami.jeg.client.GunRenderType;
import ttv.migami.jeg.client.handler.GunRenderingHandler;
import ttv.migami.jeg.client.handler.ShootingHandler;
import ttv.migami.jeg.client.render.gun.animated.AnimatedGunRenderer;
import ttv.migami.jeg.common.Gun;
import ttv.migami.jeg.item.AnimatedGunItem;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.item.attachment.IAttachment;
import ttv.migami.jeg.item.attachment.item.PaintJobCanItem;
import ttv.migami.jeg.util.DyeUtils;
import ttv.migami.jeg.util.GunModifierHelper;

import java.util.Optional;

public class ANDGunRenderer extends AnimatedGunRenderer {
    private final java.util.Set<String> debuggedGuns = new java.util.HashSet<>();
    private final GeoItemRenderer<AnimatedGunItem> tpRenderer;
    private final ANDGunModel tpModel;

    private ResourceLocation lastTexture = null;
    private ResourceLocation lastModel   = null;

    private ItemStack currentStack;
    private ItemDisplayContext currentContext;
    private LivingEntity currentEntity;

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
            public void renderRecursively(PoseStack poseStack, AnimatedGunItem animatable, GeoBone bone,
                                          RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                          boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                          float red, float green, float blue, float alpha) {

                if (bone.getName().equals("muzzle_flash")) {
                    String gunKey = currentStack != null ? currentStack.getItem().toString() : "unknown";

                    if (debuggedGuns.add(gunKey)) {
                        float px = bone.getPivotX();
                        float py = bone.getPivotY();
                        float pz = bone.getPivotZ();

                        System.out.println("[AND Debug] " + gunKey + " → muzzle_flash local pivot: " +
                                String.format("x=%.4f  y=%.4f  z=%.4f", px, py, pz));

                        var matrix = poseStack.last().pose();
                        org.joml.Vector4f pos = new org.joml.Vector4f(0, 0, 0, 1);
                        matrix.transform(pos);

                        System.out.println("[AND Debug] " + gunKey + " → after PoseStack: " +
                                String.format("x=%.4f  y=%.4f  z=%.4f", pos.x, pos.y, pos.z));
                    }

                    renderMuzzleFlashOnBone(poseStack, bufferSource, partialTick);
                }

                super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer,
                        isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
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

    private void renderMuzzleFlashOnBone(PoseStack poseStack, MultiBufferSource bufferSource, float partialTick) {
        if (currentStack == null || !(currentStack.getItem() instanceof GunItem))
            return;

        if (currentEntity == null || !GunRenderingHandler.entityIdForMuzzleFlash.contains(currentEntity.getId()))
            return;

        if (!ShootingHandler.get().isShooting() || GunModifierHelper.isSilencedFire(currentStack))
            return;

        Gun modifiedGun = ((GunItem) currentStack.getItem()).getModifiedGun(currentStack);

        float random = GunRenderingHandler.entityIdToRandomValue.getOrDefault(currentEntity.getId(), 0.5f);
        boolean flip = random >= 0.5f;

        poseStack.pushPose();

        poseStack.translate(0.0f, 0.08f, 0.0f);

        poseStack.mulPose(Axis.ZP.rotationDegrees(360f * random));
        if (flip) {
            poseStack.mulPose(Axis.XP.rotationDegrees(180f));
        }

        float size = 0.75f;
        if (modifiedGun.getDisplay().getFlash() != null) {
            size = (float) modifiedGun.getDisplay().getFlash().getSize();
        }
        float scale = size * (0.6f + 0.4f * partialTick);
        scale *= (float) GunModifierHelper.getMuzzleFlashScale(currentStack, 1.0f);
        poseStack.scale(scale, scale, scale);

        poseStack.translate(-0.5f, -0.5f, 0.0f);

        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer builder = bufferSource.getBuffer(GunRenderType.getMuzzleFlash());

        float minU = currentStack.isEnchanted() ? 0.5f : 0.0f;
        float maxU = currentStack.isEnchanted() ? 1.0f : 0.5f;

        builder.vertex(matrix, 0, 0, 0).color(1f, 1f, 1f, 1f).uv(maxU, 1f).uv2(15728880).endVertex();
        builder.vertex(matrix, 1, 0, 0).color(1f, 1f, 1f, 1f).uv(minU, 1f).uv2(15728880).endVertex();
        builder.vertex(matrix, 1, 1, 0).color(1f, 1f, 1f, 1f).uv(minU, 0f).uv2(15728880).endVertex();
        builder.vertex(matrix, 0, 1, 0).color(1f, 1f, 1f, 1f).uv(maxU, 0f).uv2(15728880).endVertex();

        poseStack.popPose();
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

        this.currentStack = stack;
        this.currentContext = context;
        this.currentEntity = Minecraft.getInstance().player;

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