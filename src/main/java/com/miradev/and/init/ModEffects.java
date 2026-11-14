package com.miradev.and.init;

import com.miradev.and.Reference;
import com.miradev.and.effect.IncineratedEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {
    public static final DeferredRegister<MobEffect> REGISTER =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Reference.MOD_ID);

    public static final RegistryObject<MobEffect> INCINERATED =
            REGISTER.register("incinerated", IncineratedEffect::new);
}