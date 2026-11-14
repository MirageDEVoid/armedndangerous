package com.miradev.and.registry;

import com.miradev.and.network.S2CResidueInfo;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResidueTooltipInfo {
    private static volatile Map<String, S2CResidueInfo.Entry> BY_GUN = Map.of();

    public static void set(List<S2CResidueInfo.Entry> entries) {
        Map<String, S2CResidueInfo.Entry> map = new HashMap<>();
        for (S2CResidueInfo.Entry e : entries) map.put(e.gun(), e);
        BY_GUN = map;
    }

    @Nullable
    public static S2CResidueInfo.Entry get(String gunKey) { return BY_GUN.get(gunKey); }
}