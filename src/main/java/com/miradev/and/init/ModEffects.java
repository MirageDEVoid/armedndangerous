package com.miradev.and.init;

import com.miradev.and.effect.IncineratedEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {

    public static final DeferredRegister<MobEffect> REGISTER;

    public static final RegistryObject<MobEffect> INCINERATED;

    static {
        REGISTER = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, "and");

        INCINERATED = REGISTER.register("incinerated", IncineratedEffect::new);
    }
}