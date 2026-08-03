package com.mirdev.and;

import com.mirdev.and.init.ModItems;
import com.mirdev.and.registry.ModCreativeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ArmedNDangerous.MODID)
public class ArmedNDangerous {

    public static final String MODID = "and";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public ArmedNDangerous() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        MinecraftForge.EVENT_BUS.register(this);

        ModItems.REGISTER.register(bus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(bus);

        bus.addListener(this::onCommonSetup);
        bus.addListener(this::onClientSetup);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
        });
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
        });
    }
}