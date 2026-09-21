package com.miradev.and.mixin;

import com.miradev.and.animations.ANDGunAnimations;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationController;
import ttv.migami.jeg.item.AnimatedGunItem;

@Mixin(AnimatedGunItem.class)
public abstract class MixinAnimatedGunItem {

    @Redirect(
            method = "registerControllers(Lsoftware/bernie/geckolib/core/animation/AnimatableManager$ControllerRegistrar;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lttv/migami/jeg/animations/GunAnimations;animationController(Lttv/migami/jeg/item/AnimatedGunItem;)Lsoftware/bernie/geckolib/core/animation/AnimationController;"
            ),
            remap = false
    )
    private AnimationController<GeoAnimatable> and$replaceAnimationController(AnimatedGunItem item) {
        return ANDGunAnimations.animationController(item);
    }
}