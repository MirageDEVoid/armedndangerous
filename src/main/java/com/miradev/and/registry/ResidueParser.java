package com.miradev.and.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.miradev.and.common.ResidueTrigger;
import com.miradev.and.common.ResidueProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ResidueParser {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final double MAX_RADIUS = 32.0;
    private static final ResourceLocation WATER = new ResourceLocation("minecraft", "block/water_still");
    private static final ResourceLocation LAVA  = new ResourceLocation("minecraft", "block/lava_still");

    private ResidueParser() {}

    /** Parses one "residue" object. A bad trigger is skipped; a bad pool drops the whole entry. */
    public static void parseInto(ResourceLocation id, JsonObject o,
                                 Map<ResourceLocation, ResidueProfile> types,
                                 Map<String, ResidueTrigger> triggers) {
        ResidueProfile type = parseType(id, o);
        types.put(id, type);

        if (!o.has("triggers")) return;
        for (JsonElement e : GsonHelper.getAsJsonArray(o, "triggers")) {
            try {
                ResidueTrigger t = parseTrigger(type, GsonHelper.convertToJsonObject(e, "trigger"));
                ResidueTrigger prev = triggers.putIfAbsent(t.gun(), t);
                if (prev != null) {
                    LOGGER.warn("[extra] '{}' already bound to residue '{}'; ignoring binding from '{}'",
                            t.gun(), prev.residue(), id);
                }
            } catch (RuntimeException ex) {
                LOGGER.error("[extra] {}: skipping trigger: {}", id, ex.getMessage());
            }
        }
    }

    private static ResidueProfile parseType(ResourceLocation id, JsonObject o) {
        double radius = GsonHelper.getAsDouble(o, "radius");
        int lifetime = GsonHelper.getAsInt(o, "lifetime");
        int interval = GsonHelper.getAsInt(o, "interval", 10);
        if (radius <= 0 || radius > MAX_RADIUS) throw new JsonSyntaxException("radius must be in (0, " + MAX_RADIUS + "]");
        if (lifetime <= 0) throw new JsonSyntaxException("lifetime must be > 0");
        if (interval < 1) throw new JsonSyntaxException("interval must be >= 1");

        JsonObject eff = GsonHelper.getAsJsonObject(o, "effect");

        ResidueProfile.Effect effect = parseEffect(GsonHelper.getAsJsonObject(o, "effect"));

        ResidueProfile.Look visual = o.has("visual")
                ? parseLook(GsonHelper.getAsJsonObject(o, "visual"), WATER, 0xFFFFFF)
                : new ResidueProfile.Look(WATER, 0xFFFFFF, List.of());

        ResidueProfile.Ignition ignition = null;
        if (o.has("ignition")) {
            JsonObject ig = GsonHelper.getAsJsonObject(o, "ignition");
            ignition = new ResidueProfile.Ignition(
                    parseLook(ig, LAVA, 0xFF731A),
                    parseIds(ig, "ignited_by"),
                    GsonHelper.getAsBoolean(ig, "spreads", true));
        }

        return new ResidueProfile(id, radius, lifetime, interval, effect,
                GsonHelper.getAsBoolean(o, "affects_owner", true), visual, ignition);
    }

    private static ResidueTrigger parseTrigger(ResidueProfile type, JsonObject o) {
        String gun = GsonHelper.getAsString(o, "gun");

        ResidueTrigger.Hit head  = o.has("on_headshot") ? parseHit(GsonHelper.getAsJsonObject(o, "on_headshot"), true)  : null;
        ResidueTrigger.Hit body  = o.has("on_body")     ? parseHit(GsonHelper.getAsJsonObject(o, "on_body"), false)     : null;
        ResidueTrigger.Hit block = o.has("on_block")    ? parseHit(GsonHelper.getAsJsonObject(o, "on_block"), false)    : null;

        if (head == null && body == null && block == null) {
            throw new JsonSyntaxException("trigger for '" + gun + "' needs at least one of on_headshot, on_body, on_block");
        }
        if (type.ignition() == null) {
            for (ResidueTrigger.Hit h : new ResidueTrigger.Hit[]{head, body, block}) {
                if (h != null && (h.ignite() || h.igniteOwned())) {
                    LOGGER.warn("[extra] trigger for '{}' uses ignite flags but residue '{}' has no ignition block; they're ignored",
                            gun, type.id());
                    break;
                }
            }
        }
        return new ResidueTrigger(gun, type.id(), head, body, block);
    }

    private static ResidueTrigger.Hit parseHit(JsonObject o, boolean defaultIgnite) {
        ResidueTrigger.Explosion explosion = null;
        if (o.has("explosion")) {
            JsonObject e = GsonHelper.getAsJsonObject(o, "explosion");
            explosion = new ResidueTrigger.Explosion(
                    GsonHelper.getAsFloat(e, "power"),
                    GsonHelper.getAsBoolean(e, "fire", false),
                    GsonHelper.getAsBoolean(e, "break_blocks", false));
        }
        return new ResidueTrigger.Hit(
                GsonHelper.getAsBoolean(o, "ignite", defaultIgnite),
                GsonHelper.getAsBoolean(o, "ignite_owned", false),
                explosion);
    }

    private static ResidueProfile.Look parseLook(JsonObject o, ResourceLocation defaultSprite, int defaultColor) {
        return new ResidueProfile.Look(
                new ResourceLocation(GsonHelper.getAsString(o, "sprite", defaultSprite.toString())),
                parseColor(o, "color", defaultColor),
                parseIds(o, "particles"));
    }

    static ResidueProfile.Effect parseEffect(JsonObject eff) {
        ResourceLocation effectId = new ResourceLocation(GsonHelper.getAsString(eff, "id"));
        if (!BuiltInRegistries.MOB_EFFECT.containsKey(effectId)) {
            throw new JsonSyntaxException("unknown effect '" + effectId + "'");
        }
        int duration = GsonHelper.getAsInt(eff, "duration", 60);
        boolean stacking = GsonHelper.getAsBoolean(eff, "stacking", false);
        int maxDuration = GsonHelper.getAsInt(eff, "max_duration", stacking ? duration * 5 : duration);
        if (maxDuration < duration) throw new JsonSyntaxException("max_duration must be >= duration");
        return new ResidueProfile.Effect(effectId, GsonHelper.getAsInt(eff, "amplifier", 0), duration,
                GsonHelper.getAsBoolean(eff, "skip_fire_immune", false), stacking, maxDuration);
    }

    static int parseColor(JsonObject o, String key, int fallback) {
        if (!o.has(key)) return fallback;
        String s = GsonHelper.getAsString(o, key);
        if (s.startsWith("#")) s = s.substring(1);
        try {
            return Integer.parseInt(s, 16) & 0xFFFFFF;
        } catch (NumberFormatException ex) {
            throw new JsonSyntaxException("'" + key + "' must be a #RRGGBB color, was '" + s + "'");
        }
    }

    static List<ResourceLocation> parseIds(JsonObject o, String key) {
        List<ResourceLocation> out = new ArrayList<>();
        if (!o.has(key)) return out;
        for (JsonElement e : GsonHelper.getAsJsonArray(o, key)) {
            out.add(new ResourceLocation(GsonHelper.convertToString(e, key)));
        }
        return out;
    }
}
