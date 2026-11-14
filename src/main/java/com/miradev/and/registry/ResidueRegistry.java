package com.miradev.and.registry;

import com.miradev.and.common.ResidueProfile;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

public class ResidueRegistry {
    private static volatile Map<ResourceLocation, ResidueProfile> TYPES = Map.of();

    public static void setAll(Map<ResourceLocation, ResidueProfile> types) { TYPES = Map.copyOf(types); }
    @Nullable
    public static ResidueProfile get(ResourceLocation id) { return TYPES.get(id); }
    public static Set<ResourceLocation> ids() { return TYPES.keySet(); }
}
