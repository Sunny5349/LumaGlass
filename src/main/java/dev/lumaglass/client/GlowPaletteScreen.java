package dev.lumaglass.client;

import dev.lumaglass.GlassConfig;
import dev.lumaglass.api.client.GlassThemedScreen;
import dev.lumaglass.client.theme.VanillaGlass;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/** Live global glow controls. Closing with Done or Escape persists the same settings. */
public final class GlowPaletteScreen extends Screen implements GlassThemedScreen {
    private final Screen parent;
    private float hue, saturation, brightness;
    private int left, top, panelWidth;
    private EditBox hex;
    private boolean syncing;
    private HueSlider hueSlider;

    public GlowPaletteScreen(Screen parent) {
        super(tr("glowPalette"));
        this.parent = parent;
        readColor(GlassConfig.GLOW_COLOR.get());
    }

    private static Component tr(String key) { return Component.translatable("screen.lumaglass." + key); }

    private void readColor(int rgb) {
        float[] hsv = java.awt.Color.RGBtoHSB((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, null);
        hue = hsv[0]; saturation = hsv[1]; brightness = hsv[2];
    }

    @Override protected void init() {
        panelWidth = Math.min(360, width-16);
        left = (width-panelWidth)/2; top = (height-230)/2;
        int right = left+164, rightWidth = panelWidth-176;
        addRenderableWidget(new ColorSquare(left+12, top+38));
        hueSlider = addRenderableWidget(new HueSlider(left+12,top+150));
        hex = new EditBox(font,right,top+50,rightWidth,20,tr("glowHex"));
        hex.setMaxLength(7);
        hex.setFilter(s -> s.matches("#?[0-9a-fA-F]{0,6}"));
        hex.setValue(String.format(Locale.ROOT,"#%06X",GlassConfig.GLOW_COLOR.get()));
        hex.setResponder(s -> {
            if(syncing) return;
            String digits = s.startsWith("#") ? s.substring(1) : s;
            boolean complete = digits.length()==6;
            hex.setTextColor(complete ? 0xffeff5ff : 0xffffb56b);
            if(complete) {
                int rgb = Integer.parseInt(digits,16);
                readColor(rgb); GlassConfig.GLOW_COLOR.set(rgb); hueSlider.refresh();
            }
        });
        addRenderableWidget(hex);
        int[] presets = {0xffffff,0xff7040,0xff70c8,0x9070ff,0x40bfff,0x50efa0};
        int swatchWidth = (rightWidth-10)/6;
        for(int i=0;i<presets.length;i++) {
            final int rgb = presets[i];
            addRenderableWidget(new Button(right+i*(swatchWidth+2),top+80,swatchWidth,20,
                    Component.literal(String.format(Locale.ROOT,"#%06X",rgb)),b -> {
                readColor(rgb); applyColor(); hueSlider.refresh();
            },message -> message.get()) {
                @Override public void renderWidget(GuiGraphics g,int mx,int my,float dt) {
                    int c=0xff000000|rgb;
                    g.fillGradient(getX()+1,getY()+1,getX()+width-1,getY()+height-1,c,c);
                    if(isHoveredOrFocused() || GlassConfig.GLOW_COLOR.get()==rgb)
                        g.renderOutline(getX(),getY(),width,height,0xffffffff);
                }
            });
        }
        addRenderableWidget(Button.builder(toggleLabel(),b -> {
            GlassConfig.GLOW_ENABLED.set(!GlassConfig.GLOW_ENABLED.get()); b.setMessage(toggleLabel());
        }).bounds(right,top+108,rightWidth,20).build());
        addRenderableWidget(new AbstractSliderButton(right,top+136,rightWidth,20,Component.empty(),GlassConfig.GLOW_STRENGTH.get()/2) {
            { updateMessage(); }
            @Override protected void updateMessage() { setMessage(tr("glowStrength").copy().append(" "+Math.round(value*200)+"%")); }
            @Override protected void applyValue() { GlassConfig.GLOW_STRENGTH.set(value*2); }
        });
        addRenderableWidget(Button.builder(tr("reset"),b -> {
            GlassConfig.GLOW_ENABLED.set(true); GlassConfig.GLOW_STRENGTH.set(1.0);
            readColor(0xffffff); applyColor(); rebuildWidgets();
        }).bounds(left+12,top+198,(panelWidth-32)/2,20).build());
        addRenderableWidget(Button.builder(tr("done"),b -> onClose())
                .bounds(left+20+(panelWidth-32)/2,top+198,(panelWidth-32)/2,20).build());
    }

    private Component toggleLabel() {
        return tr("glow").copy().append(": ").append(tr(GlassConfig.GLOW_ENABLED.get() ? "glowOn" : "glowOff"));
    }

    private void applyColor() {
        int rgb = Mth.hsvToRgb(hue,saturation,brightness) & 0xffffff;
        GlassConfig.GLOW_COLOR.set(rgb);
        if(hex!=null) {
            syncing=true;
            hex.setValue(String.format(Locale.ROOT,"#%06X",rgb)); hex.setTextColor(0xffeff5ff);
            syncing=false;
        }
    }

    @Override public void render(GuiGraphics g,int mx,int my,float dt) {
        renderBackground(g);
        VanillaGlass.panel(g,left,top,panelWidth,230,14,false);
        g.drawCenteredString(font,title,width/2,top+12,0xffffffff);
        g.drawString(font,tr("glowHex"),left+164,top+38,0xffeff5ff);
        g.drawCenteredString(font,tr("glowHint"),width/2,top+179,0xffeff5ff);
        super.render(g,mx,my,dt);
    }

    @Override public void tick() { hex.tick(); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public void removed() { GlassConfig.SPEC.save(); }

    private final class HueSlider extends AbstractSliderButton {
        HueSlider(int x,int y) { super(x,y,136,20,Component.empty(),hue); updateMessage(); }
        void refresh() { value=hue; updateMessage(); }
        @Override protected void updateMessage() { setMessage(tr("glowHue").copy().append(" "+Math.round(value*360)+"°")); }
        @Override protected void applyValue() { hue=(float)Math.min(value,.999999); applyColor(); }
        @Override public void renderWidget(GuiGraphics g,int mx,int my,float dt) {
            for(int i=0;i<width;i++) {
                int c=0xff000000|Mth.hsvToRgb(i/(float)width,1,1);
                g.fillGradient(getX()+i,getY(),getX()+i+1,getY()+height,c,c);
            }
            int cursor=getX()+(int)(value*(width-3));
            g.renderOutline(cursor,getY(),3,height,0xffffffff);
            g.drawCenteredString(font,getMessage(),getX()+width/2,getY()+6,0xffffffff);
        }
    }

    private final class ColorSquare extends AbstractWidget {
        ColorSquare(int x,int y) { super(x,y,136,104,tr("glowSquare")); }
        private void choose(double x,double y) {
            saturation=Mth.clamp((float)(x-getX())/(width-1),0,1);
            brightness=1-Mth.clamp((float)(y-getY())/(height-1),0,1);
            applyColor();
        }
        @Override public void onClick(double x,double y) { choose(x,y); }
        @Override protected void onDrag(double x,double y,double dx,double dy) { choose(x,y); }
        @Override public boolean keyPressed(int key,int scan,int modifiers) {
            if(key==GLFW.GLFW_KEY_LEFT || key==GLFW.GLFW_KEY_RIGHT) saturation=Mth.clamp(saturation+(key==GLFW.GLFW_KEY_LEFT?-.02f:.02f),0,1);
            else if(key==GLFW.GLFW_KEY_UP || key==GLFW.GLFW_KEY_DOWN) brightness=Mth.clamp(brightness+(key==GLFW.GLFW_KEY_DOWN?-.02f:.02f),0,1);
            else return super.keyPressed(key,scan,modifiers);
            applyColor(); return true;
        }
        @Override protected void renderWidget(GuiGraphics g,int mx,int my,float dt) {
            for(int x=0;x<width;x++) {
                int color=0xff000000|Mth.hsvToRgb(hue,x/(float)(width-1),1);
                g.fillGradient(getX()+x,getY(),getX()+x+1,getY()+height,color,0xff000000);
            }
            int x=getX()+Math.round(saturation*(width-1)), y=getY()+Math.round((1-brightness)*(height-1));
            g.renderOutline(x-3,y-3,7,7,0xff101010);g.renderOutline(x-2,y-2,5,5,0xffffffff);
            if(isFocused())g.renderOutline(getX()-1,getY()-1,width+2,height+2,0xffffffff);
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput n) {
            n.add(NarratedElementType.TITLE,getMessage().copy().append(" "+hex.getValue()));
            n.add(NarratedElementType.USAGE,tr("glowSquareKeys"));
        }
    }
}
