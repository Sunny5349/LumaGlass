package dev.lumaglass.mixin.client;

import dev.lumaglass.client.theme.VanillaGlass;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.ScreenEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Shadow public int width;
    @Shadow public int height;
    @Inject(method="renderBackground",at=@At("HEAD"),cancellable=true)
    private void lumaglass$background(GuiGraphics g,CallbackInfo ci) {
        if (VanillaGlass.background(g,width,height,false)) {
            MinecraftForge.EVENT_BUS.post(new ScreenEvent.BackgroundRendered((Screen)(Object)this,g));
            ci.cancel();
        }
    }
    @Inject(method="renderDirtBackground",at=@At("HEAD"),cancellable=true)
    private void lumaglass$dirt(GuiGraphics g,CallbackInfo ci) {
        if (VanillaGlass.background(g,width,height,true)) {
            MinecraftForge.EVENT_BUS.post(new ScreenEvent.BackgroundRendered((Screen)(Object)this,g));
            ci.cancel();
        }
    }
}
