package com.miradev.and.common;

import com.miradev.and.entity.HazardZoneEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;

public class HazardZoneManager {
    private static final Map<UUID, List<HazardZoneEntity>> OWNED = new HashMap<>();

    public static void register(HazardZoneEntity zone) {
        OWNED.computeIfAbsent(zone.getOwner(), k -> new ArrayList<>()).add(zone);
    }

    public static void igniteOwned(LivingEntity owner, ResourceLocation typeFilter) {
        List<HazardZoneEntity> zones = OWNED.get(owner.getUUID());
        if (zones == null) return;
        zones.removeIf(z -> !z.isAlive());
        for (HazardZoneEntity z : zones) {
            if (typeFilter == null || z.getHazardType().id.equals(typeFilter)) {
                z.ignite();
            }
        }
    }
}