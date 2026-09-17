package com.miradev.and.registry;

import com.miradev.and.common.HazardTriggerBinding;
import java.util.HashMap;
import java.util.Map;

public class HazardTriggerRegistry {
    private static Map<String, HazardTriggerBinding> BINDINGS = new HashMap<>();

    public static void setAll(Map<String, HazardTriggerBinding> bindings) {
        BINDINGS = bindings;
    }

    public static HazardTriggerBinding get(String gunId) {
        return BINDINGS.get(gunId);
    }
}