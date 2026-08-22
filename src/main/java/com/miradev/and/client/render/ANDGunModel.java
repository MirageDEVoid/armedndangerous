package com.miradev.and.client.render;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import ttv.migami.jeg.item.AnimatedGunItem;

public class ANDGunModel extends GeoModel<AnimatedGunItem> {

    private final ResourceLocation model;
    private final ResourceLocation texture;
    private final ResourceLocation animation;

    public ANDGunModel(ResourceLocation model, ResourceLocation texture, ResourceLocation animation) {
        this.model     = model;
        this.texture   = texture;
        this.animation = animation;
    }

    @Override public ResourceLocation getModelResource(AnimatedGunItem a)     { return model;     }
    @Override public ResourceLocation getTextureResource(AnimatedGunItem a)   { return texture;   }
    @Override public ResourceLocation getAnimationResource(AnimatedGunItem a) { return animation; }

    @Override
    public void handleAnimations(AnimatedGunItem animatable, long instanceId,
                                 AnimationState<AnimatedGunItem> animationState) {
    }
}