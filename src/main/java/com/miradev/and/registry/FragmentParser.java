package com.miradev.and.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.miradev.and.common.FragmentProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.slf4j.Logger;
import ttv.migami.jeg.init.ModTags;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class FragmentParser {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int MAX_COUNT = Integer.MAX_VALUE;
    public static final double MAX_RANGE = 1024.0;
    public static final int MAX_PIERCE = 1024;
    public static final int MAX_BOUNCE = 1024;

    private FragmentParser() {}

    public static void parseInto(ResourceLocation id, JsonObject o, Map<String, FragmentProfile> bySource) {
        FragmentProfile p = parse(id, o);
        for (String source : p.sources()) {
            FragmentProfile prev = bySource.putIfAbsent(source, p);
            if (prev != null) {
                LOGGER.warn("[extra] '{}' already bound to fragmentation '{}'; ignoring binding from '{}'",
                        source, prev.id(), id);
            }
        }
    }

    private static FragmentProfile parse(ResourceLocation id, JsonObject o) {
        int count = GsonHelper.getAsInt(o, "count");
        double range = GsonHelper.getAsDouble(o, "range");
        float damage = GsonHelper.getAsFloat(o, "damage");
        float falloff = GsonHelper.getAsFloat(o, "falloff", 0.5F);
        int pierce = GsonHelper.getAsInt(o, "pierce", 0);
        double hitRadius = GsonHelper.getAsDouble(o, "hit_radius", 0.25);
        int bounce = GsonHelper.getAsInt(o, "bounce", 0);
        double bounceLoss = GsonHelper.getAsDouble(o, "bounce_loss", 0.5);
        double aimBias = GsonHelper.getAsDouble(o, "aim_bias", 0.5);
        int maxAimed = GsonHelper.getAsInt(o, "max_aimed_per_target", 8);

        if (count < 1 || count > MAX_COUNT) throw new JsonSyntaxException("count must be in [1, " + MAX_COUNT + "]");
        if (range <= 0 || range > MAX_RANGE) throw new JsonSyntaxException("range must be in (0, " + MAX_RANGE + "]");
        if (damage < 0) throw new JsonSyntaxException("damage must be >= 0");
        if (falloff < 0 || falloff > 1) throw new JsonSyntaxException("falloff must be in [0, 1]");
        if (pierce < 0 || pierce > MAX_PIERCE) throw new JsonSyntaxException("pierce must be in [0, " + MAX_PIERCE + "]");
        if (hitRadius < 0 || hitRadius > 1.0) throw new JsonSyntaxException("hit_radius must be in [0, 1]");
        if (bounce < 0 || bounce > MAX_BOUNCE) throw new JsonSyntaxException("bounces must be in [0, " + MAX_BOUNCE + "]");
        if (bounceLoss < 0 || bounceLoss > 1) throw new JsonSyntaxException("bounce_loss must be in [0, 1]");
        if (aimBias < 0 || aimBias > 1) throw new JsonSyntaxException("aim_bias must be in [0, 1]");
        if (maxAimed < 1 || maxAimed > (count * aimBias)) throw new JsonSyntaxException("max_aimed_per_target must be in [1, count * aimBias]");

        ResourceLocation advantage = o.has("advantage")
                ? new ResourceLocation(GsonHelper.getAsString(o, "advantage"))
                : ModTags.Entities.NONE.location();

        FragmentProfile.Look visual = new FragmentProfile.Look(0xC8C8C8, List.of());
        if (o.has("visual")) {
            JsonObject v = GsonHelper.getAsJsonObject(o, "visual");
            visual = new FragmentProfile.Look(ResidueParser.parseColor(v, "color", 0xC8C8C8),
                    ResidueParser.parseIds(v, "particles"));
        }

        return new FragmentProfile(id, parseSources(o), count, range, parseShape(o), damage, falloff, pierce,
                hitRadius, bounce, bounceLoss, aimBias, maxAimed,
                advantage,
                GsonHelper.getAsBoolean(o, "affects_owner", true),
                GsonHelper.getAsBoolean(o, "breaks_fragile", true),
                o.has("effect") ? ResidueParser.parseEffect(GsonHelper.getAsJsonObject(o, "effect")) : null,
                visual);
    }

    private static List<String> parseSources(JsonObject o) {
        List<String> out = new ArrayList<>();
        for (JsonElement e : GsonHelper.getAsJsonArray(o, "sources")) {
            String raw = GsonHelper.convertToString(e, "source");
            ResourceLocation rl = ResourceLocation.tryParse(raw);
            if (rl == null) throw new JsonSyntaxException("bad source id '" + raw + "'");
            out.add(rl.toString());
        }
        if (out.isEmpty()) throw new JsonSyntaxException("sources must not be empty");
        return out;
    }

    private static FragmentProfile.Shape parseShape(JsonObject o) {
        if (!o.has("shape")) return FragmentProfile.Shape.SPHERE;
        JsonObject s = GsonHelper.getAsJsonObject(o, "shape");
        String type = GsonHelper.getAsString(s, "type", "sphere");
        return switch (type) {
            case "sphere" -> FragmentProfile.Shape.SPHERE;
            case "cone" -> {
                float a = GsonHelper.getAsFloat(s, "half_angle");
                if (a <= 0 || a >= 180) throw new JsonSyntaxException("half_angle must be in (0, 180)");
                yield new FragmentProfile.Shape(a);
            }
            default -> throw new JsonSyntaxException("unknown shape '" + type + "'");
        };
    }
}