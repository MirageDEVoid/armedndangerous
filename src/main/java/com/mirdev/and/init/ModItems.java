package com.mirdev.and.init;

import com.mirdev.and.item.ANDGunItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ttv.migami.jeg.item.AnimatedGunItem;

public class ModItems {
    public static final DeferredRegister<Item> REGISTER;

    public static final RegistryObject<AnimatedGunItem> RANGER_SMG;
    public static final RegistryObject<AnimatedGunItem> RAPID_SMG;

    static {
        REGISTER = DeferredRegister.create(ForgeRegistries.ITEMS, "and");

        RANGER_SMG = gun("ranger_smg", new Item.Properties().stacksTo(1).durability(640).rarity(Rarity.UNCOMMON));
        RAPID_SMG = gun("rapid_smg", new Item.Properties().stacksTo(1).durability(880).rarity(Rarity.UNCOMMON));
    }

    private static RegistryObject<AnimatedGunItem> gun(String name, Item.Properties properties) {
        return REGISTER.register(name, () -> new ANDGunItem(properties, name));
    }
}