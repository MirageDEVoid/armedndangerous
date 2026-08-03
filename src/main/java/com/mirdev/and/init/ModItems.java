package com.mirdev.and.init;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ttv.migami.jeg.item.AnimatedGunItem;

public class ModItems {
    public static final DeferredRegister<Item> REGISTER;

    public static final RegistryObject<AnimatedGunItem> RANGER_SMG;

    static {
        REGISTER = DeferredRegister.create(ForgeRegistries.ITEMS, "and");

        RANGER_SMG = REGISTER.register("ranger_smg",
                () -> new AnimatedGunItem((new Item.Properties())
                        .stacksTo(1)
                        .durability(972)
                        .rarity(Rarity.UNCOMMON), "ranger_smg"));
    }
}