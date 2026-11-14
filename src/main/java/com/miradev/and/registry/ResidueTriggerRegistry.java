package com.miradev.and.registry;

import com.miradev.and.common.ResidueTrigger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

public class ResidueTriggerRegistry {
    private static volatile Map<String, ResidueTrigger> TRIGGERS = Map.of();

    public static Collection<ResidueTrigger> all() { return TRIGGERS.values(); }

    public static void setAll(Map<String, ResidueTrigger> triggers) { TRIGGERS = Map.copyOf(triggers); }
    @Nullable
    public static ResidueTrigger get(String key) { return TRIGGERS.get(key); }

    /** GunId for data-driven guns, item registry id for everything else. */
    public static String keyFor(ItemStack weapon) {
        CompoundTag tag = weapon.getTag();
        if (tag != null && tag.contains("GunId", Tag.TAG_STRING)) return tag.getString("GunId");
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(weapon.getItem());
        return id == null ? "" : id.toString();
    }
}
