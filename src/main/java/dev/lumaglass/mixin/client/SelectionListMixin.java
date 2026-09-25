package dev.lumaglass.mixin.client;

import dev.lumaglass.client.theme.VanillaGlass;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSelectionList.class)
public abstract class SelectionListMixin {
    @Shadow protected int width,x0,x1,y0,y1;
    @Shadow private boolean renderBackground,renderTopAndBottom;
    @Unique private boolean lumaglass$changed,lumaglass$background,lumaglass$edges;
    @Inject(method="render",at=@At("HEAD"))
    private void lumaglass$start(GuiGraphics g,int mx,int my,float dt,CallbackInfo ci) {
        lumaglass$changed=VanillaGlass.active();
        if (!lumaglass$changed) return;
        lumaglass$background=renderBackground; lumaglass$edges=renderTopAndBottom;
        renderBackground=false; renderTopAndBottom=false;
    }
    @Inject(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/components/AbstractSelectionList;renderBackground(Lnet/minecraft/client/gui/GuiGraphics;)V",shift=At.Shift.AFTER))
    private void lumaglass$surface(GuiGraphics g,int mx,int my,float dt,CallbackInfo ci) {
        if (!lumaglass$changed) return;
        VanillaGlass.panel(g,x0+2,y0,x1-x0-4,y1-y0,10,false);
    }
    @Inject(method="render",at=@At("RETURN"))
    private void lumaglass$end(GuiGraphics g,int mx,int my,float dt,CallbackInfo ci) {
        if (lumaglass$changed) {renderBackground=lumaglass$background;renderTopAndBottom=lumaglass$edges;}
    }
    @Redirect(method="render", at=@At(value="INVOKE", target="Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"), require=3, allow=3)
    private void lumaglass$scrollbar(GuiGraphics g,int left,int top,int right,int bottom,int color) {
        if (!lumaglass$changed) { g.fill(left,top,right,bottom,color); return; }
        // These three calls draw the track, thumb and the old square bevel respectively.
        // Keep vanilla's positions and input math, replace only the material.
        if (color == 0xff000000) VanillaGlass.scrollbar(g,left+2,top,right-left-4,bottom-top,true,false);
        else if (color == 0xff808080) VanillaGlass.scrollbar(g,left,top,right-left,bottom-top,false,false);
    }
    @Inject(method="renderSelection",at=@At("HEAD"),cancellable=true)
    private void lumaglass$selection(GuiGraphics g,int top,int rowWidth,int rowHeight,int border,int fill,CallbackInfo ci) {
        if (VanillaGlass.panel(g,x0+(width-rowWidth)/2f,top-2,rowWidth,rowHeight+4,6,true)) ci.cancel();
    }
}
