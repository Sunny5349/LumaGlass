package dev.lumaglass.client.theme;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.lumaglass.GlassConfig;
import dev.lumaglass.LumaGlass;
import dev.lumaglass.api.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Internal adapter. The public API does not depend on these automatic theme hooks. */
@Mod.EventBusSubscriber(modid = LumaGlass.ID, value = Dist.CLIENT)
public final class VanillaGlass {
    private static GlassCanvas canvas;
    private static GlassFrame frame;
    private static PanoramaRenderer panorama;
    private static int drawing;
    private static boolean hudPass;

    private VanillaGlass() {}

    public static boolean active() {
        if (drawing > 0 || !GlassCanvas.isAvailable() || !GlassConfig.SPEC.isLoaded() || !GlassConfig.VANILLA_UI.get()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getOverlay() != null) return false;
        Screen screen = mc.screen;
        return hudPass || screen == null || (!(screen instanceof GlassThemeExempt)
                && (screen instanceof GlassThemedScreen || screen.getClass().getName().startsWith("net.minecraft.") || GlassConfig.OTHER_MOD_UI.get()));
    }

    public static GlassStyle style(float radius, boolean focused) {
        return new GlassStyle(radius, GlassConfig.REFRACTION.get().floatValue(),
                GlassConfig.FROST.get().floatValue(), Math.min(.8f, GlassConfig.TINT.get().floatValue() + (focused ? .13f : 0)),
                focused ? 1.7f : 1, 1);
    }

    private static GlassFrame frame(GuiGraphics g) {
        if (canvas == null) canvas = new GlassCanvas();
        if (frame == null) frame = canvas.begin(g, GlassConfig.BLUR.get().floatValue());
        return frame;
    }

    public static boolean panel(GuiGraphics g, float x, float y, float w, float h, float radius, boolean focused) {
        return panel(g, x, y, w, h, radius, focused, 1);
    }

    public static boolean panel(GuiGraphics g, float x, float y, float w, float h, float radius, boolean focused, float opacity) {
        if (!active() || w <= 0 || h <= 0) return false;
        drawing++;
        try {
            Minecraft mc = Minecraft.getInstance();
            // Shader pointer is expressed in this draw's local GUI coordinate system.
            org.joml.Vector4f point = new org.joml.Vector4f(
                    (float)(mc.mouseHandler.xpos() / mc.getWindow().getGuiScale()),
                    (float)(mc.mouseHandler.ypos() / mc.getWindow().getGuiScale()), 0, 1);
            new org.joml.Matrix4f(g.pose().last().pose()).invert().transform(point);
            frame(g).panel(x, y, w, h, style(radius, focused).withOpacity(Math.max(0, Math.min(1, opacity))), point.x, point.y);
            return true;
        } finally { drawing--; }
    }

    public static boolean texture(GuiGraphics g, ResourceLocation texture, int x1, int x2, int y1, int y2,
                                  float u0, float u1, float v0, float v1) {
        if (!active()) return false;
        int w = x2-x1, h = y2-y1;
        if (w <= 0 || h <= 0) return false;
        boolean progressBar=texture.getNamespace().equals("minecraft") && texture.getPath().equals("textures/gui/icons.png")
                && h==5 && w>=1 && (Math.abs(v0-64f/256f)<.0001f || Math.abs(v0-69f/256f)<.0001f
                || Math.abs(v0-84f/256f)<.0001f || Math.abs(v0-89f/256f)<.0001f);
        if (!progressBar && !skinTexture(texture.getNamespace(), texture.getPath(), v0)) return false;
        if (texture.getPath().equals("textures/gui/container/creative_inventory/tabs.png") && w == 26 && h == 32) {
            // Items are drawn separately by CreativeModeInventoryScreen. Replace the
            // whole tab backing so the inactive atlas's recessed dark face is not retained.
            boolean selected = Math.abs(v0-32f/256f)<.0001f || Math.abs(v0-96f/256f)<.0001f;
            return panel(g,x1,y1,w,h,7,selected,RenderSystem.getShaderColor()[3]);
        }
        if (texture.getPath().equals("textures/gui/container/creative_inventory/tabs.png")
                && w == 12 && h == 15 && v0 == 0 && u0 >= 232f/256f) {
            return scrollbar(g,x1+2,y1,8,h,false,u0 >= 244f/256f);
        }
        drawing++;
        try {
            // The hotbar atlas has deep black recesses; use one continuous liquid surface instead.
            if (texture.getPath().equals("textures/gui/widgets.png") && v0 < 46f/256f && h >= 20) {
                boolean selected = Math.abs(v0-22f/256f)<.0001f && w==24;
                frame(g).panel(x1,y1,w,h,style(selected?7:9,selected));
                if(w==182) for(int slot=1;slot<9;slot++)
                    g.fill(x1+1+slot*20,y1+5,x1+2+slot*20,y2-5,0x30ffffff);
                return true;
            }
            float radius = w >= 100 && h >= 80 ? 10 : Math.min(5, Math.min(w, h) * .18f);
            float opacity = RenderSystem.getShaderColor()[3];
            frame(g).texturedPanel(x1, y1, w, h, style(radius, false).withOpacity(opacity), texture, u0, v0, u1, v1);
            return true;
        } finally { drawing--; }
    }

    /** Slim rounded scroll furniture, without the atlas's opaque black track or bevel. */
    public static boolean scrollbar(GuiGraphics g,float x,float y,float w,float h,boolean track,boolean disabled) {
        if (!active() || w <= 0 || h <= 0) return false;
        drawing++;
        try {
            var material = new GlassStyle(Math.min(w,h)*.5f, .6f, .85f,
                    track ? .08f : .38f, .15f, track ? .22f : disabled ? .4f : .95f);
            frame(g).panel(x,y,w,h,material);
            return true;
        } finally { drawing--; }
    }

    /** Public for the atlas coverage verification task; no Minecraft state is read here. */
    public static boolean skinTexture(String namespace, String path, float v) {
        if (!namespace.equals("minecraft") || !path.startsWith("textures/gui/")) return false;
        // Keep world overlays and information-bearing icons exactly recognizable.
        if (path.startsWith("textures/gui/title/") || path.equals("textures/gui/options_background.png")
                || path.equals("textures/gui/icons.png") || path.equals("textures/gui/accessibility.png")
                || path.equals("textures/gui/stream_indicator.png")) return false;
        // The shared atlas includes hotbar pieces and language/accessibility button artwork.
        if (path.equals("textures/gui/widgets.png")) return true;
        return path.startsWith("textures/gui/container/") || path.startsWith("textures/gui/advancements/")
                || path.equals("textures/gui/recipe_book.png") || path.equals("textures/gui/recipe_button.png") || path.equals("textures/gui/book.png")
                || path.equals("textures/gui/demo_background.png") || path.equals("textures/gui/toasts.png")
                || path.equals("textures/gui/bars.png") || path.equals("textures/gui/spectator_widgets.png")
                || path.equals("textures/gui/social_interactions.png") || path.equals("textures/gui/report_button.png")
                || path.equals("textures/gui/tabs.png") || path.equals("textures/gui/slider.png")
                || path.equals("textures/gui/checkbox.png") || path.equals("textures/gui/statistics.png")
                || path.equals("textures/gui/server_selection.png") || path.equals("textures/gui/world_selection.png")
                || path.equals("textures/gui/resource_packs.png");
    }

    /** Replace neutral GUI backing rectangles (chat, scoreboard, player list, debug panels). */
    public static boolean backing(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        if (!active()) return false;
        int r=(color>>16)&255, green=(color>>8)&255, b=color&255, a=(color>>>24);
        int w=Math.abs(x2-x1), h=Math.abs(y2-y1);
        if (a < 12 || r > 96 || Math.abs(r-green)>8 || Math.abs(r-b)>8 || w < 12 || h < 8) return false;
        if (w >= g.guiWidth()-2 && h >= g.guiHeight()-2) return false;
        return panel(g, Math.min(x1,x2), Math.min(y1,y2), w, h, Math.min(5,h*.3f), false, Math.min(1, a/110f));
    }

    public static boolean background(GuiGraphics g, int width, int height, boolean forcePanorama) {
        if (!active()) return false;
        drawing++;
        try {
            Minecraft mc=Minecraft.getInstance();
            if (mc.level == null || forcePanorama) {
                if (panorama == null) panorama=new PanoramaRenderer(new CubeMap(new ResourceLocation("textures/gui/title/background/panorama")));
                g.flush();
                // CubeMap does not disable depth testing itself. Earlier GUI draws may
                // have written depth, which must never occlude this fresh backdrop.
                boolean depth = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
                RenderSystem.disableDepthTest();
                try { panorama.render(mc.getDeltaFrameTime(), 1); }
                finally { if (depth) RenderSystem.enableDepthTest(); }
            }
            // Neutral dimming: menu readability must not tint the panorama blue.
            g.fillGradient(0, 0, width, height, 0x451c1c1c, 0x752e2e2e);
            g.flush();
        } finally { drawing--; }
        resetPass();
        return true;
    }

    public static int readable(int color) {
        if (!active() || hudPass) return color;
        int rgb=color & 0xffffff;
        return rgb == 0x404040 || rgb == 0x3f3f3f || rgb == 0 ? (color & 0xff000000) | 0xeff5ff : color;
    }

    private static void resetPass() {
        if (frame != null) frame.close();
        frame=null;
    }

    @SubscribeEvent public static void tick(TickEvent.RenderTickEvent e) {
        if (e.phase == TickEvent.Phase.START) { resetPass(); hudPass=false; }
    }
    @SubscribeEvent public static void guiPre(RenderGuiEvent.Pre e) { resetPass(); hudPass=true; }
    @SubscribeEvent public static void guiPost(RenderGuiEvent.Post e) { resetPass(); hudPass=false; }
    @SubscribeEvent public static void screenPre(ScreenEvent.Render.Pre e) { resetPass(); hudPass=false; }
    @SubscribeEvent public static void screenPost(ScreenEvent.Render.Post e) { resetPass(); }
}
