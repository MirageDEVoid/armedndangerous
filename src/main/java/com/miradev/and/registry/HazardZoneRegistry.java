package com.miradev.and.registry;

import com.miradev.and.common.HazardZoneType;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class HazardZoneRegistry {
    private static Map<ResourceLocation, HazardZoneType> ZONES = new HashMap<>();

    public static void setAll(Map<ResourceLocation, HazardZoneType> zones) {
        ZONES = zones;
    }

    public static HazardZoneType get(ResourceLocation id) {
        return ZONES.get(id);
    }
}