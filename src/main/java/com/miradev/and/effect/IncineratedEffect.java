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
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.tickCount % 10 != 0) return;

        entity.setSecondsOnFire(1);
        entity.hurt(entity.damageSources().onFire(), 4.0f);
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        return new ArrayList<>();
    }
}