package com.miradev.and.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class IncineratedEffect extends MobEffect {

    public IncineratedEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF4500);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {

        entity.hurt(entity.damageSources().onFire(), 4.0f);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 10 == 0;
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        return new ArrayList<>();
    }
}