package com.miradev.and.event;

import com.miradev.and.Reference;
import com.miradev.and.common.ResidueTrigger;
import com.miradev.and.common.ResidueProfile;
import com.miradev.and.network.ANDNetwork;
import com.miradev.and.network.S2CResidueInfo;
import com.miradev.and.registry.ResidueRegistry;
import com.miradev.and.registry.ResidueTriggerRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class ResidueSync {

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        S2CResidueInfo msg = build();
        ServerPlayer player = event.getPlayer();
        PacketDistributor.PacketTarget target = player != null
                ? PacketDistributor.PLAYER.with(() -> player)
                : PacketDistributor.ALL.noArg();
        ANDNetwork.CHANNEL.send(target, msg);
    }

    private static S2CResidueInfo build() {
        List<S2CResidueInfo.Entry> out = new ArrayList<>();
        for (ResidueTrigger t : ResidueTriggerRegistry.all()) {
            ResidueProfile type = ResidueRegistry.get(t.residue());
            if (type == null) continue;
            ResidueTrigger.Hit hit = t.onHeadshot() != null ? t.onHeadshot() : t.onBody();
            ResidueTrigger.Explosion ex = hit != null ? hit.explosion() : null;
            out.add(new S2CResidueInfo.Entry(t.gun(), type.radius(), type.lifetime(), type.interval(),
                    hit != null && hit.igniteOwned(),
                    ex != null ? ex.power() : 0F,
                    ex != null && ex.breakBlocks()));
        }
        return new S2CResidueInfo(out);
    }
}