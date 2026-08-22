package com.miradev.and.init;

import com.miradev.and.item.ANDGunItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ttv.migami.jeg.item.AmmoItem;
import ttv.migami.jeg.item.AnimatedGunItem;

public class ModItems {
    public static final DeferredRegister<Item> REGISTER;

    public static final RegistryObject<AnimatedGunItem> RANGER_SMG;
    public static final RegistryObject<AnimatedGunItem> RAPID_SMG;
    public static final RegistryObject<AnimatedGunItem> MAGNUM_PISTOL;
    public static final RegistryObject<AnimatedGunItem> VETERINARY_PISTOL;

    public static final RegistryObject<AmmoItem> HEAVY_RIFLE_AMMO;

    static {
        REGISTER = DeferredRegister.create(ForgeRegistries.ITEMS, "and");

        RANGER_SMG = gun("ranger_smg", new Item.Properties().stacksTo(1).durability(1120).rarity(Rarity.UNCOMMON));
        RAPID_SMG = gun("rapid_smg", new Item.Properties().stacksTo(1).durability(720).rarity(Rarity.UNCOMMON));
        MAGNUM_PISTOL = gun("magnum_pistol", new Item.Properties().stacksTo(1).durability(480).rarity(Rarity.UNCOMMON));
        VETERINARY_PISTOL = gun("veterinary_pistol", new Item.Properties().stacksTo(1).durability(920).rarity(Rarity.UNCOMMON));

        HEAVY_RIFLE_AMMO = ammo("heavy_rifle_ammo", new Item.Properties().stacksTo(16));
    }

    private static RegistryObject<AnimatedGunItem> gun(String name, Item.Properties properties) {
        return REGISTER.register(name, () -> new ANDGunItem(properties, name));
    }
    private static RegistryObject<AmmoItem> ammo(String name, Item.Properties properties) {
        return REGISTER.register(name, () -> new AmmoItem(properties));
    }
}