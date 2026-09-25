package dev.lumaglass.mixin.client;

import dev.lumaglass.client.theme.VanillaGlass;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TooltipRenderUtil.class)
public abstract class TooltipMixin {
    // Forge-added overload: its name is intentionally not remapped.
    @Inject(method="renderTooltipBackground(Lnet/minecraft/client/gui/GuiGraphics;IIIIIIIII)V",remap=false,
            at=@At("HEAD"),cancellable=true)
    private static void lumaglass$tooltip(GuiGraphics g,int x,int y,int w,int h,int z,int bg1,int bg2,int border1,int border2,CallbackInfo ci) {
        if (VanillaGlass.panel(g,x-4,y-4,w+8,h+8,6,false)) ci.cancel();
    }
}
