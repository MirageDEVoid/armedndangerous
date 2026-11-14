package com.miradev.and.common;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public record ResidueTrigger(String gun, ResourceLocation residue,
                             @Nullable Hit onHeadshot, @Nullable Hit onBody, @Nullable Hit onBlock) {

    public record Hit(boolean ignite, boolean igniteOwned, @Nullable Explosion explosion) {}
    public record Explosion(float power, boolean fire, boolean breakBlocks) {}
}
