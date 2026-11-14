package com.miradev.and.armor;

import com.google.common.base.Suppliers;
import com.miradev.and.Reference;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;

public enum ANDArmorMaterials implements ArmorMaterial {
    SCRAP(Reference.MOD_ID + ":roadsign_test", 25, new int[]{2, 5, 6, 2}, 12,
            SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 0.0F,
            () -> Ingredient.of(ttv.migami.jeg.init.ModItems.SCRAP.get()));

    private static final int[] BASE_DURABILITY = {11, 16, 15, 13};
    private final String name;
    private final int durabilityMultiplier;
    private final int[] defense;
    private final int enchantValue;
    private final SoundEvent equipSound;
    private final float toughness;
    private final float knockbackResistance;
    private final Supplier<Ingredient> repair;

    ANDArmorMaterials(String name, int durabilityMultiplier, int[] defense, int enchantValue,
                      SoundEvent equipSound, float toughness, float knockbackResistance,
                      Supplier<Ingredient> repair) {
        this.name = name;
        this.durabilityMultiplier = durabilityMultiplier;
        this.defense = defense;
        this.enchantValue = enchantValue;
        this.equipSound = equipSound;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.repair = Suppliers.memoize(repair::get);
    }

    @Override public int getDurabilityForType(ArmorItem.Type t) { return BASE_DURABILITY[t.ordinal()] * durabilityMultiplier; }
    @Override public int getDefenseForType(ArmorItem.Type t) { return defense[t.ordinal()]; }
    @Override public int getEnchantmentValue() { return enchantValue; }
    @Override public SoundEvent getEquipSound() { return equipSound; }
    @Override public Ingredient getRepairIngredient() { return repair.get(); }
    @Override public String getName() { return name; }
    @Override public float getToughness() { return toughness; }
    @Override public float getKnockbackResistance() { return knockbackResistance; }
}