package com.miradev.and.client.render;

import com.miradev.and.Reference;
import com.miradev.and.init.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ttv.migami.jeg.client.render.entity.ThrowableGrenadeRenderer;
import ttv.migami.jeg.entity.throwable.ThrowableGrenadeEntity;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ANDClientEntities {
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.<ThrowableGrenadeEntity>registerEntityRenderer(
                ModEntities.THROWABLE_PIPEBOMB.get(), ThrowableGrenadeRenderer::new);
    }
}
