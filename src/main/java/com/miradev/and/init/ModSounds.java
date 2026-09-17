package com.miradev.and.init;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.miradev.and.Reference;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> REGISTER =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "and");

    public static final RegistryObject<SoundEvent> GIFT_OF_FIRE_FIRE =
            register("item.gift_of_fire.fire");
    public static final RegistryObject<SoundEvent> GIFT_OF_FIRE_BOLT_PULL =
            register("item.gift_of_fire.bolt_pull");
    public static final RegistryObject<SoundEvent> GIFT_OF_FIRE_BOLT_RELEASE =
            register("item.gift_of_fire.bolt_release");

    public static final RegistryObject<SoundEvent> MAGNUM_PISTOL_FIRE =
            register("item.magnum_pistol.fire");

    private static RegistryObject<SoundEvent> register(String key) {
        return REGISTER.register(key, () ->
                SoundEvent.createVariableRangeEvent(new ResourceLocation("and", key)));
    }
}