package example.glass;

import dev.lumaglass.api.client.GlassCanvas;
import dev.lumaglass.api.client.GlassFrame;
import dev.lumaglass.api.client.GlassStyle;
import dev.lumaglass.api.client.GlassThemeExempt;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Compiled against the public API; no imports from LumaGlass implementation packages. */
public final class ExampleGlassScreen extends Screen implements GlassThemeExempt {
    private GlassCanvas glass;
    public ExampleGlassScreen() { super(Component.literal("Example mod / Glass API")); }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fillGradient(0,0,width,height,0xff206e9b,0xff713f87);
        for (int x=0;x<width;x+=20) g.fill(x,0,x+1,height,0x50ffffff);
        for (int y=0;y<height;y+=20) g.fill(0,y,width,y+1,0x50ffffff);
        if (glass == null) glass=new GlassCanvas();
        try (GlassFrame frame=glass.begin(g,5)) {
            frame.panel(width/2f-125,height/2f-60,250,120,GlassStyle.FROSTED.withGlowColor(0x40BFFF),mouseX,mouseY);
            frame.panel(width/2f-100,height/2f+18,200,24,GlassStyle.CLEAR.withGlowColor(0xFF7040),mouseX,mouseY);
            // Foreground comes after the panels that form its background.
            g.drawCenteredString(font,"Another mod, the same glass",width/2,height/2-37,0xffffffff);
            g.drawCenteredString(font,"GlassCanvas / API v1",width/2,height/2-17,0xffdbeaff);
            g.drawString(font,"Independent glow colors",width/2-100,height/2+26,0xffffffff);
        }
        super.render(g,mouseX,mouseY,partialTick);
    }

    @Override public void removed() { if(glass!=null) {glass.close();glass=null;} }
}
