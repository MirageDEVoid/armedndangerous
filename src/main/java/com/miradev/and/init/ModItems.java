package com.miradev.and.init;

import com.miradev.and.armor.ANDArmorMaterials;
import com.miradev.and.item.ANDArmorItem;
import com.miradev.and.item.ANDGunItem;
import com.miradev.and.item.PipebombItem;
import net.minecraft.world.item.ArmorItem;
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
    public static final RegistryObject<AnimatedGunItem> TRAPDOOR_RIFLE;
    public static final RegistryObject<AnimatedGunItem> PUMP_ACTION_CARBINE;

    public static final RegistryObject<AnimatedGunItem> RANGER_SMG;
    public static final RegistryObject<AnimatedGunItem> RAPID_SMG;
    public static final RegistryObject<AnimatedGunItem> NAILGUN;

    public static final RegistryObject<AnimatedGunItem> MAGNUM_PISTOL;
    public static final RegistryObject<AnimatedGunItem> VETERINARY_PISTOL;

    public static final RegistryObject<AmmoItem> HEAVY_RIFLE_AMMO;
    public static final RegistryObject<Item> SUPPLY_STAMP;

    public static final RegistryObject<Item> PIPEBOMB;

    public static final RegistryObject<Item> TEST_CHESTPLATE;

    static {
        GIFT_OF_FIRE = gun("gift_of_fire", new Item.Properties().stacksTo(1).durability(1200).rarity(ModRarities.MYTHIC));
        TRAPDOOR_RIFLE = gun("trapdoor_rifle", new Item.Properties().stacksTo(1).durability(240));
        PUMP_ACTION_CARBINE = gun("pump_action_carbine", new Item.Properties().stacksTo(1).durability(360));

        RANGER_SMG = gun("ranger_smg", new Item.Properties().stacksTo(1).durability(1120).rarity(Rarity.UNCOMMON));
        RAPID_SMG = gun("rapid_smg", new Item.Properties().stacksTo(1).durability(720).rarity(Rarity.UNCOMMON));
        NAILGUN = gun("nailgun", new Item.Properties().stacksTo(1).durability(320));

        MAGNUM_PISTOL = gun("magnum_pistol", new Item.Properties().stacksTo(1).durability(480).rarity(Rarity.UNCOMMON));
        VETERINARY_PISTOL = gun("veterinary_pistol", new Item.Properties().stacksTo(1).durability(920).rarity(Rarity.UNCOMMON));

        HEAVY_RIFLE_AMMO = ammo("heavy_rifle_ammo", new Item.Properties().stacksTo(16));

        SUPPLY_STAMP = item("supply_stamp", new Item.Properties().stacksTo(64));

        PIPEBOMB = REGISTER.register("pipebomb", () -> new PipebombItem(new Item.Properties().stacksTo(16), 20 * 3));

        TEST_CHESTPLATE = REGISTER.register("test_chestplate", () -> new ANDArmorItem(ANDArmorMaterials.SCRAP, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
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