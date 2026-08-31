package com.miradev.and.init;

import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> REGISTER =
            DeferredRegister.create(ForgeRegistries.BLOCKS, "and");

    private static <T extends Block> RegistryObject<T> registerBlock(String name, java.util.function.Supplier<T> block) {
        return REGISTER.register(name, block);
    }
}