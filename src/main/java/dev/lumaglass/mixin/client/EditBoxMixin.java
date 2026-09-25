package dev.lumaglass.mixin.client;

import dev.lumaglass.client.theme.VanillaGlass;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EditBox.class)
public abstract class EditBoxMixin {
    @Redirect(method="renderWidget",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
    private void lumaglass$border(GuiGraphics g,int x1,int y1,int x2,int y2,int color) {
        EditBox box=(EditBox)(Object)this;
        if (!VanillaGlass.active()) {g.fill(x1,y1,x2,y2,color); return;}
        if (x1 == box.getX()-1) VanillaGlass.panel(g,x1,y1,x2-x1,y2-y1,7,box.isFocused());
        // Only replace the two background fills; vanilla cursor, selection, formatting and IME stay intact.
    }
}
