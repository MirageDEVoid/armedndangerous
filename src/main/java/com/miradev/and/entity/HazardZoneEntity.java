package com.miradev.and.entity;

import com.miradev.and.common.HazardZoneType;
import com.miradev.and.init.ModEntities;
import com.miradev.and.registry.HazardZoneRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;
import java.util.UUID;

public class HazardZoneEntity extends Entity implements IEntityAdditionalSpawnData {
    private HazardZoneType type;
    private boolean ignited;
    private UUID owner;
    private int age;
    private float renderYOffset;

    private int fallDelayTicks = 0;
    private double impactY;

    private float thickness = 0.1F;

    public float getThickness() {
        return this.thickness;
    }

    public int getEffectiveAge() {
        return Math.max(0, this.age - this.fallDelayTicks);
    }

    public float getRenderYOffset() { return this.renderYOffset; }
    public boolean isIgnitedSynced() { return this.entityData.get(IGNITED); }
    public int getFallDelayTicks() { return this.fallDelayTicks; }
    public double getImpactY() { return this.impactY; }

    private static final EntityDataAccessor<Boolean> IGNITED =
            SynchedEntityData.defineId(HazardZoneEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> IGNITE_TICK =
            SynchedEntityData.defineId(HazardZoneEntity.class, EntityDataSerializers.INT);

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        if (this.type == null) return super.getDimensions(pose);
        float diameter = (float) this.type.radius * 2;
        return EntityDimensions.scalable(diameter, 0.5F);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.type.id.toString());
        buffer.writeUUID(this.owner);
        buffer.writeFloat(this.renderYOffset);
        buffer.writeVarInt(this.fallDelayTicks);
        buffer.writeDouble(this.impactY);
        buffer.writeFloat(this.thickness);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        ResourceLocation id = new ResourceLocation(buffer.readUtf());
        this.type = HazardZoneRegistry.get(id);
        this.owner = buffer.readUUID();
        this.renderYOffset = buffer.readFloat();
        this.fallDelayTicks = buffer.readVarInt();
        this.impactY = buffer.readDouble();
        this.thickness = buffer.readFloat();
    }

    public HazardZoneEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    public static HazardZoneEntity tryCreate(Level level, double x, double y, double z, HazardZoneType type, LivingEntity owner) {
        return tryCreateInternal(level, x, y, z, y, type, owner);
    }

    public static HazardZoneEntity tryCreate(Level level, double x, double y, double z, Direction hitFace, HazardZoneType type, LivingEntity owner) {
        double gx = x + hitFace.getStepX() * 0.1;
        double gy = y + hitFace.getStepY() * 0.1;
        double gz = z + hitFace.getStepZ() * 0.1;
        return tryCreateInternal(level, gx, gy, gz, y, type, owner);
    }

    private static HazardZoneEntity tryCreateInternal(Level level, double x, double y, double z, double originalHitY, HazardZoneType type, LivingEntity owner) {
        BlockPos hitPos = BlockPos.containing(x, y, z);
        BlockPos groundPos = findGroundBelow(level, hitPos, 16);
        if (groundPos == null) return null;

        HazardZoneEntity zone = new HazardZoneEntity(ModEntities.HAZARD_ZONE.get(), level);
        zone.setPos(groundPos.getX() + 0.5, groundPos.getY() + 1, groundPos.getZ() + 0.5);
        zone.type = type;
        zone.owner = owner.getUUID();
        zone.renderYOffset = 0.025F + zone.random.nextFloat() * 0.025F;

        zone.thickness = 0.1F + (zone.random.nextFloat() * 0.1F - 0.025F);

        double fallDistance = originalHitY - (groundPos.getY() + 1.0);
        if (fallDistance > 0.8) {
            zone.fallDelayTicks = Mth.clamp((int)(fallDistance * 4.5f) + 3, 6, 40);
            zone.impactY = originalHitY;
        } else {
            zone.fallDelayTicks = 0;
            zone.impactY = zone.getY();
        }

        return zone;
    }

    private static BlockPos findGroundBelow(Level level, BlockPos start, int maxSearch) {
        BlockPos.MutableBlockPos pos = start.mutable();
        for (int i = 0; i < maxSearch; i++) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && !state.getCollisionShape(level, pos).isEmpty()) {
                return pos.immutable();
            }
            pos.move(0, -1, 0);
        }
        return null;
    }

    @Override
    public void tick() {
        super.tick();
        this.age++;

        if (this.age < this.fallDelayTicks) {
            if (this.level().isClientSide()) {
                spawnFallingOilParticles();
            }
            return;
        }

        int effectiveAge = this.getEffectiveAge();

        if (effectiveAge >= this.type.duration) {
            this.discard();
            return;
        }

        if (!this.ignited && !this.level().isClientSide() && effectiveAge % 5 == 0) {
            checkEnvironmentalIgnition();
            checkNearbyIgnited();
        }

        if (this.ignited && !this.level().isClientSide() && effectiveAge % this.type.tickInterval == 0) {
            sweepAndApply();
        }

        if (this.ignited && !this.level().isClientSide() && effectiveAge % 5 == 0) {
            igniteNeighbors();
        }

        if (this.level().isClientSide()) {
            spawnVisualParticles();
        }
    }

    private void spawnFallingOilParticles() {
        double progress = (double) this.age / (double) Math.max(this.fallDelayTicks, 1);
        double currentY = Mth.lerp(progress, this.impactY, this.getY());

        for (int i = 0; i < 2; i++) {
            double ox = (this.random.nextDouble() - 0.5) * 0.6;
            double oz = (this.random.nextDouble() - 0.5) * 0.6;
            this.level().addParticle(ParticleTypes.FALLING_LAVA,
                    this.getX() + ox, currentY, this.getZ() + oz,
                    0, -0.05, 0);
        }
    }

    private void checkNearbyIgnited() {
        double horizontalRadius = this.type.radius;
        double verticalRange = 4.0;

        AABB myZone = new AABB(
                this.getX() - horizontalRadius, this.getY() - verticalRange, this.getZ() - horizontalRadius,
                this.getX() + horizontalRadius, this.getY() + verticalRange, this.getZ() + horizontalRadius
        );

        AABB searchArea = myZone.inflate(horizontalRadius, verticalRange, horizontalRadius);

        List<HazardZoneEntity> neighbors = this.level().getEntitiesOfClass(
                HazardZoneEntity.class, searchArea,
                e -> e != this && e.ignited
        );

        for (HazardZoneEntity neighbor : neighbors) {
            double neighborHorizontal = neighbor.type.radius;
            AABB neighborZone = new AABB(
                    neighbor.getX() - neighborHorizontal, neighbor.getY() - verticalRange, neighbor.getZ() - neighborHorizontal,
                    neighbor.getX() + neighborHorizontal, neighbor.getY() + verticalRange, neighbor.getZ() + neighborHorizontal
            );

            if (myZone.intersects(neighborZone)) {
                this.ignite();
                return;
            }
        }
    }

    private void igniteNeighbors() {
        double horizontalRadius = this.type.radius;
        double verticalRange = 4.0;

        AABB myZone = new AABB(
                this.getX() - horizontalRadius, this.getY() - verticalRange, this.getZ() - horizontalRadius,
                this.getX() + horizontalRadius, this.getY() + verticalRange, this.getZ() + horizontalRadius
        );

        AABB searchArea = myZone.inflate(horizontalRadius, verticalRange, horizontalRadius);

        List<HazardZoneEntity> neighbors = this.level().getEntitiesOfClass(
                HazardZoneEntity.class, searchArea, e -> e != this && !e.ignited
        );

        for (HazardZoneEntity neighbor : neighbors) {
            double neighborHorizontal = neighbor.type.radius;
            AABB neighborZone = new AABB(
                    neighbor.getX() - neighborHorizontal, neighbor.getY() - verticalRange, neighbor.getZ() - neighborHorizontal,
                    neighbor.getX() + neighborHorizontal, neighbor.getY() + verticalRange, neighbor.getZ() + neighborHorizontal
            );

            if (myZone.intersects(neighborZone)) {
                neighbor.ignite();
            }
        }
    }

    private void checkEnvironmentalIgnition() {
        int r = (int) Math.ceil(this.type.radius);
        BlockPos center = this.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -1, -r), center.offset(r, 1, r))) {
            BlockState state = this.level().getBlockState(pos);
            if (state.is(Blocks.FIRE) || state.is(Blocks.LAVA) || state.is(Blocks.SOUL_FIRE)) {
                this.ignite();
                return;
            }
        }
    }

    private void spawnVisualParticles() {
        boolean clientIgnited = this.entityData.get(IGNITED);
        double radius = this.type.radius;

        int particleCount = clientIgnited ? 4 : 3;

        for (int i = 0; i < particleCount; i++) {
            double px = this.getX() + (this.random.nextDouble() - 0.5) * 2 * radius;
            double pz = this.getZ() + (this.random.nextDouble() - 0.5) * 2 * radius;
            double py = this.getY() + 0.08;

            if (clientIgnited) {
                if (this.random.nextFloat() < 0.75F) {
                    this.level().addParticle(ParticleTypes.FLAME, px, py, pz, 0, 0.025, 0);
                } else {
                    this.level().addParticle(ParticleTypes.LAVA, px, py, pz, 0, 0.015, 0);
                }
            } else {
                this.level().addParticle(ParticleTypes.ASH, px, py, pz, 0, 0.008, 0);
            }
        }
    }

    private void sweepAndApply() {
        if (this.level().isClientSide()) return;

        AABB area = this.getBoundingBox().inflate(this.type.radius);
        for (LivingEntity le : this.level().getEntitiesOfClass(LivingEntity.class, area)) {
            if (le.fireImmune()) continue;

            boolean isOwner = le.getUUID().equals(this.owner);
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(this.type.appliedEffect);
            if (effect == null) continue;

            int duration = isOwner
                    ? (int) (this.type.effectDuration * this.type.ownerDamageMultiplier)
                    : this.type.effectDuration;

            le.addEffect(new MobEffectInstance(effect, duration, this.type.effectAmplifier));
        }
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(IGNITED, false);
        this.entityData.define(IGNITE_TICK, -1);
    }

    public void ignite() {
        if (this.ignited) return;

        this.ignited = true;
        this.entityData.set(IGNITED, true);
        this.entityData.set(IGNITE_TICK, this.getEffectiveAge());
    }

    public int getIgniteTick() { return this.entityData.get(IGNITE_TICK); }
    public int getAge() { return this.age; }

    public UUID getOwner() { return this.owner; }
    public HazardZoneType getHazardType() { return this.type; }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
}