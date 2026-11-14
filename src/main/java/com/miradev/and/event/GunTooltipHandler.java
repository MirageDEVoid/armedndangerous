package com.miradev.and.event;

import com.miradev.and.registry.ResidueTooltipInfo;
import com.miradev.and.registry.ResidueTriggerRegistry;
import com.miradev.and.network.S2CResidueInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ttv.migami.jeg.item.GunItem;

@Mod.EventBusSubscriber(modid = "and", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GunTooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof GunItem)) return;
        if (Screen.hasShiftDown()) return;

        S2CResidueInfo.Entry info = ResidueTooltipInfo.get(ResidueTriggerRegistry.keyFor(stack));
        if (info == null) return;

        double durationSeconds = info.lifetime() / 20.0;
        double intervalSeconds = info.interval() / 20.0;

        var tooltip = event.getToolTip();
        tooltip.add(Component.literal(""));
        tooltip.add(Component.translatable("info.and.residue_header").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("info.and.residue_radius", String.format("%.1f", info.radius()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("info.and.residue_duration",
                        String.format("%.1f", durationSeconds), String.format("%.1f", intervalSeconds))
                .withStyle(ChatFormatting.GRAY));

        if (info.igniteOwned()) {
            tooltip.add(Component.translatable("info.and.residue_headshot_ignite").withStyle(ChatFormatting.RED));
        }

        if (info.explosionPower() > 0.0F) {
            MutableComponent line = Component.translatable(
                    info.explosionBreaks() ? "info.and.residue_explosion_destructive" : "info.and.residue_explosion",
                    String.format("%.1f", info.explosionPower()));
            tooltip.add(line.withStyle(ChatFormatting.RED));
        }
    }
}