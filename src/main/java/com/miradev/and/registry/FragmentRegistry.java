package com.miradev.and.registry;

import com.miradev.and.common.FragmentProfile;
import javax.annotation.Nullable;
import java.util.Map;

public class FragmentRegistry {
    private static volatile Map<String, FragmentProfile> BY_SOURCE = Map.of();

    public static void setAll(Map<String, FragmentProfile> m) { BY_SOURCE = Map.copyOf(m); }

    /** Pass ResidueTriggerRegistry.keyFor(stack) for items. */
    @Nullable
    public static FragmentProfile get(String source) { return BY_SOURCE.get(source); }

    public static int size() { return BY_SOURCE.size(); }
}