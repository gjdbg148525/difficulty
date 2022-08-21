package com.tighug.difficulty.util;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    public static final ForgeConfigSpec COMMON_CONFIG;
    public static final ForgeConfigSpec.DoubleValue DOUBLE_VALUE;
    public static final ForgeConfigSpec.DoubleValue MAX_VALUE;
    public static final ForgeConfigSpec.IntValue INITIAL_VALUE;
    public static final ForgeConfigSpec.IntValue INT_VALUE;

    static {
        ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();
        CLIENT_BUILDER.push("server");
        DOUBLE_VALUE = CLIENT_BUILDER.defineInRange("difficulty_factor", 1d, 0d, 10d);
        INITIAL_VALUE = CLIENT_BUILDER.defineInRange("initial_difficulty", 0, -Integer.MAX_VALUE, Integer.MAX_VALUE);
        MAX_VALUE = CLIENT_BUILDER.defineInRange("max_difficulty", Float.MAX_VALUE, 0, Float.MAX_VALUE);
        CLIENT_BUILDER.pop();
        CLIENT_BUILDER.push("client");
        INT_VALUE = CLIENT_BUILDER.defineInRange("min_attack_interval", 10, 1, 20);
        CLIENT_BUILDER.pop();
        COMMON_CONFIG = CLIENT_BUILDER.build();
    }
}
