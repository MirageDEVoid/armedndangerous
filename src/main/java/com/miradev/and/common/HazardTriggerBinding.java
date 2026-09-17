package com.miradev.and.common;

import net.minecraft.resources.ResourceLocation;

public class HazardTriggerBinding {
    public String gunId;
    public ResourceLocation hazardType;
    public boolean ignitesOwnedOnHeadshot;

    public float explosionPower = 0.0F; // go to googal for this calculation nerd
    public boolean explosionCausesFire = false;
    public boolean explosionDestroysBlocks = false;
}