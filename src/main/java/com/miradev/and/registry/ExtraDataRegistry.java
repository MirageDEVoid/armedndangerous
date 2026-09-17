package com.miradev.and.registry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.miradev.and.Reference;
import com.miradev.and.common.HazardTriggerBinding;
import com.miradev.and.common.HazardZoneType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class ExtraDataRegistry extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().create();

    public ExtraDataRegistry() {
        super(GSON, "extra");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceMap, ResourceManager manager, ProfilerFiller profiler) {
        System.out.println("[extraData] Reload triggered, found " + resourceMap.size() + " files in extra/");

        Map<ResourceLocation, HazardZoneType> newHazards = new HashMap<>();
        Map<String, HazardTriggerBinding> newTriggers = new HashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : resourceMap.entrySet()) {
            ResourceLocation id = entry.getKey();
            JsonObject root = entry.getValue().getAsJsonObject();
            System.out.println("[extraData] Found entry with id: " + id);

            if (root.has("hazard_zone")) {
                try {
                    newHazards.put(id, parseHazardZone(id, root.getAsJsonObject("hazard_zone")));
                } catch (Exception e) {
                    System.err.println("[extraData] Failed to parse hazard_zone in " + id + ": " + e.getMessage());
                }
            }

            if (root.has("hazard_trigger")) {
                try {
                    JsonObject obj = root.getAsJsonObject("hazard_trigger");
                    HazardTriggerBinding binding = new HazardTriggerBinding();
                    binding.gunId = obj.get("gun_id").getAsString();
                    binding.hazardType = new ResourceLocation(obj.get("hazard_type").getAsString());
                    binding.ignitesOwnedOnHeadshot = obj.has("ignites_owned_on_headshot") && obj.get("ignites_owned_on_headshot").getAsBoolean();

                    // Explosion (all optional)
                    if (obj.has("explosion_power")) {
                        binding.explosionPower = obj.get("explosion_power").getAsFloat();
                    }
                    if (obj.has("explosion_causes_fire")) {
                        binding.explosionCausesFire = obj.get("explosion_causes_fire").getAsBoolean();
                    }
                    if (obj.has("explosion_destroys_blocks")) {
                        binding.explosionDestroysBlocks = obj.get("explosion_destroys_blocks").getAsBoolean();
                    }

                    newTriggers.put(binding.gunId, binding);
                } catch (Exception e) {
                    System.err.println("[extraData] Failed to parse hazard_trigger in " + id + ": " + e.getMessage());
                }
            }
        }

        HazardZoneRegistry.setAll(newHazards);
        HazardTriggerRegistry.setAll(newTriggers);
    }

    private HazardZoneType parseHazardZone(ResourceLocation id, JsonObject obj) {
        HazardZoneType type = new HazardZoneType();
        type.id = id;
        type.radius = obj.get("radius").getAsDouble();
        type.duration = obj.get("duration").getAsInt();
        type.tickInterval = obj.get("tick_interval").getAsInt();
        type.appliedEffect = new ResourceLocation(obj.get("applied_effect").getAsString());
        type.effectAmplifier = obj.get("effect_amplifier").getAsInt();
        type.effectDuration = obj.get("effect_duration").getAsInt();
        type.ownerDamageMultiplier = obj.get("owner_damage_multiplier").getAsFloat();
        return type;
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ExtraDataRegistry());
    }
}