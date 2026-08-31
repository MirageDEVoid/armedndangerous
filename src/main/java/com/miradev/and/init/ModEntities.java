package com.miradev.and.init;

import com.miradev.and.Reference;
import com.miradev.and.entity.HazardZoneEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> REGISTER =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Reference.MOD_ID);

    public static final RegistryObject<EntityType<HazardZoneEntity>> HAZARD_ZONE =
            REGISTER.register("hazard_zone", () -> EntityType.Builder
                    .<HazardZoneEntity>of(HazardZoneEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.F)
                    .noSave()
                    .fireImmune()
                    .clientTrackingRange(8)
                    .updateInterval(20)
                    .build(new ResourceLocation(Reference.MOD_ID, "hazard_zone").toString()));
}