package com.miradev.and.client;

import com.miradev.and.common.ANDAdvantages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import ttv.migami.jeg.common.Gun;
import ttv.migami.jeg.item.GunItem;

public class StealthAdvantageChecker {

    public static boolean isHoldingStealthGun() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return false;

        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof GunItem gunItem)) return false;

        Gun gun = gunItem.getModifiedGun(held);
        if (gun == null) return false;

        return ANDAdvantages.STEALTH.equals(gun.getProjectile().getAdvantage());
    }
}