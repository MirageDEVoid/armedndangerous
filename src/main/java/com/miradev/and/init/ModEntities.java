package com.miradev.and.init;

import com.miradev.and.Reference;
import com.miradev.and.entity.ResidueEntity;
import com.miradev.and.entity.ThrowablePipebombEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> REGISTER =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Reference.MOD_ID);

    public static final RegistryObject<EntityType<ResidueEntity>> RESIDUE =
            REGISTER.register("residue", () -> EntityType.Builder
                    .<ResidueEntity>of(ResidueEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.F)
                    .noSave()
                    .fireImmune()
                    .clientTrackingRange(8)
                    .updateInterval(20)
                    .build(new ResourceLocation(Reference.MOD_ID, "residue").toString()));

    public static final RegistryObject<EntityType<ThrowablePipebombEntity>> THROWABLE_PIPEBOMB =
            REGISTER.register("throwable_pipebomb", () -> EntityType.Builder
                    .<ThrowablePipebombEntity>of(ThrowablePipebombEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .setTrackingRange(64)
                    .setUpdateInterval(1)
                    .setShouldReceiveVelocityUpdates(true)
                    .build("throwable_pipebomb"));
}