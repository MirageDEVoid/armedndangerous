package com.miradev.and.client.render;

import com.miradev.and.Reference;
import com.miradev.and.item.ANDArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class ANDArmorRenderer extends GeoArmorRenderer<ANDArmorItem> {
    public ANDArmorRenderer() {
        super(new GeoModel<>() {
            @Override public ResourceLocation getModelResource(ANDArmorItem a) {
                return new ResourceLocation(Reference.MOD_ID, "geo/armor/roadsign_test.geo.json");
            }
            @Override public ResourceLocation getTextureResource(ANDArmorItem a) {
                return new ResourceLocation(Reference.MOD_ID, "textures/armor/roadsign_test.png");
            }
            @Override public ResourceLocation getAnimationResource(ANDArmorItem a) {
                return new ResourceLocation(Reference.MOD_ID, "animations/armor/roadsign_test.animation.json");
            }
        });
    }
}
