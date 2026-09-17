package com.miradev.and.init;

import com.miradev.and.item.ANDGunItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import ttv.migami.jeg.item.AmmoItem;
import ttv.migami.jeg.item.AnimatedGunItem;

import static net.minecraftforge.registries.ForgeRegistries.ITEMS;

public class ModItems {
    public static final DeferredRegister<Item> REGISTER = DeferredRegister.create(ITEMS, "and");

    public static final RegistryObject<AnimatedGunItem> GIFT_OF_FIRE;
    public static final RegistryObject<AnimatedGunItem> RANGER_SMG;
    public static final RegistryObject<AnimatedGunItem> RAPID_SMG;
    public static final RegistryObject<AnimatedGunItem> MAGNUM_PISTOL;
    public static final RegistryObject<AnimatedGunItem> VETERINARY_PISTOL;

    public static final RegistryObject<AmmoItem> HEAVY_RIFLE_AMMO;
    public static final RegistryObject<Item> SUPPLY_STAMP;

    static {
        GIFT_OF_FIRE = gun("gift_of_fire", new Item.Properties().stacksTo(1).durability(1200).rarity(ModRarities.MYTHIC));

        RANGER_SMG = gun("ranger_smg", new Item.Properties().stacksTo(1).durability(1120).rarity(Rarity.UNCOMMON));
        RAPID_SMG = gun("rapid_smg", new Item.Properties().stacksTo(1).durability(720).rarity(Rarity.UNCOMMON));
        MAGNUM_PISTOL = gun("magnum_pistol", new Item.Properties().stacksTo(1).durability(480).rarity(Rarity.UNCOMMON));
        VETERINARY_PISTOL = gun("veterinary_pistol", new Item.Properties().stacksTo(1).durability(920).rarity(Rarity.UNCOMMON));

        HEAVY_RIFLE_AMMO = ammo("heavy_rifle_ammo", new Item.Properties().stacksTo(16));

        SUPPLY_STAMP = item("supply_stamp", new Item.Properties().stacksTo(64));
    }

    private static RegistryObject<AnimatedGunItem> gun(String name, Item.Properties properties) {
        return REGISTER.register(name, () -> new ANDGunItem(properties, name));
    }
    private static RegistryObject<AmmoItem> ammo(String name, Item.Properties properties) {
        return REGISTER.register(name, () -> new AmmoItem(properties));
    }
    private static RegistryObject<Item> item(String name, Item.Properties properties) {
        return REGISTER.register(name, () -> new Item(properties));
    }
}