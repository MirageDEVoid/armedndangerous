package com.miradev.and.client.render;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import ttv.migami.jeg.item.AnimatedGunItem;

public class ANDGunModel extends GeoModel<AnimatedGunItem> {

    private final ResourceLocation defaultModel;
    private final ResourceLocation defaultTexture;
    private final ResourceLocation defaultAnimation;

    private ResourceLocation currentModel;
    private ResourceLocation currentTexture;
    private ResourceLocation currentAnimation;

    public ANDGunModel(ResourceLocation model, ResourceLocation texture, ResourceLocation animation) {
        this.defaultModel     = model;
        this.defaultTexture   = texture;
        this.defaultAnimation = animation;
    }

    @Override
    public ResourceLocation getModelResource(AnimatedGunItem a) {
        return currentModel != null ? currentModel : defaultModel;
    }

    @Override
    public ResourceLocation getTextureResource(AnimatedGunItem a) {
        return currentTexture != null ? currentTexture : defaultTexture;
    }

    @Override
    public ResourceLocation getAnimationResource(AnimatedGunItem a) {
        return currentAnimation != null ? currentAnimation : defaultAnimation;
    }

    @Override
    public void handleAnimations(AnimatedGunItem animatable, long instanceId,
                                 AnimationState<AnimatedGunItem> animationState) {
    }

    public void setCurrentTexture(ResourceLocation texture) {
        this.currentTexture = texture;
    }

    public void setCurrentModel(ResourceLocation model) {
        this.currentModel = model;
    }

    public void setCurrentAnimation(ResourceLocation animation) {
        this.currentAnimation = animation;
    }
}