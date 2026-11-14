package com.miradev.and.common;

import net.minecraft.resources.ResourceLocation;
import javax.annotation.Nullable;
import java.util.List;

public record FragmentProfile(
        ResourceLocation id,
        List<String> sources,
        int count, // total frag
        double range, // total range, shared to bounce
        Shape shape,
        float damage, // per frag before all calculations
        float falloff, // % damage left at max range
        int pierce,
        double hitRadius, // fragment thickness
        int bounce, // ricochet only for solid blocks
        double bounceLoss, // damage multi per bounce
        double aimBias, // % of frags aimed at living targets, the rest fly free
        int maxAimed, // aimed frags cap, the rest fly free
        ResourceLocation advantage,
        boolean affectsOwner,
        boolean breaksFragile,
        @Nullable ResidueProfile.Effect effect,
        Look visual) {

    public record Shape(float halfAngle) {
        public static final Shape SPHERE = new Shape(180F);
        public boolean isSphere() { return halfAngle >= 180F; }
    }

    public record Look(int color, List<ResourceLocation> particles) {}
}