package dev.lumaglass.client;

import com.mojang.logging.LogUtils;
import dev.lumaglass.GlassConfig;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;
import java.util.function.DoubleConsumer;

public final class GlassScreen extends Screen implements dev.lumaglass.api.client.GlassThemeExempt {
    private final Screen parent;
    private dev.lumaglass.api.client.GlassCanvas canvas;
    private dev.lumaglass.api.client.GlassFrame glassFrame;
    private final PanoramaRenderer panorama = new PanoramaRenderer(
            new CubeMap(new ResourceLocation("textures/gui/title/background/panorama")));
    private float blur = GlassConfig.BLUR.get().floatValue();
    private float refraction = GlassConfig.REFRACTION.get().floatValue();
    private float frost = GlassConfig.FROST.get().floatValue();
    private float tint = GlassConfig.TINT.get().floatValue();
    private float scale, originX, originY, pixelScale;
    private float cardX = 0, cardY = 58;
    private boolean dragging, pattern, enabled = true;
    private float dragOffsetX, dragOffsetY;
    private int frames;

    public GlassScreen(Screen parent) {
        super(tr("title"));
        this.parent = parent;
    }

    private static Component tr(String key) { return Component.translatable("screen.lumaglass." + key); }

    @Override
    protected void init() {
        scale = Math.min(1.5f, Math.min((width - 24) / 460f, (height - 24) / 280f));
        scale = Math.max(0.1f, scale);
        originX = (width - 460 * scale) / 2;
        originY = (height - 280 * scale) / 2;
        pixelScale = (float)minecraft.getWindow().getGuiScale() * scale;
        dragging = false;
        addRenderableWidget(new MaterialSlider(248, 84, "blur", blur / 16, 16, v -> blur = (float)v));
        addRenderableWidget(new MaterialSlider(248, 119, "refraction", refraction / 24, 24, v -> refraction = (float)v));
        addRenderableWidget(new MaterialSlider(248, 154, "frost", frost, 1, v -> frost = (float)v));
        addRenderableWidget(new MaterialSlider(248, 189, "tint", tint / .4, .4, v -> tint = (float)v));
        addRenderableWidget(new GlassButton(0, 245, 88, tr("compare"), () -> enabled = !enabled));
        addRenderableWidget(new GlassButton(96, 245, 88, tr("pattern"), () -> pattern = !pattern));
        addRenderableWidget(new GlassButton(192, 245, 112, tr("glowPalette"), () -> minecraft.setScreen(new GlowPaletteScreen(this))));
        addRenderableWidget(new GlassButton(312, 245, 68, tr("reset"), () -> {
            blur = 5; refraction = 9; frost = .65f; tint = .12f;
            cardX = 0; cardY = 58; enabled = true;
            GlassConfig.GLOW_ENABLED.set(true); GlassConfig.GLOW_COLOR.set(0xffffff); GlassConfig.GLOW_STRENGTH.set(1.0);
            rebuildWidgets();
        }));
        addRenderableWidget(new GlassButton(388, 245, 72, tr("done"), this::onClose));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (minecraft.level == null) {
            panorama.render(partialTick, 1);
            g.fillGradient(0, 0, width, height, 0x20181818, 0x70313131);
        }
        if (pattern) drawPattern(g);
        // Flush all background geometry before copying the actual framebuffer.
        if (canvas == null) canvas = new dev.lumaglass.api.client.GlassCanvas();
        glassFrame = canvas.begin(g, blur, pixelScale);
        float mx = (mouseX - originX) / scale, my = (mouseY - originY) / scale;
        g.pose().pushPose();
        g.pose().translate(originX, originY, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, "L U M A / G L A S S", 0, 4, 0xffffffff, false);
        g.drawString(font, tr("subtitle"), 0, 23, 0xffe2ecf6, false);
        g.drawString(font, "FORGE 1.20.1", 374, 4, 0xffe2ecf6, false);

        surface(g, cardX, cardY, 214, 169, 25, 1, mx, my);
        g.drawString(font, tr("live"), (int)cardX + 18, (int)cardY + 17, 0xffeef6ff, false);
        g.pose().pushPose();
        g.pose().translate(cardX + 18, cardY + 41, 0);
        g.pose().scale(2.1f, 2.1f, 1);
        g.drawString(font, tr("hero1"), 0, 0, 0xffffffff, true);
        g.drawString(font, tr("hero2"), 0, 13, 0xffffffff, true);
        g.pose().popPose();
        g.drawString(font, tr("drag"), (int)cardX + 18, (int)cardY + 109, 0xffe8f1fa, true);
        g.drawString(font, enabled ? tr("on") : tr("off"), (int)cardX + 18, (int)cardY + 144, 0xffffffff, true);
        g.renderItem(new ItemStack(Items.DIAMOND), (int)cardX + 170, (int)cardY + 137);
        // Controls stay above the movable card, matching their input priority.
        surface(g, 232, 52, 228, 180, 20, 0.7f, mx, my);
        g.drawString(font, tr("material"), 248, 66, 0xffffffff, true);
        super.render(g, (int)mx, (int)my, partialTick);
        if (!GlassRenderer.available()) g.drawString(font, tr("unavailable"), 0, 275, 0xffffbb77);
        g.flush();
        g.pose().popPose();
        glassFrame.close();
        glassFrame = null;
        capturePreview();
    }

    private void surface(GuiGraphics g, float x, float y, float w, float h, float radius,
                         float strength, float mx, float my) {
        if (enabled) glassFrame.panel(x, y, w, h, new dev.lumaglass.api.client.GlassStyle(
                radius, refraction * strength, frost, tint, 1, 1), mx, my);
        else g.renderOutline((int)x, (int)y, (int)w, (int)h, 0x60ffffff);
    }

    private void drawPattern(GuiGraphics g) {
        g.fillGradient(0, 0, width, height, 0xff1e719c, 0xff322e68);
        g.fillGradient(width/8, 0, width/2, height, 0xfff8bd7f, 0xffdb4e87);
        g.fillGradient(width*2/3, 0, width, height, 0xff72e0c1, 0xff3679c3);
        for (int x = 0; x < width; x += 16) g.fill(x, 0, x+1, height, 0x60ffffff);
        for (int y = 0; y < height; y += 16) g.fill(0, y, width, y+1, 0x60ffffff);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        double lx = (x-originX)/scale, ly = (y-originY)/scale;
        if (super.mouseClicked(lx, ly, button)) return true;
        if (button == 0 && lx >= cardX && lx <= cardX+214 && ly >= cardY && ly <= cardY+169) {
            dragging = true;
            dragOffsetX = (float)lx-cardX;
            dragOffsetY = (float)ly-cardY;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (dragging && button == 0) {
            cardX = Mth.clamp((float)(x-originX)/scale-dragOffsetX, 0, 246);
            cardY = Mth.clamp((float)(y-originY)/scale-dragOffsetY, 40, 65);
            return true;
        }
        return super.mouseDragged((x-originX)/scale, (y-originY)/scale, button, dx/scale, dy/scale);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        dragging = false;
        return super.mouseReleased((x-originX)/scale, (y-originY)/scale, button);
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { minecraft.setScreen(parent); }

    @Override
    public void removed() {
        GlassConfig.BLUR.set((double)blur);
        GlassConfig.REFRACTION.set((double)refraction);
        GlassConfig.FROST.set((double)frost);
        GlassConfig.TINT.set(Math.min(0.4, (double)tint));
        GlassConfig.SPEC.save();
        if (canvas != null) canvas.close();
        canvas = null;
    }

    private final class GlassButton extends AbstractButton {
        private final Runnable action;
        private float hover;
        GlassButton(int x, int y, int w, Component text, Runnable action) {
            super(x, y, w, 27, text);
            this.action = action;
        }
        @Override public void onPress() { action.run(); }
        @Override protected void updateWidgetNarration(NarrationElementOutput n) { defaultButtonNarrationText(n); }
        @Override protected void renderWidget(GuiGraphics g, int mx, int my, float partialTick) {
            hover = Mth.lerp(.18f, hover, isHoveredOrFocused() ? 1 : 0);
            glassFrame.panel(getX(), getY()-hover, width, height,
                    new dev.lumaglass.api.client.GlassStyle(13, refraction*.4f, frost,
                            tint+.06f+hover*.09f, 1+hover, 1), mx, my);
            g.drawCenteredString(font, getMessage(), getX()+width/2, getY()+9-(int)hover, 0xffffffff);
        }
    }

    private final class MaterialSlider extends AbstractSliderButton {
        private final String label;
        private final double max;
        private final DoubleConsumer consumer;
        MaterialSlider(int x, int y, String label, double value, double max, DoubleConsumer consumer) {
            super(x, y, 196, 29, Component.empty(), value);
            this.label = label;
            this.max = max;
            this.consumer = consumer;
            updateMessage();
        }
        @Override protected void updateMessage() {
            String number = max <= 1 ? String.format(Locale.ROOT, "%.0f%%", value*max*100)
                    : String.format(Locale.ROOT, "%.1f", value*max);
            setMessage(tr(label).copy().append("  " + number));
        }
        @Override protected void applyValue() { consumer.accept(value*max); }
        @Override public void renderWidget(GuiGraphics g, int mx, int my, float partialTick) {
            g.drawString(font, getMessage(), getX(), getY(), 0xfff4f7ff, true);
            g.fill(getX()+4, getY()+20, getX()+width-4, getY()+22, 0x608eafd0);
            int knobX = getX()+4+(int)(value*(width-8));
            g.fill(getX()+4, getY()+20, knobX, getY()+22, 0xffe8f8ff);
            glassFrame.panel(knobX-5, getY()+16, 10, 10,
                    new dev.lumaglass.api.client.GlassStyle(5, 1, .5f,
                            isHoveredOrFocused() ? .4f : .25f, 1, 1), mx, my);
        }
    }

    /** Explicit development flag only; never writes screenshots in normal gameplay. */
    private void capturePreview() {
        if (!Boolean.getBoolean("lumaglass.capture")) return;
        frames++;
        if (frames == 80 || frames == 110 || frames == 140) {
            String name = frames == 80 ? "lumaglass-preview.png" : frames == 110 ? "lumaglass-grid.png" : "lumaglass-baseline.png";
            Screenshot.grab(minecraft.gameDirectory, name, minecraft.getMainRenderTarget(),
                    c -> LogUtils.getLogger().info("LumaGlass capture: {}", c.getString()));
        }
        if (frames == 90) pattern = true;
        if (frames == 120) enabled = false;
        if (frames == 150) { enabled = true; pattern = false; }
    }
}

