package dev.lumaglass.mixin.client;

import dev.lumaglass.client.theme.VanillaGlass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSliderButton.class)
public abstract class SliderMixin extends AbstractWidget {
    @Shadow protected double value;
    protected SliderMixin(int x,int y,int w,int h,Component c) {super(x,y,w,h,c);}
    @Inject(method="renderWidget",at=@At("HEAD"),cancellable=true)
    private void lumaglass$render(GuiGraphics g,int mx,int my,float dt,CallbackInfo ci) {
        if (!VanillaGlass.panel(g,getX(),getY(),width,height,10,isHoveredOrFocused(),alpha)) return;
        VanillaGlass.panel(g,getX()+(float)value*(width-8),getY()+1,8,height-2,4,true,alpha);
        renderScrollingString(g,Minecraft.getInstance().font,2,(active?0xffffff:0xa0a0a0)|Mth.ceil(alpha*255)<<24);
        ci.cancel();
    }
}
