package dev.lumaglass.mixin.client;

import dev.lumaglass.client.theme.VanillaGlass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

@Mixin(ChatComponent.class)
public abstract class ChatMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private List<GuiMessage.Line> trimmedMessages;
    @Shadow private int chatScrollbarPos;
    @Shadow public abstract int getLinesPerPage();
    @Shadow private boolean isChatFocused() { throw new AssertionError(); }
    @Shadow private static double getTimeFactor(int age) { throw new AssertionError(); }
    @Shadow private int getLineHeight() { throw new AssertionError(); }
    @Unique private int lumaglass$rows;
    @Unique private int lumaglass$alpha;
    @Unique private boolean lumaglass$painted;

    @Inject(method="render",at=@At("HEAD"))
    private void lumaglass$measure(GuiGraphics g,int ticks,int mx,int my,CallbackInfo ci) {
        lumaglass$rows=0; lumaglass$alpha=0; lumaglass$painted=false;
        if (!VanillaGlass.active()) return;
        boolean focused=isChatFocused();
        double textOpacity=minecraft.options.chatOpacity().get()*.9+.1;
        for(int i=0;i<getLinesPerPage() && i+chatScrollbarPos<trimmedMessages.size();i++) {
            int age=ticks-trimmedMessages.get(i+chatScrollbarPos).addedTime();
            double fade=focused?1:getTimeFactor(age);
            if((age<200 || focused) && (int)(255*fade*textOpacity)>3) {
                lumaglass$rows=i+1;
                lumaglass$alpha=Math.max(lumaglass$alpha,(int)(255*fade*minecraft.options.textBackgroundOpacity().get()));
            }
        }
    }

    @Redirect(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V",ordinal=0))
    private void lumaglass$history(GuiGraphics g,int x1,int y1,int x2,int y2,int color) {
        if (!VanillaGlass.active()) { g.fill(x1,y1,x2,y2,color); return; }
        if (!lumaglass$painted && lumaglass$rows>0 && lumaglass$alpha>0) {
            VanillaGlass.panel(g,x1,y2-lumaglass$rows*getLineHeight(),x2-x1,
                    lumaglass$rows*getLineHeight(),6,false,Math.min(1,lumaglass$alpha/110f));
        }
        lumaglass$painted=true;
    }
}
