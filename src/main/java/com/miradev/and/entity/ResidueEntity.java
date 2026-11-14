package com.miradev.and.entity;

import com.miradev.and.common.ResidueProfile;
import com.miradev.and.effect.ScalingDamageEffect;
import com.miradev.and.init.ModEntities;
import com.miradev.and.registry.ResidueParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class ResidueEntity extends Entity implements IEntityAdditionalSpawnData {

    private static final EntityDataAccessor<Boolean> ACTIVE =
            SynchedEntityData.defineId(ResidueEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> IGNITE_TICK =
            SynchedEntityData.defineId(ResidueEntity.class, EntityDataSerializers.INT);

    private static final ResidueProfile.Look DEFAULT_LOOK =
            new ResidueProfile.Look(new ResourceLocation("minecraft", "block/water_still"), 0xFFFFFF, List.of());

    private static final double REACH_BELOW = 0.5, REACH_ABOVE = 2.0;
    private static final double MERGE_FRACTION = 0.5;
    private static final int IGNITION_CHECK_INTERVAL = 5;
    private static final int SPREAD_INTERVAL = 5;
    private static final int GROUND_SEARCH_DEPTH = 16;
    private static final double MIN_FALL_FOR_DELAY = 0.8;

    // states:

    // server side
    @Nullable private ResidueProfile type;
    @Nullable private UUID owner;

    // shared w/ client
    private double radius;
    private int lifetime = 1;
    private ResidueProfile.Look look = DEFAULT_LOOK;
    @Nullable private ResidueProfile.Look ignitedLook;
    private int fallDelayTicks;
    private double impactY;
    private float thickness = 0.1F;

    private int age;

    public ResidueEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    // getters:

    public boolean isActive() { return this.entityData.get(ACTIVE); }
    public int getIgniteTick() { return this.entityData.get(IGNITE_TICK); }
    public int getAge() { return this.age; }
    public int getEffectiveAge() { return Math.max(0, this.age - this.fallDelayTicks); }
    public int getFallDelayTicks() { return this.fallDelayTicks; }
    public double getRadius() { return this.radius; }
    public int getLifetime() { return this.lifetime; }
    public float getThickness() { return this.thickness; }
    public ResidueProfile.Look getLook() { return this.look; }
    @Nullable public ResidueProfile.Look getIgnitedLook() { return this.ignitedLook; }
    @Nullable public UUID getOwner() { return this.owner; }

    // networking:

    @Override
    protected void defineSynchedData() {
        this.entityData.define(ACTIVE, false);
        this.entityData.define(IGNITE_TICK, -1);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override public boolean shouldBeSaved() { return false; }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return this.radius > 0 ? EntityDimensions.scalable((float) this.radius * 2, 0.5F) : super.getDimensions(pose);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buf) {
        buf.writeDouble(this.radius);
        buf.writeVarInt(this.lifetime);
        this.look.write(buf);
        buf.writeBoolean(this.ignitedLook != null);
        if (this.ignitedLook != null) this.ignitedLook.write(buf);
        buf.writeVarInt(this.fallDelayTicks);
        buf.writeDouble(this.impactY);
        buf.writeFloat(this.thickness);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buf) {
        this.radius = buf.readDouble();
        this.lifetime = buf.readVarInt();
        this.look = ResidueProfile.Look.read(buf);
        this.ignitedLook = buf.readBoolean() ? ResidueProfile.Look.read(buf) : null;
        this.fallDelayTicks = buf.readVarInt();
        this.impactY = buf.readDouble();
        this.thickness = buf.readFloat();
        this.refreshDimensions();
    }

    // creation:

    // mob interaction

    @Nullable
    public static ResidueEntity tryCreate(Level level, double x, double y, double z,
                                          ResidueProfile type, LivingEntity owner) {
        return create(level, x, y, z, y, type, owner);
    }

    // block interaction

    @Nullable
    public static ResidueEntity tryCreate(Level level, double x, double y, double z, Direction face,
                                          ResidueProfile type, LivingEntity owner) {
        return create(level, x + face.getStepX() * 0.1, y + face.getStepY() * 0.1, z + face.getStepZ() * 0.1,
                y, type, owner);
    }

    @Nullable
    private static ResidueEntity create(Level level, double x, double y, double z, double originalHitY,
                                        ResidueProfile type, LivingEntity owner) {
        BlockPos ground = findGroundBelow(level, BlockPos.containing(x, y, z));
        if (ground == null) return null;
        if (isCrowded(level, ground, type)) return null;

        ResidueEntity zone = new ResidueEntity(ModEntities.RESIDUE.get(), level);
        zone.type = type;
        zone.owner = owner.getUUID();
        zone.radius = type.radius();
        zone.lifetime = type.lifetime();
        zone.look = type.visual();
        zone.ignitedLook = type.ignition() != null ? type.ignition().look() : null;
        zone.thickness = 0.1F + (zone.random.nextFloat() * 0.1F - 0.025F);
        zone.setPos(ground.getX() + 0.5, ground.getY() + 1, ground.getZ() + 0.5);
        zone.refreshDimensions();

        double fall = originalHitY - (ground.getY() + 1.0);
        if (fall > MIN_FALL_FOR_DELAY) {
            zone.fallDelayTicks = Mth.clamp((int) (fall * 4.5F) + 3, 6, 40);
            zone.impactY = originalHitY;
        } else {
            zone.impactY = zone.getY();
        }

        if (type.ignition() == null) zone.activate();
        return zone;
    }

    @Nullable
    private static BlockPos findGroundBelow(Level level, BlockPos start) {
        BlockPos.MutableBlockPos pos = start.mutable();
        for (int i = 0; i < GROUND_SEARCH_DEPTH; i++) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && !state.getCollisionShape(level, pos).isEmpty()) return pos.immutable();
            pos.move(0, -1, 0);
        }
        return null;
    }

    private static boolean isCrowded(Level level, BlockPos ground, ResidueProfile type) {
        double merge = type.radius() * MERGE_FRACTION;
        if (merge <= 0) return false;

        double cx = ground.getX() + 0.5, cy = ground.getY() + 1.0, cz = ground.getZ() + 0.5;
        double reach = merge + ResidueParser.MAX_RADIUS;
        AABB search = new AABB(cx - reach, cy - 2, cz - reach, cx + reach, cy + 3, cz + reach);

        for (ResidueEntity e : level.getEntitiesOfClass(ResidueEntity.class, search,
                other -> other.type != null && other.type.id().equals(type.id()))) {
            if (Math.abs(e.getX() - cx) < merge
                    && Math.abs(e.getZ() - cz) < merge
                    && Math.abs(e.getY() - cy) < 1.5) {
                return true;
            }
        }
        return false;
    }

    // activation:

    // ignite/mark active

    public void activate() {
        if (isActive()) return;
        this.entityData.set(ACTIVE, true);
        this.entityData.set(IGNITE_TICK, getEffectiveAge());
    }

    // activate owned zones

    public static void igniteOwned(LivingEntity owner, @Nullable ResourceLocation typeFilter) {
        if (!(owner.level() instanceof ServerLevel level)) return;
        UUID id = owner.getUUID();
        for (ResidueEntity z : level.getEntities(EntityTypeTest.forClass(ResidueEntity.class),
                e -> e.isAlive() && e.type != null && id.equals(e.owner)
                        && (typeFilter == null || e.type.id().equals(typeFilter)))) {
            z.activate();
        }
    }

    // tick stuff:

    @Override
    public void tick() {
        super.tick();
        this.age++;
        if (this.level().isClientSide()) tickClient(); else tickServer();
    }

    private void tickClient() {
        if (this.age < this.fallDelayTicks) {
            spawnFallingParticles();
            return;
        }
        if (getEffectiveAge() >= this.lifetime) { discard(); return; }
        spawnVisualParticles();
    }

    private void tickServer() {
        if (this.type == null) { discard(); return; }
        if (this.age < this.fallDelayTicks) return;

        int t = getEffectiveAge();
        if (t >= this.lifetime) { discard(); return; }

        if (!isActive()) {
            if (t % IGNITION_CHECK_INTERVAL == 0) checkEnvironmentalIgnition();
            return;
        }

        if (t % this.type.interval() == 0) sweepAndApply();

        ResidueProfile.Ignition ig = this.type.ignition();
        if (ig != null && ig.spreads() && t % SPREAD_INTERVAL == 0) spreadIgnition();
    }

    // server stuff:

    // square zone and vertical window for effect sweep

    private AABB zoneBox() {
        double r = this.type.radius();
        return new AABB(getX() - r, getY() - REACH_BELOW, getZ() - r,
                getX() + r, getY() + REACH_ABOVE, getZ() + r);
    }

    private void sweepAndApply() {
        ResidueProfile.Effect fx = this.type.effect();
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(fx.id());
        if (effect == null) return;

        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, zoneBox())) {
            if (canAffect(target, fx)) applyTo(target, effect, fx);
        }
    }

    private boolean canAffect(LivingEntity target, ResidueProfile.Effect fx) {
        if (fx.skipFireImmune() && target.fireImmune()) return false;
        return this.type.affectsOwner() || !target.getUUID().equals(this.owner);
    }

    private void applyTo(LivingEntity target, MobEffect effect, ResidueProfile.Effect fx) {
        int duration = fx.duration();

        if (fx.stacking()) {
            MobEffectInstance current = target.getEffect(effect);
            if (current != null) duration = Math.min(current.getDuration() + fx.duration(), fx.maxDuration());
        }
        if (effect instanceof ScalingDamageEffect scaling && this.owner != null) {
            scaling.setSource(target, this.owner); // kill credit goes to the residue's owner
        }
        target.addEffect(new MobEffectInstance(effect, duration, fx.amplifier()));
    }

    // light other unlit zone in range on fire

    private void spreadIgnition() {
        double reach = this.type.radius() + ResidueParser.MAX_RADIUS;
        AABB search = new AABB(getX() - reach, getY() - 4, getZ() - reach, getX() + reach, getY() + 4, getZ() + reach);
        for (ResidueEntity other : this.level().getEntitiesOfClass(ResidueEntity.class, search,
                e -> e != this && e.type != null && !e.isActive())) {
            double touch = this.type.radius() + other.type.radius();
            if (Math.abs(other.getX() - getX()) < touch && Math.abs(other.getZ() - getZ()) < touch) {
                other.activate();
            }
        }
    }

    // lit up if a block in flammable config is found in range

    private void checkEnvironmentalIgnition() {
        ResidueProfile.Ignition ig = this.type.ignition();
        if (ig == null || ig.ignitedBy().isEmpty()) return;

        double r = this.type.radius();
        BlockPos min = BlockPos.containing(getX() - r, getY() - 1, getZ() - r);
        BlockPos max = BlockPos.containing(getX() + r, getY() + 1, getZ() + r);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            ResourceLocation key = BuiltInRegistries.BLOCK.getKey(this.level().getBlockState(pos).getBlock());
            if (ig.ignitedBy().contains(key)) { activate(); return; }
        }
    }

    // client particles:

    private void spawnFallingParticles() {
        double progress = (double) this.age / Math.max(this.fallDelayTicks, 1);
        double y = Mth.lerp(progress, this.impactY, getY());
        for (int i = 0; i < 2; i++) {
            spawnOne(this.look.particles(),
                    getX() + (random.nextDouble() - 0.5) * 0.6, y, getZ() + (random.nextDouble() - 0.5) * 0.6, -0.05);
        }
    }

    private void spawnVisualParticles() {
        boolean lit = isActive() && this.ignitedLook != null;
        List<ResourceLocation> particles = lit ? this.ignitedLook.particles() : this.look.particles();
        int count = lit ? 4 : 3;
        for (int i = 0; i < count; i++) {
            double ox = (random.nextDouble() * 2 - 1) * this.radius; // uniform over the square
            double oz = (random.nextDouble() * 2 - 1) * this.radius;
            spawnOne(particles, getX() + ox, getY() + 0.08, getZ() + oz, 0.02);
        }
    }

    private void spawnOne(List<ResourceLocation> ids, double x, double y, double z, double vy) {
        if (ids.isEmpty()) return;
        ResourceLocation id = ids.get(random.nextInt(ids.size()));
        if (BuiltInRegistries.PARTICLE_TYPE.get(id) instanceof SimpleParticleType simple) {
            this.level().addParticle(simple, x, y, z, 0, vy, 0);
        }
    }
}