package com.mirdev.and.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(
        modid = "and",
        value = {Dist.CLIENT},
        bus = Bus.MOD
)
public enum SpecialModels {
    RANGER_SMG("gun/ranger_smg");

    private final ResourceLocation modelLocation;
    private BakedModel cachedModel;

    private SpecialModels(String modelName) {
        this.modelLocation = new ResourceLocation("and", "special/" + modelName);
    }

    public BakedModel getModel() {
        if (this.cachedModel == null) {
            this.cachedModel = Minecraft.getInstance().getModelManager().getModel(this.modelLocation);
        }

        return this.cachedModel;
    }

    @SubscribeEvent
    public static void registerAdditional(ModelEvent.RegisterAdditional event) {
        for(SpecialModels model : values()) {
            event.register(model.modelLocation);
        }

    }

    @SubscribeEvent
    public static void onBake(ModelEvent.BakingCompleted event) {
        for(SpecialModels model : values()) {
            model.cachedModel = null;
        }

    }
}