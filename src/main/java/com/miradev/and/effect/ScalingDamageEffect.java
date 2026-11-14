package com.miradev.and.effect;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class ScalingDamageEffect extends MobEffect {

    public record Params(int interval, float baseDamage, int stepEvery, float multiplier,
                         float maxDamage, boolean igniteVictim) {
        public float damageFor(int pulse) {
            float dmg = baseDamage * (float) Math.pow(multiplier, pulse / Math.max(stepEvery, 1));
            return Math.min(dmg, maxDamage);
        }
    }

    private final Supplier<Params> params;
    private final ResourceKey<DamageType> damageType;

    public ScalingDamageEffect(MobEffectCategory category, int color,
                               ResourceKey<DamageType> damageType, Supplier<Params> params) {
        super(category, color);
        this.damageType = damageType;
        this.params = params;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) { return true; }

    @Override
    public void applyEffectTick(LivingEntity victim, int amplifier) {
        Level level = victim.level();
        if (level.isClientSide) return;
        Params p = this.params.get();
        if (victim.tickCount % p.interval() != 0) return;

        String key = dataKey();
        CompoundTag tag = victim.getPersistentData().getCompound(key);
        long now = level.getGameTime();

        int pulse = (now - tag.getLong("LastPulse") > p.interval() * 2L) ? 0 : tag.getInt("Pulse");
        tag.putInt("Pulse", pulse + 1);
        tag.putLong("LastPulse", now);
        victim.getPersistentData().put(key, tag);

        Entity attacker = null;
        if (tag.hasUUID("Source") && level instanceof ServerLevel server) {
            attacker = server.getEntity(tag.getUUID("Source"));
        }

        Vec3 motion = victim.getDeltaMovement();
        victim.hurt(source(level, attacker), p.damageFor(pulse));
        victim.setDeltaMovement(motion);
        if (p.igniteVictim()) victim.setSecondsOnFire(1);
    }

    public void setSource(LivingEntity victim, UUID source) {
        String key = dataKey();
        CompoundTag tag = victim.getPersistentData().getCompound(key);
        tag.putUUID("Source", source);
        victim.getPersistentData().put(key, tag);
    }

    private String dataKey() { return "and_fx_" + ForgeRegistries.MOB_EFFECTS.getKey(this); }

    private DamageSource source(Level level, @Nullable Entity attacker) {
        Holder<DamageType> type = level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(this.damageType);
        return new DamageSource(type, null, attacker);
    }

    @Override
    public List<ItemStack> getCurativeItems() { return new ArrayList<>(); }
}
