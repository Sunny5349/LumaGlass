package dev.lumaglass.mixin.client;

import dev.lumaglass.client.theme.VanillaGlass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractButton.class)
public abstract class ButtonMixin extends AbstractWidget {
    protected ButtonMixin(int x, int y, int w, int h, Component c) { super(x,y,w,h,c); }
    @Shadow public abstract void renderString(GuiGraphics g, Font font, int color);

    @Inject(method="renderWidget", at=@At("HEAD"), cancellable=true)
    private void lumaglass$render(GuiGraphics g, int mx, int my, float dt, CallbackInfo ci) {
        if (VanillaGlass.panel(g,getX(),getY(),width,height,Math.min(10,height*.5f),isHoveredOrFocused(),alpha*(active?1:.5f))) {
            renderString(g, Minecraft.getInstance().font, getFGColor() | Mth.ceil(alpha*255)<<24);
            ci.cancel();
        }
    }
}
