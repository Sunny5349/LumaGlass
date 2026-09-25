package dev.lumaglass;

import net.minecraftforge.fml.common.Mod;

/** Client classes are registered by Dist.CLIENT subscribers only. */
@Mod(LumaGlass.ID)
public final class LumaGlass {
    public static final String ID = "lumaglass";

    public LumaGlass() {
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.CLIENT, GlassConfig.SPEC);
    }
}
