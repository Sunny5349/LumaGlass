package dev.lumaglass.mixin.client;

import dev.lumaglass.client.theme.VanillaGlass;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeInventoryMixin {
    @Shadow private static CreativeModeTab selectedTab;
    @Shadow private int getTabX(CreativeModeTab tab) { throw new AssertionError(); }
    @Shadow private int getTabY(CreativeModeTab tab) { throw new AssertionError(); }

    @Inject(method="renderTabButton", at=@At("HEAD"), cancellable=true)
    private void lumaglass$roundTab(GuiGraphics g, CreativeModeTab tab, CallbackInfo ci) {
        var screen = (AbstractContainerScreen<?>)(Object)this;
        int x = screen.getGuiLeft() + getTabX(tab);
        int y = screen.getGuiTop() + getTabY(tab) + 3;
        if (!VanillaGlass.panel(g,x,y,26,26,13,tab == selectedTab)) return;
        g.pose().pushPose();
        try {
            g.pose().translate(0,0,100);
            g.renderItem(tab.getIconItem(),x+5,y+5);
            g.renderItemDecorations(net.minecraft.client.Minecraft.getInstance().font,tab.getIconItem(),x+5,y+5);
        } finally { g.pose().popPose(); }
        ci.cancel();
    }

    @Inject(method="checkTabClicked", at=@At("HEAD"), cancellable=true)
    private void lumaglass$roundHit(CreativeModeTab tab, double x, double y, CallbackInfoReturnable<Boolean> ci) {
        if (!VanillaGlass.active()) return;
        double dx = x - getTabX(tab) - 13, dy = y - getTabY(tab) - 16;
        ci.setReturnValue(dx*dx + dy*dy <= 13*13);
    }

    @Inject(method="checkTabHovering",at=@At("HEAD"),cancellable=true)
    private void lumaglass$roundHover(GuiGraphics g,CreativeModeTab tab,int x,int y,CallbackInfoReturnable<Boolean> ci) {
        if (!VanillaGlass.active()) return;
        var screen=(AbstractContainerScreen<?>)(Object)this;
        double dx=x-screen.getGuiLeft()-getTabX(tab)-13,dy=y-screen.getGuiTop()-getTabY(tab)-16;
        boolean inside=dx*dx+dy*dy<=13*13;
        if(inside) g.renderTooltip(net.minecraft.client.Minecraft.getInstance().font,tab.getDisplayName(),x,y);
        ci.setReturnValue(inside);
    }
}
