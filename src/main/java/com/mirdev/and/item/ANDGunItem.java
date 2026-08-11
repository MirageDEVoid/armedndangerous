package com.mirdev.and.item;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import com.mirdev.and.client.render.ANDGunModel;
import com.mirdev.and.client.render.ANDGunRenderer;
import ttv.migami.jeg.item.AnimatedGunItem;

import java.util.function.Consumer;

public class ANDGunItem extends AnimatedGunItem {

    private final String gunName;

    public ANDGunItem(Properties properties, String gunName) {
        super(properties, gunName);
        this.gunName = gunName;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private ANDGunRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    ANDGunModel tpModel = new ANDGunModel(
                            new ResourceLocation("and", "geo/item/" + gunName + "_tp.geo.json"),
                            new ResourceLocation("and", "textures/animated/gun/" + gunName + ".png"),
                            new ResourceLocation("and", "animations/item/" + gunName + ".animation.json")
                    );
                    renderer = new ANDGunRenderer(
                            new ResourceLocation("and", gunName),
                            tpModel
                    );
                }
                return renderer;
            }
        });
    }
}