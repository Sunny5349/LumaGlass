package dev.lumaglass.mixin.client;

import dev.lumaglass.client.theme.VanillaGlass;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SelectWorldScreen.class)
public abstract class SelectWorldScreenMixin {
    @Inject(method="render", at=@At("HEAD"))
    private void lumaglass$background(GuiGraphics g, int mx, int my, float dt, CallbackInfo ci) {
        // Unlike multiplayer, this screen relies entirely on the list's opaque dirt backdrop.
        // Paint a fresh background before replacing that dirt with transparent glass.
        var screen = (SelectWorldScreen)(Object)this;
        VanillaGlass.background(g, screen.width, screen.height, false);
    }
}
