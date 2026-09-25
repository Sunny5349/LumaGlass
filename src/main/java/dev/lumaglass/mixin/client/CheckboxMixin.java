package dev.lumaglass.mixin.client;

import dev.lumaglass.client.theme.VanillaGlass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Checkbox.class)
public abstract class CheckboxMixin extends AbstractWidget {
    @Shadow private boolean selected;
    @Shadow @Final private boolean showLabel;
    protected CheckboxMixin(int x,int y,int w,int h,Component c) {super(x,y,w,h,c);}
    @Inject(method="renderWidget",at=@At("HEAD"),cancellable=true)
    private void lumaglass$render(GuiGraphics g,int mx,int my,float dt,CallbackInfo ci) {
        if (!VanillaGlass.panel(g,getX(),getY(),20,height,7,isHoveredOrFocused()||selected,alpha)) return;
        var font=Minecraft.getInstance().font;
        if (selected) g.drawCenteredString(font,"✓",getX()+10,getY()+(height-8)/2,0xffd0ffff);
        if (showLabel) g.drawString(font,getMessage(),getX()+24,getY()+(height-8)/2,0xffeff5ff);
        ci.cancel();
    }
}
