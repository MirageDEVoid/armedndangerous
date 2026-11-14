package com.miradev.and.network;

import com.miradev.and.Reference;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ANDNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Reference.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        CHANNEL.registerMessage(0, S2CResidueInfo.class,
                S2CResidueInfo::encode, S2CResidueInfo::decode, S2CResidueInfo::handle);

        CHANNEL.registerMessage(1, S2CFragmentBurst.class, S2CFragmentBurst::encode,
                S2CFragmentBurst::decode, S2CFragmentBurst::handle);
    }
}