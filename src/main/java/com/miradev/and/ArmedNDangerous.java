package com.miradev.and;

import com.miradev.and.init.ModBlocks;
import com.miradev.and.init.ModEffects;
import com.miradev.and.init.ModItems;
import com.miradev.and.registry.ModCreativeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Reference.MOD_ID)
public class ArmedNDangerous {
    public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_ID);

    public ArmedNDangerous() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        MinecraftForge.EVENT_BUS.register(this);

        ModItems.REGISTER.register(bus);
        ModEffects.REGISTER.register(bus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(bus);

        bus.addListener(this::onCommonSetup);
        bus.addListener(this::onClientSetup);

        MinecraftForge.EVENT_BUS.addListener(this::onAddReloadListeners);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
        });
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
        });
    }

    private void onAddReloadListeners(AddReloadListenerEvent event) {
    }
}