package com.miradev.and.network;

import com.miradev.and.client.FragmentClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class S2CFragmentBurst {
    public record Ray(byte kind, float[] points) {}

    private final Vec3 origin;
    private final int color;
    private final List<ResourceLocation> particles;
    private final List<Ray> rays;

    public S2CFragmentBurst(Vec3 origin, int color, List<ResourceLocation> particles, List<Ray> rays) {
        this.origin = origin; this.color = color; this.particles = particles; this.rays = rays;
    }

    public Vec3 origin() { return origin; }
    public int color() { return color; }
    public List<ResourceLocation> particles() { return particles; }
    public List<Ray> rays() { return rays; }

    public static void encode(S2CFragmentBurst m, FriendlyByteBuf buf) {
        buf.writeDouble(m.origin.x); buf.writeDouble(m.origin.y); buf.writeDouble(m.origin.z);
        buf.writeInt(m.color);
        buf.writeCollection(m.particles, FriendlyByteBuf::writeResourceLocation);
        buf.writeVarInt(m.rays.size());
        for (Ray r : m.rays) {
            buf.writeByte(r.kind());
            int n = r.points().length / 3;
            buf.writeByte(n);
            for (float f : r.points()) buf.writeFloat(f);
        }
    }

    public static S2CFragmentBurst decode(FriendlyByteBuf buf) {
        Vec3 origin = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        int color = buf.readInt();
        List<ResourceLocation> particles = buf.readList(FriendlyByteBuf::readResourceLocation);
        int count = buf.readVarInt();
        List<Ray> rays = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            byte kind = buf.readByte();
            int n = buf.readByte();
            float[] pts = new float[n * 3];
            for (int j = 0; j < pts.length; j++) pts[j] = buf.readFloat();
            rays.add(new Ray(kind, pts));
        }
        return new S2CFragmentBurst(origin, color, particles, rays);
    }

    public static void handle(S2CFragmentBurst m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> FragmentClient.play(m)));
        ctx.get().setPacketHandled(true);
    }
}