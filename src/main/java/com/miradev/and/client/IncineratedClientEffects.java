package com.miradev.and.client;

import com.miradev.and.Reference;
import com.miradev.and.init.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class IncineratedClientEffects {

    private static final float SMOKE_CHANCE = 0.05F;
    private static final boolean SMOKE_IN_FIRST_PERSON = false;

    /** Ticks the local player has been continuously burning. Drives the overlay's ramp. Client only. */
    public static int burnTicks = 0;
    private static int clearDelay = 0;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused()) return;
        LocalPlayer p = mc.player;
        if (p != null && p.hasEffect(ModEffects.INCINERATED.get())) {
            burnTicks++;
            clearDelay = 0;
        } else if (++clearDelay > 10) {
            burnTicks = 0;
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity e = event.getEntity();
        Level level = e.level();
        if (!level.isClientSide || !e.isAlive()) return;
        if (!e.hasEffect(ModEffects.INCINERATED.get())) return;

        RandomSource rand = e.getRandom();
        if (rand.nextFloat() >= SMOKE_CHANCE) return;

        Minecraft mc = Minecraft.getInstance();
        if (!SMOKE_IN_FIRST_PERSON && e == mc.player && mc.options.getCameraType().isFirstPerson()) return;

        double x = e.getX() + (rand.nextDouble() - 0.5) * e.getBbWidth() * 0.8;
        double y = e.getY() + e.getBbHeight() * (0.5 + rand.nextDouble() * 0.5);
        double z = e.getZ() + (rand.nextDouble() - 0.5) * e.getBbWidth() * 0.8;
        level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0.0, 0.1, 0.0);
    }
}