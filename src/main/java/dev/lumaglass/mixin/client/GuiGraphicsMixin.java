package dev.lumaglass.mixin.client;

import dev.lumaglass.client.theme.VanillaGlass;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @Inject(method="innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFF)V",at=@At("HEAD"),cancellable=true)
    private void lumaglass$texture(ResourceLocation texture,int x1,int x2,int y1,int y2,int z,
                                   float u0,float u1,float v0,float v1,CallbackInfo ci) {
        if (VanillaGlass.texture((GuiGraphics)(Object)this,texture,x1,x2,y1,y2,u0,u1,v0,v1)) ci.cancel();
    }
    @Inject(method="blitNineSliced(Lnet/minecraft/resources/ResourceLocation;IIIIIIIIIIII)V",at=@At("HEAD"),cancellable=true)
    private void lumaglass$nineSlice(ResourceLocation texture,int x,int y,int w,int h,int left,int top,int right,int bottom,
                                     int sw,int sh,int u,int v,CallbackInfo ci) {
        if (texture.getNamespace().equals("minecraft") &&
                (texture.getPath().equals("textures/gui/tab_button.png") || VanillaGlass.skinTexture("minecraft",texture.getPath(),v/256f))
                && VanillaGlass.panel((GuiGraphics)(Object)this,x,y,w,h,Math.min(10,h*.5f),v==0)) ci.cancel();
    }
    @Inject(method="fill(Lnet/minecraft/client/renderer/RenderType;IIIIII)V",at=@At("HEAD"),cancellable=true)
    private void lumaglass$fill(RenderType type,int x1,int y1,int x2,int y2,int z,int color,CallbackInfo ci) {
        if (VanillaGlass.backing((GuiGraphics)(Object)this,x1,y1,x2,y2,color)) ci.cancel();
    }
    // Both float-coordinate overloads are added by Forge and keep their names in production.
    @ModifyVariable(method={"drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;FFIZ)I",
            "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;FFIZ)I"},
            at=@At("HEAD"),argsOnly=true,ordinal=0,remap=false)
    private int lumaglass$text(int color) {return VanillaGlass.readable(color);}
}
