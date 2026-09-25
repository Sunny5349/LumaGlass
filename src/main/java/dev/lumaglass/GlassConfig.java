package dev.lumaglass;

import net.minecraftforge.common.ForgeConfigSpec;

public final class GlassConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue BLUR, REFRACTION, FROST, TINT, GLOW_STRENGTH;
    public static final ForgeConfigSpec.BooleanValue VANILLA_UI, OTHER_MOD_UI, GLOW_ENABLED;
    public static final ForgeConfigSpec.IntValue GLOW_COLOR;
    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("LumaGlass client material settings.");
        VANILLA_UI = b.comment("Apply liquid glass to vanilla UI and HUD by default.").define("vanillaUi", true);
        OTHER_MOD_UI = b.comment("Also theme vanilla widgets used on other mods' screens. API drawing is independent.")
                .define("otherModUi", false);
        BLUR = b.comment("Blur radius in interface pixels").defineInRange("blur", 5.0, 0.0, 16.0);
        REFRACTION = b.comment("Displacement at the curved glass edge").defineInRange("refraction", 9.0, 0.0, 24.0);
        FROST = b.comment("Mix between clear and frosted background").defineInRange("frost", 0.65, 0.0, 1.0);
        TINT = b.comment("Glass surface tint").defineInRange("tint", 0.12, 0.0, 0.4);
        GLOW_ENABLED = b.comment("Enable added rim and pointer glow; blur/refraction remain when disabled.").define("glowEnabled", true);
        GLOW_COLOR = b.comment("Glow RGB color as an integer, 0xFFFFFF = white.").defineInRange("glowColor", 0xffffff, 0, 0xffffff);
        GLOW_STRENGTH = b.comment("Added glow intensity; zero removes glow.").defineInRange("glowStrength", 1.0, 0.0, 2.0);
        SPEC = b.build();
    }
    private GlassConfig() {}
}
