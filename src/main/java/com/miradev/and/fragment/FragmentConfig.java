package com.miradev.and.fragment;

import net.minecraftforge.common.ForgeConfigSpec;

public final class FragmentConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue SELF_DAMAGE_PERCENT;
    public static final ForgeConfigSpec.BooleanValue FRAGMENTS_BREAK_FRAGILE;
    public static final ForgeConfigSpec.DoubleValue PIPEBOMB_BLAST_RADIUS;
    public static final ForgeConfigSpec.BooleanValue DEBUG_FEEDBACK;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("fragmentation");
        SELF_DAMAGE_PERCENT = b
                .comment("Fragment damage the thrower takes from their own bomb, in percent. 0 = immune, 100 = full.")
                .defineInRange("self_damage_percent", 25, 0, Integer.MAX_VALUE);
        FRAGMENTS_BREAK_FRAGILE = b
                .comment("Whether fragments can break glass and other fragile blocks (jeg's own glass-breaking config must also allow it).")
                .define("fragments_break_fragile", true);
        b.pop();
        b.push("pipebomb");
        PIPEBOMB_BLAST_RADIUS = b
                .comment("Radius of the pipebomb's own explosion. 0 = no blast, fragments only.")
                .defineInRange("blast_radius", 2.0, 0.0, 32.0);
        b.pop();
        DEBUG_FEEDBACK = b.comment("Action-bar summary to the thrower after each burst (hits, damage, fragile blocks broken).")
                .define("debug_feedback", false);
        SPEC = b.build();
    }

    private FragmentConfig() {}
}