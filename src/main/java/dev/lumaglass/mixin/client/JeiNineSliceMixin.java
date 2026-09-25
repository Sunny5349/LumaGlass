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
@Mixin(targets="mezz.jei.common.gui.elements.DrawableNineSliceTexture",remap=false)
public abstract class JeiNineSliceMixin {
    @Shadow @Final private ResourceLocation location;

    @Inject(method="draw(Lnet/minecraft/client/gui/GuiGraphics;IIII)V",at=@At("HEAD"),cancellable=true,require=0)
    private void lumaglass$surface(GuiGraphics g,int x,int y,int w,int h,CallbackInfo ci) {
        if(JeiGlass.draw(g,location,x,y,w,h))ci.cancel();
    }
}
