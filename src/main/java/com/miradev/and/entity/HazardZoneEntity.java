package com.miradev.and.entity;

import com.miradev.and.common.HazardZoneType;
import com.miradev.and.init.ModEntities;
import com.miradev.and.registry.HazardZoneRegistry;
import net.minecraft.core.BlockPos;
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

    public float getRenderYOffset() { return this.renderYOffset; }

    public boolean isIgnitedSynced() { return this.entityData.get(IGNITED); }

    private static final EntityDataAccessor<Boolean> IGNITED =
            SynchedEntityData.defineId(HazardZoneEntity.class, EntityDataSerializers.BOOLEAN);

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket()
    {
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
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        ResourceLocation id = new ResourceLocation(buffer.readUtf());
        this.type = HazardZoneRegistry.get(id);
        this.owner = buffer.readUUID();
        this.renderYOffset = buffer.readFloat();
    }

    public HazardZoneEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    public HazardZoneEntity(Level level, double x, double y, double z, HazardZoneType type, LivingEntity owner) {
        this(ModEntities.HAZARD_ZONE.get(), level);

        BlockPos snapped = BlockPos.containing(x, y, z);
        double snappedX = snapped.getX();
        double snappedZ = snapped.getZ();
        double snappedY = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, snapped).getY();

        this.setPos(snappedX, snappedY, snappedZ);
        this.type = type;
        this.owner = owner.getUUID();
        this.renderYOffset = 0.05F + this.random.nextFloat() * 0.05F;
    }

    @Override
    public void tick() {
        super.tick();
        this.age++;
        if (this.age >= this.type.duration) { this.discard(); return; }

        if (!this.ignited && !this.level().isClientSide() && this.age % 5 == 0) {
            checkEnvironmentalIgnition();
        }

        if (this.ignited && !this.level().isClientSide() && this.age % this.type.tickInterval == 0) {
            sweepAndApply();
        }

        if (this.ignited && !this.level().isClientSide() && this.age % 5 == 0) {
            igniteNeighbors();
        }

        if (this.level().isClientSide()) {
            spawnVisualParticles();
        }
    }

    private void igniteNeighbors() {
        double horizontalRadius = this.type.radius;
        double verticalRange = 4.0;

        AABB myZone = new AABB(
                this.getX() - horizontalRadius, this.getY() - verticalRange, this.getZ() - horizontalRadius,
                this.getX() + horizontalRadius, this.getY() + verticalRange, this.getZ() + horizontalRadius
        );

        AABB searchArea = myZone.inflate(horizontalRadius, verticalRange, 0);

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

        int particleCount = clientIgnited ? 6 : 4;

        for (int i = 0; i < particleCount; i++) {
            double px = this.getX() + (this.random.nextDouble() - 0.5) * 2 * radius;
            double pz = this.getZ() + (this.random.nextDouble() - 0.5) * 2 * radius;

            BlockPos samplePos = BlockPos.containing(px, this.getY(), pz);
            double groundY = this.level().getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, samplePos).getY();
            double py = groundY + 0.05;

            if (clientIgnited) {
                this.level().addParticle(ParticleTypes.FLAME, px, py, pz, 0, 0.02, 0);
            } else {
                this.level().addParticle(ParticleTypes.ASH, px, py, pz, 0, 0, 0);
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

    private static final EntityDataAccessor<Integer> IGNITE_TICK =
            SynchedEntityData.defineId(HazardZoneEntity.class, EntityDataSerializers.INT);

    @Override
    protected void defineSynchedData() {
        this.entityData.define(IGNITED, false);
        this.entityData.define(IGNITE_TICK, -1);
    }

    public void ignite() {
        this.ignited = true;
        this.entityData.set(IGNITED, true);
        this.entityData.set(IGNITE_TICK, this.age);
    }

    public int getIgniteTick() { return this.entityData.get(IGNITE_TICK); }
    public int getAge() { return this.age; }

    public UUID getOwner() { return this.owner; }
    public HazardZoneType getHazardType() { return this.type; }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
}