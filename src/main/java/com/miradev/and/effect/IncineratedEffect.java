package com.miradev.and.effect;

import com.miradev.and.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectCategory;

public class IncineratedEffect extends ScalingDamageEffect {
    public static final ResourceKey<DamageType> DAMAGE_TYPE =
            ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(Reference.MOD_ID, "incinerated"));

    // interval, base, step, multiplier, cap, visual fire
    private static final Params DEFAULTS = new Params(10, 1.0F, 2, 1.5F, 96.0F, true);

    public IncineratedEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF4500, DAMAGE_TYPE, () -> DEFAULTS);
    }
}