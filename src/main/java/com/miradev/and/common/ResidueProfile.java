package com.miradev.and.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.List;

public record ResidueProfile(
        ResourceLocation id,
        double radius,
        int lifetime,
        int interval,
        Effect effect,
        boolean affectsOwner,
        Look visual,
        @Nullable Ignition ignition) {

    public record Effect(ResourceLocation id, int amplifier, int duration, boolean skipFireImmune,
                         boolean stacking, int maxDuration) {}

    public record Look(ResourceLocation sprite, int color, List<ResourceLocation> particles) {
        public void write(FriendlyByteBuf buf) {
            buf.writeResourceLocation(sprite);
            buf.writeInt(color);
            buf.writeCollection(particles, FriendlyByteBuf::writeResourceLocation);
        }
        public static Look read(FriendlyByteBuf buf) {
            return new Look(buf.readResourceLocation(), buf.readInt(), buf.readList(FriendlyByteBuf::readResourceLocation));
        }
    }

    public record Ignition(Look look, List<ResourceLocation> ignitedBy, boolean spreads) {}
}
