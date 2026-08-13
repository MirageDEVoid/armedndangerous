package com.mirdev.and.registry;

import com.mirdev.and.ArmedNDangerous;
import com.mirdev.and.init.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.core.registries.Registries;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ArmedNDangerous.MODID);

    public static final RegistryObject<CreativeModeTab> ARMEDNDANGEROUS_TAB = CREATIVE_MODE_TABS.register("and_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + ArmedNDangerous.MODID))
                    .icon(() -> new ItemStack(ModItems.MAGNUM_PISTOL.get()))
                    .withSearchBar()
                    .displayItems((parameters, output) -> {
                        ModItems.REGISTER.getEntries().stream()
                                .map(RegistryObject::get)
                                .forEach(output::accept);
                    })
                    .build()
    );
}