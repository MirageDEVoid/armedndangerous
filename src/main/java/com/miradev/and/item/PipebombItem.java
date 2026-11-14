package com.miradev.and.item;

import com.miradev.and.entity.ThrowablePipebombEntity;
import com.miradev.and.init.ModEntities;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import ttv.migami.jeg.entity.throwable.ThrowableGrenadeEntity;
import ttv.migami.jeg.item.GrenadeItem;

public class PipebombItem extends GrenadeItem {
    public PipebombItem(Properties properties, int maxCookTime) {
        super(properties, maxCookTime);
    }

    @Override
    public ThrowableGrenadeEntity create(ItemStack stack, Level world, LivingEntity entity, int timeLeft) {
        return new ThrowablePipebombEntity(ModEntities.THROWABLE_PIPEBOMB.get(), world, entity, timeLeft);
    }
}