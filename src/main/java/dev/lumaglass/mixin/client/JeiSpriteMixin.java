package dev.lumaglass.mixin.client;

import dev.lumaglass.client.theme.JeiGlass;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="mezz.jei.common.gui.elements.DrawableSprite",remap=false)
public abstract class JeiSpriteMixin {
    @Shadow @Final private ResourceLocation location;
    @Shadow @Final private int width,height;
    @Shadow private int trimLeft,trimRight,trimTop,trimBottom;

    @Inject(method="draw(Lnet/minecraft/client/gui/GuiGraphics;IIIIII)V",at=@At("HEAD"),cancellable=true,require=0)
    private void lumaglass$surface(GuiGraphics g,int x,int y,int top,int bottom,int left,int right,CallbackInfo ci) {
        int l=left+trimLeft,r=right+trimRight,t=top+trimTop,b=bottom+trimBottom;
        if(JeiGlass.draw(g,location,x+l,y+t,width-l-r,height-t-b))ci.cancel();
    }
}
