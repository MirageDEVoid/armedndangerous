package com.miradev.and.event;

import com.miradev.and.common.HazardTriggerBinding;
import com.miradev.and.common.HazardZoneType;
import com.miradev.and.registry.HazardTriggerRegistry;
import com.miradev.and.registry.HazardZoneRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import ttv.migami.jeg.item.GunItem;

@Mod.EventBusSubscriber(modid = "and", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GunTooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        if (!(stack.getItem() instanceof GunItem)) return;

        if (Screen.hasShiftDown()) return;

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) return;

        HazardTriggerBinding binding = HazardTriggerRegistry.get(itemId.toString());
        if (binding == null) return;

        HazardZoneType type = HazardZoneRegistry.get(binding.hazardType);
        if (type == null) return;

        double radius = type.radius;
        double durationSeconds = type.duration / 20.0;
        double tickDamageInterval = type.tickInterval / 20.0;

        var tooltip = event.getToolTip();

        tooltip.add(Component.literal(""));
        tooltip.add(Component.translatable("info.and.hazard_header").withStyle(ChatFormatting.GOLD));

        tooltip.add(Component.translatable("info.and.hazard_radius", String.format("%.1f", radius))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("info.and.hazard_duration",
                        String.format("%.1f", durationSeconds), String.format("%.1f", tickDamageInterval))
                .withStyle(ChatFormatting.GRAY));

        if (binding.ignitesOwnedOnHeadshot) {
            tooltip.add(Component.translatable("info.and.hazard_headshot_ignite").withStyle(ChatFormatting.RED));
        }

        if (binding.explosionPower > 0.0F) {
            MutableComponent explosionLine;
            if (binding.explosionDestroysBlocks) {
                explosionLine = Component.translatable("info.and.hazard_explosion_destructive",
                        String.format("%.1f", binding.explosionPower));
            } else {
                explosionLine = Component.translatable("info.and.hazard_explosion",
                        String.format("%.1f", binding.explosionPower));
            }
            tooltip.add(explosionLine.withStyle(ChatFormatting.RED));
        }
    }
}