package com.miradev.and.registry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.miradev.and.Reference;
import com.miradev.and.common.FragmentProfile;
import com.miradev.and.common.ResidueTrigger;
import com.miradev.and.common.ResidueProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class ExtraDataRegistry extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();

    public ExtraDataRegistry() { super(GSON, "extra"); }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, ResidueProfile> residues = new HashMap<>();
        Map<String, ResidueTrigger> triggers = new HashMap<>();
        Map<String, FragmentProfile> fragments = new HashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : new TreeMap<>(files).entrySet()) {
            ResourceLocation id = entry.getKey();
            try {
                JsonObject root = GsonHelper.convertToJsonObject(entry.getValue(), "file");
                if (root.has("hazard_zone") || root.has("hazard_trigger")) {
                    LOGGER.warn("[extra] {} uses the old hazard_* format and was skipped; migrate it to \"residue\"", id);
                }
                if (root.has("residue")) {
                    ResidueParser.parseInto(id, GsonHelper.getAsJsonObject(root, "residue"), residues, triggers);
                }
                if (root.has("fragmentation")) {
                    FragmentParser.parseInto(id, GsonHelper.getAsJsonObject(root, "fragmentation"), fragments);
                }

            } catch (RuntimeException ex) {
                LOGGER.error("[extra] Skipping {}: {}", id, ex.getMessage());
            }
        }

        ResidueRegistry.setAll(residues);
        ResidueTriggerRegistry.setAll(triggers);
        FragmentRegistry.setAll(fragments);
        LOGGER.info("[extra] Loaded {} residues, {} gun triggers, {} fragment bindings", residues.size(), triggers.size(), fragments.size());
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ExtraDataRegistry());
    }
}