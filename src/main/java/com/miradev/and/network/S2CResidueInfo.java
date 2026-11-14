package com.miradev.and.network;

import com.miradev.and.registry.ResidueTooltipInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class S2CResidueInfo {
    public record Entry(String gun, double radius, int lifetime, int interval,
                        boolean igniteOwned, float explosionPower, boolean explosionBreaks) {}

    private final List<Entry> entries;

    public S2CResidueInfo(List<Entry> entries) { this.entries = entries; }

    public static void encode(S2CResidueInfo msg, FriendlyByteBuf buf) {
        buf.writeCollection(msg.entries, (b, e) -> {
            b.writeUtf(e.gun());
            b.writeDouble(e.radius());
            b.writeVarInt(e.lifetime());
            b.writeVarInt(e.interval());
            b.writeBoolean(e.igniteOwned());
            b.writeFloat(e.explosionPower());
            b.writeBoolean(e.explosionBreaks());
        });
    }

    public static S2CResidueInfo decode(FriendlyByteBuf buf) {
        return new S2CResidueInfo(buf.readList(b -> new Entry(
                b.readUtf(), b.readDouble(), b.readVarInt(), b.readVarInt(),
                b.readBoolean(), b.readFloat(), b.readBoolean())));
    }

    public static void handle(S2CResidueInfo msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ResidueTooltipInfo.set(msg.entries));
        ctx.get().setPacketHandled(true);
    }
}
