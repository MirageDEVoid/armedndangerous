package com.miradev.and.client;

import com.miradev.and.Reference;
import com.miradev.and.network.S2CFragmentBurst;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class FragmentClient {
    private static final double SPEED = 2.0;
    private static final double STEP = 0.5;
    private static final int MAX_ACTIVE = Integer.MAX_VALUE;

    private static final List<Active> ACTIVE = new ArrayList<>();

    private FragmentClient() {}

    private static final class Active {
        final ClientLevel level;
        final DustParticleOptions dust;
        final Vec3[][] pts;
        final double[][] cum;
        final double[] total;
        final double[] speed;
        final byte[] kind;
        int age;

        Active(ClientLevel level, S2CFragmentBurst m) {
            this.level = level;
            float r = (m.color() >> 16 & 255) / 255F, g = (m.color() >> 8 & 255) / 255F, b = (m.color() & 255) / 255F;
            this.dust = new DustParticleOptions(new Vector3f(r, g, b), 0.6F);
            int n = m.rays().size();
            this.pts = new Vec3[n][];
            this.cum = new double[n][];
            this.total = new double[n];
            this.speed = new double[n];
            this.kind = new byte[n];
            Vec3 o = m.origin();
            for (int i = 0; i < n; i++) {
                S2CFragmentBurst.Ray ray = m.rays().get(i);
                float[] f = ray.points();
                int segs = f.length / 3;
                Vec3[] p = new Vec3[segs + 1];
                double[] c = new double[segs + 1];
                p[0] = o;
                for (int j = 0; j < segs; j++) {
                    p[j + 1] = o.add(f[j * 3], f[j * 3 + 1], f[j * 3 + 2]);
                    c[j + 1] = c[j] + p[j].distanceTo(p[j + 1]);
                }
                pts[i] = p;
                cum[i] = c;
                total[i] = c[segs];
                kind[i] = ray.kind();
                speed[i] = SPEED * (0.8 + 0.4 * level.random.nextDouble());
            }
        }
    }

    public static void play(S2CFragmentBurst m) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        RandomSource rand = level.random;
        Vec3 o = m.origin();

        level.addParticle(ParticleTypes.FLASH, true, o.x, o.y, o.z, 0, 0, 0);
        for (ResourceLocation id : m.particles()) {
            ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.get(id);
            if (type instanceof ParticleOptions opt) {
                for (int i = 0; i < 6; i++) {
                    level.addParticle(opt, true, o.x, o.y, o.z,
                            rand.nextGaussian() * 0.15, rand.nextGaussian() * 0.15, rand.nextGaussian() * 0.15);
                }
            }
        }

        Active a = new Active(level, m);
        if (ACTIVE.size() >= MAX_ACTIVE) ACTIVE.remove(0);
        if (advance(a)) ACTIVE.add(a);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ACTIVE.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) { ACTIVE.clear(); return; }
        if (mc.isPaused()) return;
        ACTIVE.removeIf(a -> a.level != mc.level || !advance(a));
    }

    private static Vec3 pointAt(Active a, int ray, double d) {
        double[] cum = a.cum[ray];
        Vec3[] pts = a.pts[ray];
        int j = 1;
        while (j < cum.length - 1 && d > cum[j]) j++;
        double segLen = cum[j] - cum[j - 1];
        double t = segLen < 1.0E-6 ? 0.0 : (d - cum[j - 1]) / segLen;
        return pts[j - 1].lerp(pts[j], Mth.clamp(t, 0.0, 1.0));
    }

    private static boolean advance(Active a) {
        RandomSource rand = a.level.random;
        boolean anyLeft = false;
        for (int i = 0; i < a.pts.length; i++) {
            double total = a.total[i];
            double from = a.age * a.speed[i];
            if (total < 1.0E-4 || from >= total) continue;
            double to = from + a.speed[i];
            double reach = Math.min(to, total);

            for (double d = (Math.floor(from / STEP) + 1) * STEP; d <= reach; d += STEP) {
                Vec3 pt = pointAt(a, i, d);
                a.level.addParticle(a.dust, true, pt.x, pt.y, pt.z, 0, 0, 0);
            }

            double[] cum = a.cum[i];
            for (int j = 1; j < cum.length - 1; j++) {
                if (cum[j] > from && cum[j] <= reach) {
                    Vec3 b = a.pts[i][j];
                    a.level.addParticle(ParticleTypes.ELECTRIC_SPARK, true, b.x, b.y, b.z,
                            rand.nextGaussian() * 0.1, rand.nextGaussian() * 0.1, rand.nextGaussian() * 0.1);
                }
            }

            if (to >= total) {
                Vec3 e = a.pts[i][a.pts[i].length - 1];
                switch (a.kind[i]) {
                    case 1 -> a.level.addParticle(ParticleTypes.ELECTRIC_SPARK, true, e.x, e.y, e.z,
                            rand.nextGaussian() * 0.1, rand.nextGaussian() * 0.1, rand.nextGaussian() * 0.1);
                    case 2 -> a.level.addParticle(ParticleTypes.CRIT, true, e.x, e.y, e.z,
                            rand.nextGaussian() * 0.15, rand.nextGaussian() * 0.15, rand.nextGaussian() * 0.15);
                    default -> {}
                }
            } else {
                anyLeft = true;
            }
        }
        a.age++;
        return anyLeft;
    }
}