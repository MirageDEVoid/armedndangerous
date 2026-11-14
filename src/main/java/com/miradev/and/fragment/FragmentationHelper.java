package com.miradev.and.fragment;

import com.miradev.and.common.FragmentProfile;
import com.miradev.and.common.ResidueProfile;
import com.miradev.and.network.ANDNetwork;
import com.miradev.and.network.S2CFragmentBurst;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.init.ModTags;
import ttv.migami.jeg.network.PacketHandler;
import ttv.migami.jeg.network.message.S2CMessageBlood;
import ttv.migami.jeg.network.message.S2CMessageProjectileHitEntity;

import javax.annotation.Nullable;
import java.util.*;

public final class FragmentationHelper {
    private static final int MAX_TARGETS = 48;
    private static final int MAX_RAYS_PER_TARGET = 32;
    private static final int AIMED_RAY_BUDGET = 512;
    private static final int MIN_WILD_RAYS = 48;
    private static final int MAX_WILD_RAYS = 192;
    private static final int MAX_AIMED_TRACERS = 40;
    private static final int MAX_TRACERS = 64;
    private static final double SURFACE_PROBE = 0.6;
    private static final int IMPACT_SOUNDS = 6;
    private static final float IMPACT_VOLUME = 1.0F;

    private FragmentationHelper() {}

    public enum Kind { AIR, BLOCK, ENTITY }
    public record Tracer(List<Vec3> points, Kind kind) {}
    public record Result(List<Tracer> tracers, int entitiesHit, double totalDamage, int fragileBroken) {}

    private static final class Stats {
        int fragileBroken;
        final Map<BlockPos, Vec3> impacts = new LinkedHashMap<>();
        void impact(BlockPos pos, Vec3 at) { if (impacts.size() < 64) impacts.putIfAbsent(pos.immutable(), at); }
    }

    private static final class Target {
        final Entity entity;
        final AABB core; // aimed frags point to hitbox, cover check for ts
        final AABB box; // frag radius inflate
        final double dist;
        final double radius;
        int rays;
        double weight = 1.0;

        Target(Entity entity, double inflate, Vec3 origin) {
            this.entity = entity;
            this.core = entity.getBoundingBox();
            this.box = core.inflate(inflate);
            this.dist = box.getCenter().distanceTo(origin);
            this.radius = 0.5 * Math.sqrt(box.getXsize() * box.getXsize()
                    + box.getYsize() * box.getYsize() + box.getZsize() * box.getZsize());
        }
    }

    private record Hit(Target target, double dist) {}

    public static Vec3 originFor(Entity bomb) {
        return bomb.getBoundingBox().getCenter().add(0, 0.25, 0);
    }

    public static Result burst(ServerLevel level, Vec3 origin, @Nullable Vec3 forward,
                               @Nullable Entity owner, @Nullable Entity direct, FragmentProfile p) {
        RandomSource rand = level.random;
        ResourceLocation advantage = FragmentRules.effectiveAdvantage(p.advantage());
        Stats stats = new Stats();

        // shape
        Vec3 axis = forward != null && forward.lengthSqr() > 1.0E-6 ? forward.normalize() : new Vec3(0, 1, 0);
        double halfDeg = p.shape().halfAngle();
        double acceptDeg = halfDeg;
        if (forward == null && halfDeg >= 180.0) {
            Vec3 n = surfaceNormal(level, origin);
            if (n != null) { axis = n; halfDeg = 100.0; acceptDeg = 130.0; }
        }
        double cosHalf = Math.cos(Math.toRadians(halfDeg));
        double acceptRad = Math.toRadians(acceptDeg);
        double cosAccept = Math.cos(acceptRad);

        double selfScale = FragmentConfig.SELF_DAMAGE_PERCENT.get() / 100.0;
        boolean ownerCanBeHit = p.affectsOwner() && selfScale > 0;
        boolean breakFragile = p.breaksFragile() && FragmentConfig.FRAGMENTS_BREAK_FRAGILE.get();

        // 1. candidates for frags to hit
        List<Target> all = new ArrayList<>();
        AABB area = new AABB(origin, origin).inflate(p.range() + p.hitRadius());
        for (Entity e : level.getEntities((Entity) null, area, FragmentRules.TARGETS)) {
            if (e == direct) continue;
            if (e == owner && !ownerCanBeHit) continue;
            if (FragmentRules.ignores(e, owner, advantage)) continue;
            if (!FragmentRules.pvpAllows(e, owner)) continue;
            Target t = new Target(e, p.hitRadius(), origin);
            if (t.dist - t.radius > p.range()) continue;
            all.add(t);
        }
        all.sort(Comparator.comparingDouble(t -> t.dist));
        if (all.size() > MAX_TARGETS) all = new ArrayList<>(all.subList(0, MAX_TARGETS));

        // 2. aimable frag, ignore owner(wild still hit)
        List<Target> aimable = new ArrayList<>();
        for (Target t : all) {
            if (t.entity == owner) continue;
            if (acceptDeg < 180.0 && t.dist > 1.0E-4) {
                double cos = Mth.clamp(t.box.getCenter().subtract(origin).normalize().dot(axis), -1.0, 1.0);
                if (Math.acos(cos) > acceptRad + Math.atan2(t.radius, t.dist)) continue;
            }
            if (!exposed(level, origin, t.entity)) continue;
            aimable.add(t);
        }

        // 3. budget split for frags, aimed and wild
        double aimedBudget = p.count() * p.aimBias();
        double perTarget = 0.0;
        double leftover = aimedBudget;
        if (!aimable.isEmpty()) {
            perTarget = Math.min(aimedBudget / aimable.size(), p.maxAimed());
            leftover = aimedBudget - perTarget * aimable.size();
        }
        double wildBudget = p.count() * (1.0 - p.aimBias()) + leftover;

        int totalRays = 0;
        for (Target t : aimable) {
            int n = stochasticRound(perTarget, rand);
            if (n <= 0) continue;
            t.rays = Math.min(n, MAX_RAYS_PER_TARGET);
            t.weight = (double) n / t.rays;
            totalRays += t.rays;
        }
        if (totalRays > AIMED_RAY_BUDGET) {
            double f = (double) AIMED_RAY_BUDGET / totalRays;
            for (Target t : aimable) {
                if (t.rays <= 0) continue;
                int scaled = Math.max(1, (int) (t.rays * f));
                t.weight *= (double) t.rays / scaled;
                t.rays = scaled;
            }
        }

        // 4. aimed frags
        Map<Entity, Double> damage = new LinkedHashMap<>();
        List<Tracer> tracers = new ArrayList<>();
        for (Target t : aimable) {
            for (int i = 0; i < t.rays; i++) {
                Vec3 aim = new Vec3(Mth.lerp(rand.nextDouble(), t.core.minX, t.core.maxX),
                        Mth.lerp(rand.nextDouble(), t.core.minY, t.core.maxY),
                        Mth.lerp(rand.nextDouble(), t.core.minZ, t.core.maxZ));
                Vec3 dir = aim.subtract(origin);
                if (dir.lengthSqr() < 1.0E-8) continue;
                dir = dir.normalize();
                if (dir.dot(axis) < cosAccept - 1.0E-9) continue;
                Tracer tr = castFragment(level, origin, dir, owner, p, breakFragile, all, damage, t.weight, stats);
                if (tr != null && tracers.size() < MAX_AIMED_TRACERS) tracers.add(tr);
            }
        }

        // 5. wild frags physic
        int wildRays = wildBudget < 0.5 ? 0 : Mth.clamp((int) Math.round(wildBudget), MIN_WILD_RAYS, MAX_WILD_RAYS);
        double wildWeight = wildRays == 0 ? 0.0 : wildBudget / wildRays;
        int stride = Math.max(1, wildRays / 24);
        double phase = rand.nextDouble();
        for (int i = 0; i < wildRays; i++) {
            Vec3 dir = latticeDir(axis, cosHalf, i, wildRays, phase);
            Tracer tr = castFragment(level, origin, dir, owner, p, breakFragile, all, damage, wildWeight, stats);
            if (tr != null && i % stride == 0 && tracers.size() < MAX_TRACERS) tracers.add(tr);
        }
        playImpactSounds(level, stats, rand);

        // 6. hurt entity once
        LivingEntity ownerLiving = owner instanceof LivingEntity le ? le : null;
        Difficulty difficulty = level.getDifficulty();
        int hurtCount = 0;
        double totalDamage = 0;
        for (Map.Entry<Entity, Double> entry : damage.entrySet()) {
            Entity target = entry.getKey();
            double dmg = entry.getValue() * FragmentRules.advantageMultiplier(target, advantage);
            if (target == owner) dmg *= selfScale;
            if (owner != null && !(owner instanceof Player)) dmg /= difficulty != Difficulty.HARD ? 2.0 : 1.5;
            if (dmg <= 0.0) continue;
            if (applyDamage(level, target, owner, ownerLiving, direct, dmg, advantage, p)) {
                hurtCount++;
                totalDamage += dmg;
            }
        }

        // 7. feedback
        sendBurst(level, origin, tracers, p);
        if (FragmentConfig.DEBUG_FEEDBACK.get() && owner instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.literal(String.format(Locale.ROOT,
                    "Frags: %d hit | %.1f dmg | aimed %.0f over %d targets | %d wild rays | %d fragile",
                    hurtCount, totalDamage, perTarget * aimable.size(), aimable.size(), wildRays,
                    stats.fragileBroken)), true);
        }
        return new Result(tracers, hurtCount, totalDamage, stats.fragileBroken);
    }

    @Nullable
    private static Tracer castFragment(ServerLevel level, Vec3 origin, Vec3 dir0, @Nullable Entity owner,
                                       FragmentProfile p, boolean breakFragile, List<Target> all,
                                       Map<Entity, Double> damage, double weight0, Stats stats) {
        List<Vec3> points = new ArrayList<>(p.bounce() + 1);
        Set<Entity> already = p.bounce() > 0 ? new HashSet<>() : null;   // one frag cant hit twice
        Vec3 pos = origin;
        Vec3 dir = dir0;
        double travelled = 0.0;
        double weight = weight0;
        int entityBudget = p.pierce() + 1;
        Kind kind = Kind.AIR;

        for (int seg = 0; seg <= p.bounce(); seg++) {
            double remaining = p.range() - travelled;
            if (remaining < 0.3) break;
            Vec3 end = pos.add(dir.scale(remaining));
            kind = Kind.AIR;

            BlockHitResult block = FragmentRules.clip(level, pos, end);
            boolean blocked = block.getType() != HitResult.Type.MISS;
            boolean fragile = false;
            if (blocked) {
                end = block.getLocation();
                kind = Kind.BLOCK;
                fragile = level.getBlockState(block.getBlockPos()).is(ModTags.Blocks.FRAGILE);
                if (breakFragile && FragmentRules.tryBreakFragile(level, block.getBlockPos(), owner, weight)) {
                    stats.fragileBroken++;
                } else {
                    stats.impact(block.getBlockPos(), block.getLocation());
                }
            }

            List<Hit> hits = new ArrayList<>();
            for (Target c : all) {
                if (already != null && already.contains(c.entity)) continue;
                if (c.box.contains(pos)) {
                    hits.add(new Hit(c, 0.0));
                    continue;
                }
                Optional<Vec3> hp = c.box.clip(pos, end);
                if (hp.isPresent()) hits.add(new Hit(c, pos.distanceTo(hp.get())));
            }
            hits.sort(Comparator.comparingDouble(Hit::dist));

            double segLen = pos.distanceTo(end);
            boolean stoppedByEntity = false;
            for (Hit h : hits) {
                double pathDist = travelled + h.dist();
                double falloff = 1.0 - (1.0 - p.falloff()) * Math.min(1.0, pathDist / p.range());
                damage.merge(h.target().entity, p.damage() * weight * falloff, Double::sum);
                if (already != null) already.add(h.target().entity);
                if (--entityBudget <= 0) {
                    segLen = h.dist();
                    end = pos.add(dir.scale(segLen));
                    kind = Kind.ENTITY;
                    stoppedByEntity = true;
                    break;
                }
            }
            points.add(end);
            if (stoppedByEntity) break;
            if (!blocked || fragile || seg == p.bounce()) break;

            Direction face = block.getDirection();
            Vec3 normal = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
            dir = dir.subtract(normal.scale(2.0 * dir.dot(normal))).normalize();
            pos = end.add(normal.scale(0.02));
            travelled += segLen;
            weight *= p.bounceLoss();
            if (weight <= 1.0E-4) break;
        }

        if (points.isEmpty()) return null;
        Vec3 last = points.get(points.size() - 1);
        if (points.size() == 1 && last.distanceToSqr(origin) <= 0.04) return null;
        return new Tracer(points, kind);
    }

    private static boolean exposed(ServerLevel level, Vec3 origin, Entity e) {
        AABB b = e.getBoundingBox();
        double cx = (b.minX + b.maxX) / 2.0, cz = (b.minZ + b.maxZ) / 2.0;
        double[] ys = { b.minY + 0.1 * b.getYsize(), (b.minY + b.maxY) / 2.0, b.minY + 0.9 * b.getYsize() };
        for (double y : ys) {
            if (FragmentRules.clip(level, origin, new Vec3(cx, y, cz)).getType() == HitResult.Type.MISS) return true;
        }
        return false;
    }

    @Nullable
    private static Vec3 surfaceNormal(ServerLevel level, Vec3 origin) {
        Vec3 sum = Vec3.ZERO;
        for (Direction d : Direction.values()) {
            Vec3 step = new Vec3(d.getStepX(), d.getStepY(), d.getStepZ());
            BlockHitResult hit = FragmentRules.clip(level, origin, origin.add(step.scale(SURFACE_PROBE)));
            if (hit.getType() != HitResult.Type.MISS) sum = sum.subtract(step);
        }
        return sum.lengthSqr() < 1.0E-6 ? null : sum.normalize();
    }

    private static void playImpactSounds(ServerLevel level, Stats stats, RandomSource rand) {
        if (stats.impacts.isEmpty()) return;
        List<Map.Entry<BlockPos, Vec3>> list = new ArrayList<>(stats.impacts.entrySet());
        Collections.shuffle(list, new Random(rand.nextLong()));
        int n = Math.min(IMPACT_SOUNDS, list.size());
        for (int i = 0; i < n; i++) {
            BlockState state = level.getBlockState(list.get(i).getKey());
            if (state.isAir()) continue;
            SoundType type = state.getSoundType();
            Vec3 at = list.get(i).getValue();
            level.playSound(null, at.x, at.y, at.z, type.getBreakSound(), SoundSource.BLOCKS,
                    IMPACT_VOLUME, (1.2F + rand.nextFloat() * 0.4F) * type.getPitch());
        }
    }

    private static void sendBurst(ServerLevel level, Vec3 origin, List<Tracer> tracers, FragmentProfile p) {
        if (tracers.isEmpty()) return;
        List<S2CFragmentBurst.Ray> rays = new ArrayList<>(tracers.size());
        for (Tracer t : tracers) {
            float[] pts = new float[t.points().size() * 3];
            for (int i = 0; i < t.points().size(); i++) {
                Vec3 pt = t.points().get(i);
                pts[i * 3] = (float) (pt.x - origin.x);
                pts[i * 3 + 1] = (float) (pt.y - origin.y);
                pts[i * 3 + 2] = (float) (pt.z - origin.z);
            }
            rays.add(new S2CFragmentBurst.Ray((byte) t.kind().ordinal(), pts));
        }
        ANDNetwork.CHANNEL.send(PacketDistributor.NEAR.with(() ->
                        new PacketDistributor.TargetPoint(origin.x, origin.y, origin.z, 96.0, level.dimension())),
                new S2CFragmentBurst(origin, p.visual().color(), p.visual().particles(), rays));
    }

    private static boolean applyDamage(ServerLevel level, Entity target, @Nullable Entity owner,
                                       @Nullable LivingEntity ownerLiving, @Nullable Entity direct,
                                       double dmg, ResourceLocation advantage, FragmentProfile p) {
        Vec3 velocityBefore = target.getDeltaMovement();
        target.invulnerableTime = 0;
        boolean hurt = target.hurt(FragmentRules.damageSource(level, direct, ownerLiving, target), (float) dmg);
        if (!Config.COMMON.gameplay.enableKnockback.get()) target.setDeltaMovement(velocityBefore);
        if (!hurt) return false;

        FragmentRules.advantageSideEffect(target, advantage);

        Vec3 c = target.getBoundingBox().getCenter();
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY(), target.getZ(),
                Mth.clamp((int) dmg / 2, 1, 20), target.getBbWidth() / 2, target.getBbHeight() / 2,
                target.getBbWidth() / 2, 0.1);

        if (target instanceof LivingEntity living) {
            if (p.effect() != null) applyEffect(living, p.effect());
            if (owner instanceof ServerPlayer sp && target != owner) {
                PacketHandler.getPlayChannel().sendToPlayer(() -> sp, new S2CMessageProjectileHitEntity(
                        c.x, c.y, c.z, S2CMessageProjectileHitEntity.HitType.NORMAL, target instanceof Player));
            }
            PacketHandler.getPlayChannel().sendToTracking(() -> target, new S2CMessageBlood(c.x, c.y, c.z));
        }
        return true;
    }

    private static void applyEffect(LivingEntity target, ResidueProfile.Effect fx) {
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(fx.id());
        if (effect == null) return;
        if (fx.skipFireImmune() && target.fireImmune()) return;
        int duration = fx.duration();
        MobEffectInstance existing = target.getEffect(effect);
        if (fx.stacking() && existing != null) {
            duration = Math.min(existing.getDuration() + duration, fx.maxDuration());
        }
        target.addEffect(new MobEffectInstance(effect, duration, fx.amplifier()));
    }

    private static int stochasticRound(double v, RandomSource r) {
        int base = (int) Math.floor(v);
        return base + (r.nextDouble() < v - base ? 1 : 0);
    }

    private static Vec3 latticeDir(Vec3 axis, double cosHalf, int i, int n, double phase) {
        double z = 1.0 - ((i + 0.5) / n) * (1.0 - cosHalf);
        double s = Math.sqrt(Math.max(0.0, 1.0 - z * z));
        double phi = i * 2.399963229728653 + phase * Math.PI * 2.0;
        Vec3 up = Math.abs(axis.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = axis.cross(up).normalize();
        Vec3 up2 = right.cross(axis);
        return right.scale(s * Math.cos(phi)).add(up2.scale(s * Math.sin(phi))).add(axis.scale(z)).normalize();
    }
}